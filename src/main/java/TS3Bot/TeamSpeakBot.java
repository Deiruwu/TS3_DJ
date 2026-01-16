package TS3Bot;

import TS3Bot.audio.MusicManager;
import TS3Bot.audio.TrackScheduler;
import TS3Bot.audio.YouTubeHelper;
import TS3Bot.commands.*;
import TS3Bot.commands.maintenance.DeleteCommand;
import TS3Bot.commands.playback.*;
import TS3Bot.commands.playlist.*;
import TS3Bot.commands.playlist.sets.*;
import TS3Bot.commands.stats.*;
import TS3Bot.config.JsonHelper;
import TS3Bot.db.PlaylistDAO;
import TS3Bot.db.TrackDAO;
import TS3Bot.db.StatsDAO;
import TS3Bot.interfaces.Replyable;
import TS3Bot.model.*;
import com.github.manevolent.ts3j.event.TS3Listener;
import com.github.manevolent.ts3j.event.TextMessageEvent;
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

    private final JsonObject rootConfig;
    private final JsonObject defaultConfig;
    private final JsonObject serverConfig;
    private final File configFile;

    private final CommandRegistry commandRegistry;
    private boolean running = false;

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

        this.allPlaylists = playlistDao.getAllPlaylists();
        this.commandRegistry = new CommandRegistry();

        setupTrackListener();
        registerCommands();

        YouTubeHelper.setDownloadListener(track -> {
            replyAction("Track fuera de la base de datos. Descargando: " + track + "...");
        });
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

        // Playlist commands
        commandRegistry.register(new CreatePlaylistCommand(this));
        commandRegistry.register(new ListPlaylistsCommand(this));
        commandRegistry.register(new ShowPlaylistCommand(this));
        commandRegistry.register(new PlayPlaylistCommand(this));
        commandRegistry.register(new AddToPlaylistCommand(this));
        commandRegistry.register(new RemoveToPlaylistCommand(this));
        commandRegistry.register(new DislikeCommand(this));
        commandRegistry.register(new LikeCommand(this));

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
        if (!raw.startsWith("!")) return;

        String[] parts = raw.split("\\s+", 2);
        String label = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        CommandContext ctx = new CommandContext(
                args,
                e.getInvokerUniqueId(),
                e.getInvokerName(),
                raw
        );

        System.out.println(e.getInvokerName() + " executed: " + raw);
        commandRegistry.executeCommand(label, ctx);
    }

    // ========================================
    // TRACK LISTENER
    // ========================================

    private void setupTrackListener() {
        player.setTrackStartListener((userUid, userName, trackUuid) -> {
            Track trackActual = trackDao.getTrack(trackUuid);

            String baseNick = "[" + JsonHelper.getString(defaultConfig, serverConfig, "bot.nickname") + "] - " + trackActual.getTitle();
            String request = (userName == null) ? "" : " request by " + userName;
            String newNick = baseNick;
            if (baseNick.length() > 27){
                newNick = baseNick.substring(0, 27).concat("...");
            }

            try {
                client.setDescription(trackActual.toStringNotFormmat() + " [" + trackActual.getFormattedDuration() + "]" + request);
                client.setNickname(newNick);
            } catch (Exception e) {
                System.err.println("Error cambiando nick: " + e.getMessage());
            }

            reply("Reproduciendo: " + trackActual);

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

    public void refreshPlaylists() {
        this.allPlaylists = playlistDao.getAllPlaylists();
    }

    public void reply(String msg) {
        try {
            client.sendServerMessage(msg);
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
        // reemplaza "request by" en el username por "".
        if (userName.contains("request by")) {
            userName = userName.replace("request by", "");
        }
        userName = userName.trim();

        String playlistName = "Música de " + userName;

        Playlist userPlaylist = null;
        for (Playlist p : allPlaylists) {
            if (p.getName().equals(playlistName) && p.getOwnerUid().equals(userUid)) {
                userPlaylist = p;
                break;
            }
        }

        if (userPlaylist == null) {
            int newId = playlistDao.createPlaylist(playlistName, userUid, userName ,PlaylistType.FAVORITES);
            if (newId != -1) {
                refreshPlaylists();
                for (Playlist p : allPlaylists) {
                    if (p.getId() == newId) {
                        userPlaylist = p;
                        break;
                    }
                }
            }
        }

        if (userPlaylist != null) {
            playlistDao.addSongToPlaylist(userPlaylist.getId(), songUuid);
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
            this.running = true;
            runConsoleLoop();
        } catch (Exception e) {
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