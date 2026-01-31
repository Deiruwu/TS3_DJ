package TS3Bot;

import TS3Bot.audio.MetadataClient;
import TS3Bot.audio.MusicManager;
import TS3Bot.audio.TrackScheduler;
import TS3Bot.audio.download.listener.DownloadPipelineListener;
import TS3Bot.commands.*;
import TS3Bot.commands.playback.*;
import TS3Bot.commands.playlist.*;
import TS3Bot.commands.playlist.sets.*;
import TS3Bot.commands.stats.*;
import TS3Bot.config.JsonHelper;
import TS3Bot.db.PlaylistDAO;
import TS3Bot.db.TrackDAO;
import TS3Bot.db.StatsDAO;
import TS3Bot.interfaces.Replyable;
import TS3Bot.managers.ConfirmationManager;
import TS3Bot.model.*;
import TS3Bot.model.enums.PlaylistType;
import TS3Bot.services.DiscordService;
import TS3Bot.services.UserStateManager;
import com.github.manevolent.ts3j.event.*;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class TeamSpeakBot implements TS3Listener, Replyable {
    private final LocalTeamspeakClientSocket client;
    private final TrackScheduler player;

    private final MusicManager musicManager;
    private final TrackDAO trackDao;
    private final PlaylistDAO playlistDao;
    private final StatsDAO statsDao;

    private final UserStateManager userManager;
    private final DiscordService discordService;

    private final JsonObject rootConfig;
    private final JsonObject defaultConfig;
    private final JsonObject serverConfig;
    private final File configFile;

    private final CommandRegistry commandRegistry;
    private boolean running = false;
    private String botNickname;
    private ConfirmationManager confirmationManager;

    private final MetadataClient metadataClient;

    public List<Playlist> allPlaylists;

    public TeamSpeakBot(JsonObject rootConfig, File configFile) {
        this.rootConfig = rootConfig;
        this.configFile = configFile;
        this.defaultConfig = rootConfig.getAsJsonObject("default");
        JsonArray servers = rootConfig.getAsJsonArray("servers");
        this.serverConfig = (servers != null && servers.size() > 0) ? servers.get(0).getAsJsonObject() : new JsonObject();

        this.client = new LocalTeamspeakClientSocket();
        this.client.addListener(this);
        this.player = new TrackScheduler();

        TS3Bot.db.DatabaseManager.init();
        this.trackDao = new TrackDAO();
        this.playlistDao = new PlaylistDAO();
        this.statsDao = new StatsDAO();
        this.musicManager = new MusicManager();

        this.userManager = new UserStateManager();
        this.discordService = DiscordService.getInstance();

        this.allPlaylists = playlistDao.getAllPlaylists();
        this.commandRegistry = new CommandRegistry();

        this.confirmationManager = new ConfirmationManager();

        this.metadataClient = new MetadataClient();

        setupBotConfiguration();
        setupDownloadListener();
        setupTrackListener();
        registerCommands();

    }

    private void setupBotConfiguration() {
        this.botNickname = "TS3Bot";
        if (serverConfig.has("bot") && serverConfig.getAsJsonObject("bot").has("nickname")) {
            this.botNickname = serverConfig.getAsJsonObject("bot").get("nickname").getAsString();
        } else if (defaultConfig.has("bot") && defaultConfig.getAsJsonObject("bot").has("nickname")) {
            this.botNickname = defaultConfig.getAsJsonObject("bot").get("nickname").getAsString();
        }

        String webhookUrl = null;
        if (serverConfig.has("discord_webhook")) {
            webhookUrl = serverConfig.get("discord_webhook").getAsString();
        }

        if (webhookUrl != null) {
            discordService.setConfig(webhookUrl, this.botNickname);
            System.out.println("Discord Service: ON (User: " + this.botNickname + ")");
        }
    }

    @Override
    public TeamSpeakBot getBot() {
        return this;
    }

    // ========================================
    // COMMAND REGISTRATION
    // ========================================

    private void registerCommands() {
        // Playback commands
        commandRegistry.register(new PlayCommand(this));
        commandRegistry.register(new PlayNextCommand(this));
        commandRegistry.register(new SkipCommand(this));
        commandRegistry.register(new SkipToCommand(this));
        commandRegistry.register(new QueueCommand(this));
        commandRegistry.register(new RemoveCommand(this));
        commandRegistry.register(new ShuffleCommand(this));
        commandRegistry.register(new VolumeCommand(this));
        commandRegistry.register(new ClearCommand(this));
        commandRegistry.register(new DeleteCommand(this));
        commandRegistry.register(new CancelCommand(this));
        commandRegistry.register(new StopCommand(this));

        // Playlist commands
        commandRegistry.register(new CreatePlaylistCommand(this));
        commandRegistry.register(new ListPlaylistsCommand(this));
        commandRegistry.register(new ShowPlaylistCommand(this));
        commandRegistry.register(new PlayPlaylistCommand(this));
        commandRegistry.register(new AddToPlaylistCommand(this));
        commandRegistry.register(new RemoveToPlaylistCommand(this));
        commandRegistry.register(new DislikeCommand(this));
        commandRegistry.register(new LikeCommand(this));
        commandRegistry.register(new RenamePlaylistCommand(this));
        commandRegistry.register(new DeletePlaylistCommand(this));
        commandRegistry.register(new InviteCollaboratorCommand(this));

        // Set operations
        commandRegistry.register(new IntersectCommand(this));
        commandRegistry.register(new UnionCommand(this));
        commandRegistry.register(new SymdiffCommand(this));

        // Stats commands
        commandRegistry.register(new TopCommand(this));
        commandRegistry.register(new LeastCommand(this));
        commandRegistry.register(new TopGlobalCommand(this));
        commandRegistry.register(new LeastGlobalCommand(this));

        commandRegistry.register(new HelpCommand(this));

        System.out.println("Registered " + commandRegistry.getCommandCount() + " commands");
    }

    // ========================================
    // MESSAGE HANDLING
    // ========================================

    @Override
    public void onTextMessage(TextMessageEvent e) {
        if (e.getInvokerId() == client.getClientId()) return;

        String raw = e.getMessage().trim().replaceAll("\\[/?(?i)URL\\]", "");

        // Manejar confirmaciones pendientes
        if (confirmationManager.hasPending(e.getInvokerUniqueId())) {
            confirmationManager.handleResponse(e.getInvokerUniqueId(), raw);
            return;
        }

        if (!raw.startsWith("!")) return;

        String[] parts = raw.split("\\s+", 2);
        String label = parts[0].toLowerCase();
        String argsRaw = parts.length > 1 ? parts[1] : "";

        // Parsear flags
        Map<String, String> flags = new HashMap<>();
        StringBuilder cleanArgs = new StringBuilder();

        if (!argsRaw.isEmpty()) {
            String[] argParts = argsRaw.split("\\s+");
            for (String part : argParts) {
                if (part.startsWith("--")) {
                    String flag = part.substring(2).toLowerCase();
                    flags.put(flag, "true");
                } else {
                    if (cleanArgs.length() > 0) cleanArgs.append(" ");
                    cleanArgs.append(part);
                }
            }
        }

        CommandContext ctx = new CommandContext(
                cleanArgs.toString(),
                e.getInvokerUniqueId(),
                e.getInvokerId(),
                e.getInvokerName(),
                raw,
                flags
        );

        System.out.println(e.getInvokerName() + " executed: " + raw);
        commandRegistry.executeCommand(label, ctx);
    }
    @Override
    public void onClientJoin(ClientJoinEvent e) {
        if (e.getClientType() != 0) return;

        if (e.getClientId() == client.getClientId()) return;

        System.out.println("Nuevo usuario: " + e.getClientNickname() + ". Grupos del servidor: " + e.getClientServerGroups() + ". id: " + e.getClientId());

        userManager.addUser(e);
        discordService.onUserJoin(e.getClientId(), e.getClientNickname());
    }

    @Override
    public void onClientLeave(ClientLeaveEvent e) {

        System.out.println("El usuario " + e.getInvokerName() + " ha abandonado el servidor.");

        userManager.removeUser(e);
        discordService.onUserLeave(e.getClientId());
    }

    @Override
    public void onClientChanged(ClientUpdatedEvent e) {
        if (userManager.updateUser(e)) {
            String myUid = client.getIdentity().getUid().toBase64();
            discordService.fetchInitialList(this.client, myUid);
        }
    }

    // ========================================
    // TRACK LISTENER
    // ========================================


    private void setupDownloadListener() {
        metadataClient.setDownloadListener(
                new DownloadPipelineListener(this)
        );
    }

    private void setupTrackListener() {
        player.setTrackStartListener((userUid, userName, trackUuid) -> {
            QueuedTrack queuedTrack = player.getCurrentTrack();
            Track trackActual = queuedTrack.getTrack();

           String albumUrl = trackActual.getThumbnail();

           discordService.setAvatarUrl(albumUrl);


            String baseNick = "[" + JsonHelper.getString(defaultConfig, serverConfig, "bot.nickname") + "] - " + trackActual.getTitle();
            String newNick = baseNick;
            if (baseNick.length() > 27){
                newNick = baseNick.substring(0, 27).concat("...");
            }

            boolean isUserRequest = (userName != null && userUid != null);
            QueuedTrack tempQueuedTrack = new QueuedTrack(trackActual, userUid, userName, isUserRequest);
            String requestInfo = tempQueuedTrack.getRequestInfo();

            try {
                client.setDescription(trackActual.toStringNotFormmat() + " [" + trackActual.getFormattedDuration() + "]" + requestInfo);
                client.setNickname(newNick);
            } catch (Exception e) {
                System.err.println("Error cambiando nick: " + e.getMessage());
            }

            replyPlayingListener(queuedTrack.toString());

            if (userUid == null) return;
            if (trackUuid == null) return;

            statsDao.registrarReproduccion(userUid, trackUuid);
            ensureUserPersonalPlaylist(userUid, userName, trackUuid);
        });
    }

    // ========================================
    // PUBLIC METHODS FOR COMMANDS
    // ========================================

    public TrackScheduler getPlayer() {
        return player;
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public PlaylistDAO getPlaylistManager() {
        return playlistDao;
    }

    public TrackDAO getTrackDao() {
        return trackDao;
    }

    public StatsDAO getStatsDao() {
        return statsDao;
    }

    public List<Playlist> getAllPlaylists() {
        return allPlaylists;
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public boolean userBelongsToGroup(int userId, int groupId) {
        return userManager.userBelongsToGroup(userId, groupId);
    }

    public ConnectedUser getUserByName(String name) {
        return userManager.getUserByName(name);
    }

    public ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }

    public int getClientIdByUid(String uid) {
        return userManager.getClientIdByUid(uid);
    }

    public void refreshPlaylists() {
        this.allPlaylists = playlistDao.getAllPlaylists();
    }

    public void reply(String msg) {
        try {
            client.sendServerMessage(msg);
        } catch (Exception ignored) {
        }
    }

    public void replyPoke(int uidUser,String msg){
        try {
            client.clientPoke(uidUser, msg);
        } catch (Exception ignored) {
        }
    }

    public void saveVolumeConfig(int vol) {
        try {
            if (!defaultConfig.has("audio")) defaultConfig.add("audio", new JsonObject());
            defaultConfig.getAsJsonObject("audio").addProperty("volume", vol);
            try (Writer writer = new FileWriter(configFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(rootConfig, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========================================
    // INTERNAL HELPER
    // ========================================

    private void ensureUserPersonalPlaylist(String userUid, String userName, String songUuid) {
        Playlist userPlaylist = playlistDao.getFavoritesPlaylist(userUid);

        if (userPlaylist == null) {
            String playlistName = "Música de " + userName;
            int newId = playlistDao.createPlaylist(playlistName, userUid, PlaylistType.FAVORITES);

            if (newId != -1) {
                refreshPlaylists();
                userPlaylist = playlistDao.getPlaylist(newId);
            }
        }

        if (userPlaylist != null) {
            playlistDao.addTrackToPlaylist(userPlaylist.getId(), songUuid, userUid);
        }
    }

    // ========================================
    // BOT LIFECYCLE
    // ========================================

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        try {
            setupIdentity();
            setupBotDetails();
            int vol = 50;
            if (defaultConfig.has("audio")) vol = defaultConfig.getAsJsonObject("audio").get("volume").getAsInt();
            player.setVolume(vol);

            connect();

            userManager.setBotClientId(client.getClientId());

            String myUid = client.getIdentity().getUid().toBase64();

            discordService.fetchInitialList(this.client, myUid);

            this.running = true;
            runConsoleLoop();
        } catch (Exception e) {
            System.err.println("CRASH AL INICIAR:");
            e.printStackTrace();

            stop();
        }
    }

    private void setupIdentity() throws Exception {
        String path = JsonHelper.getString(defaultConfig, serverConfig, "bot.identity.file");
        File file = new File(path);
        LocalIdentity id = file.exists() ? LocalIdentity.read(file) : LocalIdentity.generateNew(10);
        if (!file.exists()) id.save(file);
        client.setIdentity(id);
    }

    private void setupBotDetails() {
        client.setNickname(JsonHelper.getString(defaultConfig, serverConfig, "bot.nickname"));
    }

    private void connect() throws IOException, TimeoutException, InterruptedException {
        String address = JsonHelper.getString(defaultConfig, serverConfig, "address");
        long timeout = JsonHelper.getInt(defaultConfig, serverConfig, "timeout");
        System.out.println("Conectando...");
        client.connect(address, timeout);
        client.waitForState(ClientConnectionState.CONNECTED, timeout);

        client.setMicrophone(this.player);
        System.out.println("Bot conectado.");
    }

    private void runConsoleLoop() {
        Scanner sc = new Scanner(System.in);
        while (running && sc.hasNextLine()) {
            String in = sc.nextLine();
            if (in.equalsIgnoreCase("/exit")) {
                stop();
                break;
            }
            reply(in);
        }
    }

    private void cleanup() {
        this.running = false;
        if (player != null) player.shutdown();
        if (client != null) {
            client.setMicrophone(null);
            try {
                client.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    public void stop() {
        cleanup();
        System.exit(0);
    }
}