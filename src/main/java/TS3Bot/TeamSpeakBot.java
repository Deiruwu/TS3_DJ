package TS3Bot;

import TS3Bot.audio.MusicManager;
import TS3Bot.audio.TrackScheduler;
import TS3Bot.config.JsonHelper;
import TS3Bot.db.PlaylistDAO;
import TS3Bot.db.SongDAO;
import TS3Bot.db.StatsDAO;
import TS3Bot.model.Playlist;
import TS3Bot.model.PlaylistType;
import TS3Bot.model.QueuedTrack;
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

        setupTrackListener();
        registerCommands();
    }

    private void setupTrackListener() {
        player.setTrackStartListener((userUid, userName, trackUuid) -> {
            statsDao.registrarReproduccion(userUid, trackUuid);
            ensureUserPersonalPlaylist(userUid, userName, trackUuid);
        });
    }

    private void refreshPlaylists() {
        this.allPlaylists = playlistDao.getAllPlaylists();
    }

    private void registerCommands() {
        register("!skip", (args) -> {
            reply("[color=orange]Saltando...[/color]");
            player.next();
        }, "!next", "!s");

        register("!stop", (args) -> {
            reply("[color=red]Detenido.[/color]");
            player.shutdown();
        }, "!leave");

        register("!queue", (args) -> {
            String actual = player.getCurrentSongName();
            reply("[color=blue]Sonando:[/color] [i]" + actual + "[/i]");
            reply(player.getQueueDetails());
        }, "!q", "!cola");

        register("!vol", this::handleVolume, "!v", "!volumen");
        register("!shuffle", (args) -> {
            player.shuffle();
            reply("[color=purple]Cola mezclada.[/color]");
        }, "!mix", "!mezclar");

        register("!remove", this::handleRemove, "!rm", "!del");
        register("!listp", (args) -> handleListPlaylists(), "!playlists", "!lp");

        register("!help", (args) -> handleHelp(), "!h", "!ayuda", "!comandos");
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
            case "!p", "!play" -> handlePlay(args, userUid, userName, false);
            case "!pn", "!playnext" -> handlePlay(args, userUid, userName, true);
            case "!pp" -> handlePlayPlaylist(args, userUid, userName);
            case "!createp" -> handleCreatePlaylist(args, userUid);
            case "!addp" -> handleAddSongToPlaylist(args, userUid);
            default -> {
                Consumer<String> action = commandMap.get(label);
                if (action != null) action.accept(args);
            }
        }
    }

    private void handlePlay(String query, String userUid, String userName, boolean insertNext) {
        if (query.isEmpty()) return;

        new Thread(() -> {
            try {
                reply("[color=gray]Buscando...[/color]");
                Track track = musicManager.resolve(query);
                QueuedTrack queuedTrack = new QueuedTrack(track, userUid, userName);

                if (insertNext) {
                    player.queueNext(queuedTrack);
                    reply("[color=lime]Siguiente:[/color] [i]" + track + " -> " +track.getFormattedDuration() + "[/i]");
                } else {
                    player.queue(queuedTrack);
                    reply("[color=blue]Añadido:[/color] [i]" + track + " -> " +track.getFormattedDuration() + "[/i]");
                }

            } catch (Exception e) {
                e.printStackTrace();
                reply("[color=red]Error: " + e.getMessage() + "[/color]");
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

    private void handlePlayPlaylist(String input, String userUid, String userName) {
        if (input.isEmpty()) {
            reply("[color=gray]Uso: !pp <nombre|#>[/color]");
            return;
        }

        new Thread(() -> {
            Playlist playlist = resolvePlaylist(input);
            if (playlist == null) {
                reply("[color=red]Playlist no encontrada.[/color]");
                return;
            }

            List<String> uuids = playlistDao.getTrackUuidsFromPlaylist(playlist);
            if (uuids.isEmpty()) {
                reply("[color=red]Playlist vacía.[/color]");
                return;
            }

            reply("[color=blue]Cargando " + uuids.size() + " canciones...[/color]");

            for (String uuid : uuids) {
                try {
                    Track track = musicManager.resolve(uuid);
                    QueuedTrack queuedTrack = new QueuedTrack(track, userUid, userName);
                    player.queue(queuedTrack);
                } catch (Exception ignored) {
                    System.err.println("Falló carga de track: " + uuid);
                }
            }
            reply("[color=lime]Playlist cargada.[/color]");
        }).start();
    }

    private void handleAddSongToPlaylist(String args, String userUid) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            reply("[color=gray]Uso: !addp <nombre|#> <canción>[/color]");
            return;
        }

        new Thread(() -> {
            try {
                Playlist playlist = resolvePlaylist(parts[0]);
                if (playlist == null) {
                    reply("[color=red]Playlist no encontrada.[/color]");
                    return;
                }

                Track track = musicManager.resolve(parts[1]);
                playlistDao.addSongToPlaylist(playlist.getId(), track.getUuid());

                reply("[color=lime]Añadido a[/color] [b]" + playlist.getName() + "[/b]");
            } catch (Exception e) {
                reply("[color=red]Error: " + e.getMessage() + "[/color]");
            }
        }).start();
    }

    private void handleCreatePlaylist(String name, String userUid) {
        if (name.isEmpty()) {
            reply("[color=gray]Uso: !createp <nombre>[/color]");
            return;
        }

        int id = playlistDao.createPlaylist(name, userUid, PlaylistType.USER);
        if (id != -1) {
            refreshPlaylists();
            reply("[color=lime]Playlist creada:[/color] [b]" + name + "[/b]");
        } else {
            reply("[color=red]Error: nombre duplicado.[/color]");
        }
    }

    private void handleListPlaylists() {
        if (allPlaylists.isEmpty()) {
            reply("[color=gray]No hay playlists. Usa !createp[/color]");
            return;
        }

        reply(" ");
        reply("[color=blue][b]PLAYLISTS[/b][/color]");
        for (int i = 0; i < allPlaylists.size(); i++) {
            String name = allPlaylists.get(i).getName();
            reply("  [color=purple]#" + (i + 1) + "[/color] " + name);
        }
        reply(" ");
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

    private void handleRemove(String args) {
        try {
            int index = Integer.parseInt(args) - 1;
            if (player.removeFromQueue(index)) {
                reply("[color=orange]Canción #" + (index + 1) + " eliminada.[/color]");
            } else {
                reply("[color=red]Índice inválido.[/color]");
            }
        } catch (Exception e) {
            reply("[color=gray]Uso: !remove <#>[/color]");
        }
    }

    private void handleVolume(String args) {
        try {
            int vol = Integer.parseInt(args);
            player.setVolume(vol);
            saveVolumeConfig(vol);
            reply("[color=blue]Volumen:[/color] " + vol + "%");
        } catch (Exception e) {
            reply("[color=gray]Uso: !vol <0-100>[/color]");
        }
    }

    private void handleHelp() {
        reply(" ");
        reply("[color=blue][b]COMANDOS DEL BOT[/b][/color]");
        reply(" ");
        reply("[color=purple]Reproducción[/color]");
        reply("  !play <canción> -> Añadir a la cola");
        reply("  !pn <canción>   -> Poner como siguiente");
        reply("  !skip           -> Saltar canción");
        reply("  !stop           -> Detener todo");
        reply("  !queue          -> Ver cola actual");
        reply("  !remove <#>     -> Quitar de la cola");
        reply("  !vol <0-100>    -> Ajustar volumen");
        reply("  !shuffle        -> Mezclar cola");
        reply(" ");
        reply("[color=purple]Playlists[/color]");
        reply("  !createp <nombre>       -> Crear playlist");
        reply("  !addp <#|nombre> <song> -> Añadir canción");
        reply("  !removep <#> <song#>    -> Quitar canción");
        reply("  !pp <#|nombre>          -> Reproducir playlist");
        reply("  !listp                  -> Ver todas");
        reply(" ");
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
        System.out.println("Bot conectado.");
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