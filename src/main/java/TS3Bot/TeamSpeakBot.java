package TS3Bot;

import TS3Bot.audio.MusicManager;
import TS3Bot.audio.TrackScheduler;
import TS3Bot.config.JsonHelper;
import TS3Bot.db.PlaylistDAO;
import TS3Bot.db.SongDAO;
import TS3Bot.db.StatsDAO;
import TS3Bot.model.Playlist;
import TS3Bot.model.PlaylistType;
import TS3Bot.model.Track;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class TeamSpeakBot implements TS3Listener {
    private final LocalTeamspeakClientSocket client;
    private final TrackScheduler player;

    private final MusicManager musicManager;
    private final SongDAO songDao;
    private final PlaylistDAO playlistDao;
    private final StatsDAO statsDao;

    private final JsonObject rootConfig;
    private final JsonObject defaultConfig;
    private final JsonObject serverConfig;
    private final File configFile;

    private final Map<String, Consumer<String>> commandMap = new HashMap<>();
    private boolean running = false;

    private List<Playlist> allPlaylists;

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
        this.songDao = new SongDAO();
        this.playlistDao = new PlaylistDAO();
        this.statsDao = new StatsDAO();
        this.musicManager = new MusicManager();

        this.allPlaylists = playlistDao.getAllPlaylists();

        registerCommands();
    }

    private void refreshPlaylists() {
        this.allPlaylists = playlistDao.getAllPlaylists();
    }

    private void registerCommands() {
        register("!skip", (args) -> { reply("[color=orange][b]»[/b][/color] Saltando canción..."); player.next(); }, "!next", "!s");
        register("!stop", (args) -> { reply("[color=red][b]■[/b][/color] Música detenida."); player.shutdown(); }, "!leave");
        register("!queue", (args) -> {
            String actual = player.getCurrentSongName();
            reply("[color=blue][b]♪ Sonando ahora:[/b][/color] [i]" + actual + "[/i]");
            reply(player.getQueueDetails());
        }, "!q", "!list");

        register("!vol", this::handleVolume, "!v");
        register("!shuffle", (args) -> { player.shuffle(); reply("[color=purple][b]🎲[/b][/color] ¡Cola mezclada!"); }, "!mix");

        register("!listp", (args) -> handleListPlaylists(), "!playlists");

        register("!help", (args) -> handleHelp(), "!h", "!ayuda");
    }

    private void register(String command, Consumer<String> action, String... aliases) {
        commandMap.put(command, action);
        for (String alias : aliases) commandMap.put(alias, action);
    }

    @Override
    public void onTextMessage(TextMessageEvent e) {
        if (e.getInvokerId() == client.getClientId()) return;

        String raw = e.getMessage().trim();

        raw = raw.replaceAll("\\[/?(?i)URL\\]", "");

        if (!raw.startsWith("!")) return;

        String userUid = e.getInvokerUniqueId();
        String userName = e.getInvokerName();
        String[] parts = raw.split("\\s+", 2);
        String label = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (label){
            case "!p", "!play" -> handlePlay(args, userUid, userName);
            case "!pp" -> handlePlayPlaylist(args);
            case "!createp" -> handleCreatePlaylist(args, userUid);
            case "!addp" -> handleAddSongToPlaylist(args, userUid);
            default -> {
                Consumer<String> action = commandMap.get(label);
                if (action != null) action.accept(args);
            }
        }
    }

    private void handlePlay(String query, String userUid, String userName) {
        if (query.isEmpty()) return;

        new Thread(() -> {
            try {
                reply("[color=orange][b]Buscando...[/b][/color]");
                Track track = musicManager.resolve(query);
                player.queue(track);

                statsDao.registrarReproduccion(userUid, track.getUuid());
                ensureUserPersonalPlaylist(userUid, userName, track.getUuid());

                reply("[color=purple][b]»[/b][/color] Reproduciendo: [b]" + track + "[/b]");

            } catch (Exception e) {
                e.printStackTrace();
                reply("[color=red][b]✘ Error:[/b][/color] " + e.getMessage());
            }
        }).start();
    }

    private void ensureUserPersonalPlaylist(String userUid, String userName, String songUuid) {
        String playlistName = "Música de " + userName;

        Playlist userPlaylist = null;
        for (Playlist p : allPlaylists) {
            if (p.getName().equals(playlistName) && p.getOwnerUid().equals(userUid)) {
                userPlaylist = p;
                break;
            }
        }

        if (userPlaylist == null) {
            int newId = playlistDao.createPlaylist(playlistName, userUid, PlaylistType.FAVORITES);
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

    private void handlePlayPlaylist(String input) {
        if (input.isEmpty()) { reply("[color=gray]Uso: !pp <nombre o número>[/color]"); return; }

        new Thread(() -> {
            Playlist playlist = resolvePlaylist(input);
            if (playlist == null) {
                reply("[color=red][b]![/b][/color] Playlist no encontrada.");
                return;
            }

            List<String> uuids = playlistDao.getTrackUuidsFromPlaylist(playlist);
            if (uuids.isEmpty()) { reply("[color=red][b]![/b][/color] Playlist vacía o inexistente."); return; }

            reply("[color=blue][b]⌛[/b][/color] Se añadieron [b]" + uuids.size() + "[/b] canciones a la cola...");

            for (String uuid : uuids) {
                try {
                    Track track = musicManager.resolve(uuid);
                    player.queue(track);
                } catch (Exception ignored) {
                    System.err.println("Falló carga de track en playlist: " + uuid);
                }
            }
            reply("[color=green][b]✓[/b][/color] Playlist cargada.");
        }).start();
    }

    private void handleAddSongToPlaylist(String args, String userUid) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            reply("[color=gray]Uso: !addp <nombre o número> <Canción>[/color]");
            return;
        }

        new Thread(() -> {
            try {
                Playlist playlist = resolvePlaylist(parts[0]);
                if (playlist == null) {
                    reply("[color=red][b]![/b][/color] Playlist no encontrada.");
                    return;
                }

                Track track = musicManager.resolve(parts[1]);

                playlistDao.addSongToPlaylist(playlist.getId(), track.getUuid());
                reply("[color=green][b]+[/b][/color] [i]" + track.getTitle() + "[/i] añadida a la playlist [b]" + playlist.getName() + "[/b]");
            } catch (Exception e) {
                reply("[color=red][b]✘ Error:[/b][/color] " + e.getMessage());
            }
        }).start();
    }

    private void handleCreatePlaylist(String name, String userUid) {
        if (name.isEmpty()) { reply("[color=gray]Uso: !createp <nombre>[/color]"); return; }
        int id = playlistDao.createPlaylist(name, userUid, PlaylistType.USER);
        if (id != -1) {
            refreshPlaylists();
            reply("[color=green][b]✓[/b][/color] Playlist [b]'" + name + "'[/b] creada. [i](ID: " + id + ")[/i]");
        } else {
            reply("[color=red][b]✘ Error:[/b][/color] Nombre duplicado o error DB.");
        }
    }

    private void handleListPlaylists() {
        if (allPlaylists.isEmpty()) {
            reply("[color=gray]No hay playlists. Usa !createp <nombre>[/color]");
            return;
        }
        reply("[color=blue][b]======= PLAYLISTS =======[/b][/color]");
        for (int i = 0; i < allPlaylists.size(); i++) {
            reply("[color=darkgreen]\t•[/color] " + allPlaylists.get(i).getName() + " [i](#" + (i + 1) + ")[/i]");
        }
    }

    private Playlist resolvePlaylist(String input) {
        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < allPlaylists.size()) {
                return allPlaylists.get(index);
            }
        } catch (NumberFormatException e) {
            for (Playlist p : allPlaylists) {
                if (p.getName().equalsIgnoreCase(input)) {
                    return p;
                }
            }
        }
        return null;
    }

    private void handleVolume(String args) {
        try {
            int vol = Integer.parseInt(args);
            player.setVolume(vol);
            saveVolumeConfig(vol);
            reply("[color=blue][b]🔊 Volumen:[/b][/color] [b]" + vol + "%[/b]");
        } catch (Exception e) { reply("[color=gray]Uso: !vol 0-100[/color]"); }
    }

    private void handleHelp() {
        reply("[color=royalblue][b]▬▬▬▬▬▬▬▬▬▬▬▬▬▬ MUSIC BOT HELP ▬▬▬▬▬▬▬▬▬▬▬▬▬▬[/b][/color]");
        reply("[color=darkcyan][b] REPRODUCCIÓN[/b][/color]");
        reply("  [b]!p[/b] [i]<nombre/link>[/i] [color=gray]- Reproduce/Añade.[/color]");
        reply("  [b]!skip[/b] [color=gray]- Siguiente pista.[/color]");
        reply("  [b]!stop[/b] [color=gray]- Detener todo.[/color]");
        reply("  [b]!vol[/b] [i]<0-100>[/i] [color=gray]- Volumen.[/color]");
        reply(" ");
        reply("[color=darkcyan][b] PLAYLISTS[/b][/color]");
        reply("  [b]!createp[/b] [i]<nombre>[/i] | [b]!addp[/b] [i]<nombre/#> <canción>[/i]");
        reply("  [b]!pp[/b] [i]<nombre/#>[/i] | [b]!listp[/b]");
        reply("[color=royalblue][b]▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬[/b][/color]");
    }

    private void saveVolumeConfig(int vol) {
        try {
            if (!defaultConfig.has("audio")) defaultConfig.add("audio", new JsonObject());
            defaultConfig.getAsJsonObject("audio").addProperty("volume", vol);
            try (Writer writer = new FileWriter(configFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(rootConfig, writer);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

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
        } catch (Exception e) { stop(); }
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
        System.out.println(">>> ¡LISTO!");
    }

    private void reply(String msg) {
        try { client.sendServerMessage(msg); } catch (Exception ignored) {}
    }

    private void runConsoleLoop() {
        Scanner sc = new Scanner(System.in);
        while (running && sc.hasNextLine()) {
            String in = sc.nextLine();
            if (in.equalsIgnoreCase("/exit")) { stop(); break; }
            reply(in);
        }
    }

    private void cleanup() {
        this.running = false;
        if (player != null) player.shutdown();
        if (client != null) {
            client.setMicrophone(null);
            try { client.disconnect(); } catch (Exception ignored) {}
        }
    }

    public void stop() { cleanup(); System.exit(0); }
}