package TS3Bot;

import TS3Bot.audio.TrackScheduler;
import TS3Bot.audio.YouTubeHelper;
import TS3Bot.config.JsonHelper;
import TS3Bot.db.MusicDAO;
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
    private final MusicDAO dao = new MusicDAO();

    private final JsonObject rootConfig;
    private final JsonObject defaultConfig;
    private final JsonObject serverConfig;
    private final File configFile;

    private final Map<String, Consumer<String>> commandMap = new HashMap<>();
    private boolean running = false;

    public TeamSpeakBot(JsonObject rootConfig, File configFile) {
        this.rootConfig = rootConfig;
        this.configFile = configFile;
        this.defaultConfig = rootConfig.getAsJsonObject("default");
        JsonArray servers = rootConfig.getAsJsonArray("servers");
        this.serverConfig = (servers != null && servers.size() > 0) ? servers.get(0).getAsJsonObject() : new JsonObject();

        this.client = new LocalTeamspeakClientSocket();
        this.client.addListener(this);
        this.player = new TrackScheduler();

        registerCommands();
    }

    private void registerCommands() {
        register("!p", this::handlePlay, "!play");
        register("!skip", (args) -> { reply("[color=orange][b]»[/b][/color] Saltando canción..."); player.next(); }, "!next", "!s");
        register("!stop", (args) -> { reply("[color=red][b]■[/b][/color] Música detenida."); player.shutdown(); }, "!leave");
        register("!queue", (args) -> {
            String actual = player.getCurrentSongName();
            reply("[color=blue][b]♪ Sonando ahora:[/b][/color] [i]" + actual + "[/i]");
            reply(player.getQueueDetails());
        }, "!q", "!list");
        register("!pp", this::handlePlayPlaylist);
        register("!vol", this::handleVolume, "!v");
        register("!listp", (args) -> handleListPlaylists(), "!playlists");
        register("!shuffle", (args) -> { player.shuffle(); reply("[color=purple][b]🎲[/b][/color] ¡Cola mezclada aleatoriamente!"); }, "!mix");
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
        if (!raw.startsWith("!")) return;

        String userUid = e.getInvokerUniqueId();
        String[] parts = raw.split("\\s+", 2);
        String label = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        if (label.equals("!createp")) {
            handleCreatePlaylist(args, userUid);
        } else if (label.equals("!addp")) {
            handleAddSongToPlaylist(args, userUid);
        } else {
            Consumer<String> action = commandMap.get(label);
            if (action != null) action.accept(args);
        }
    }

    // --- MANEJO DE PLAYLISTS ---

    private void handleCreatePlaylist(String name, String userUid) {
        if (name.isEmpty()) { reply("[color=gray]Uso: !createp <nombre>[/color]"); return; }
        int id = dao.crearPlaylist(name, userUid);
        if (id != -1) {
            reply("[color=green][b]✔[/b][/color] Playlist [b]'" + name + "'[/b] creada con éxito. [i](ID: " + id + ")[/i]");
        } else {
            reply("[color=red][b]✘ Error:[/b][/color] El nombre ya existe o hay un problema con la DB.");
        }
    }

    private void handleAddSongToPlaylist(String args, String userUid) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            reply("[color=gray]Uso: !addp <ID_Playlist> <Canción>[/color]");
            return;
        }

        new Thread(() -> {
            try {
                int pId = Integer.parseInt(parts[0]);
                YouTubeHelper.MusicEntry entry = YouTubeHelper.processRequest(parts[1], true);

                dao.añadirCancionAPlaylist(pId, entry.uuid, userUid);
                reply("[color=green][b]+[/b][/color] [i]" + entry.titulo + "[/i] añadida a la playlist [b]#" + pId + "[/b]");
            } catch (Exception e) {
                reply("[color=red][b]✘ Error:[/b][/color] " + e.getMessage());
            }
        }).start();
    }

    private void handlePlayPlaylist(String playlistName) {
        if (playlistName.isEmpty()) { reply("[color=gray]Uso: !pp <nombre>[/color]"); return; }
        new Thread(() -> {
            List<String> uuids = dao.getUuidsDePlaylist(playlistName);
            if (uuids.isEmpty()) { reply("[color=red][b]![/b][/color] Playlist vacía o inexistente."); return; }

            reply("[color=blue][b]⌛[/b][/color] Cargando [b]" + uuids.size() + "[/b] canciones de [i]" + playlistName + "[/i]...");
            for (String uuid : uuids) {
                try {
                    YouTubeHelper.MusicEntry entry = YouTubeHelper.processRequest(uuid, true);
                    player.queue(new File(entry.ruta), entry.titulo);
                } catch (Exception ignored) {}
            }
            reply("[color=green][b]✔[/b][/color] Playlist [b]" + playlistName + "[/b] cargada en la cola.");
        }).start();
    }

    private void handleListPlaylists() {
        List<String> playlists = dao.obtenerTodasLasPlaylists();

        if (playlists.isEmpty()) {
            reply("[color=gray]No hay playlists creadas. Usa !createp <nombre>[/color]");
            return;
        }

        reply("[color=blue][b]======= LISTADO DE PLAYLISTS =======[/b][/color]");
        for (String p : playlists) {
            reply("[color=darkgreen]•[/color] " + p);
        }
        reply("[color=gray][i]Usa !pp <nombre> para reproducir una.[/i][/color]");
    }

    // --- REPRODUCCIÓN Y VOLUMEN ---

    private void handlePlay(String query) {
        if (query.isEmpty()) return;

        new Thread(() -> {
            try {
                YouTubeHelper.MusicEntry entry = YouTubeHelper.processRequest(query, true);
                player.queue(new File(entry.ruta), entry.titulo);

                if (!entry.uuid.equals("Caché")) {
                    reply("[color=green][b]»[/b][/color] Añadido a la cola: [b]" + entry.titulo + "[/b]");
                }
            } catch (Exception e) {
                reply("[color=red][b]✘ Error procesando:[/b][/color] " + query);
            }
        }).start();
    }

    private void handleVolume(String args) {
        try {
            int vol = Integer.parseInt(args);
            player.setVolume(vol);
            saveVolumeConfig(vol);
            reply("[color=blue][b] Volumen:[/b][/color] [b]" + vol + "%[/b]");
        } catch (Exception e) { reply("[color=gray]Uso: !vol 0-100[/color]"); }
    }

    private void handleHelp() {
        reply("[color=royalblue][b]▬▬▬▬▬▬▬▬▬▬▬▬▬▬ MUSIC BOT HELP ▬▬▬▬▬▬▬▬▬▬▬▬▬▬[/b][/color]");

        reply("[color=darkcyan][b]▶ REPRODUCCIÓN[/b][/color]");
        reply("  [b]!p[/b] [i]<nombre/link>[/i] [color=gray]- Reproduce o añade a la cola.[/color]");
        reply("  [b]!skip[/b] [color=gray]- Salta a la siguiente pista.[/color]");
        reply("  [b]!stop[/b] [color=gray]- Detiene la música y limpia la cola.[/color]");
        reply("  [b]!vol[/b] [i]<0-100>[/i] [color=gray]- Ajusta el volumen maestro.[/color]");

        reply(" "); // Espaciador

        reply("[color=darkcyan][b]COLA Y LISTAS[/b][/color]");
        reply("  [b]!queue[/b] [color=gray]- Muestra qué suena y qué viene después.[/color]");
        reply("  [b]!shuffle[/b] [color=gray]- Mezcla las canciones en espera.[/color]");
        reply("  [b]!listp[/b] [color=gray]- Lista todas las playlists del servidor.[/color]");

        reply(" "); // Espaciador

        reply("[color=darkcyan][b]GESTIÓN DE PLAYLISTS[/b][/color]");
        reply("  [b]!createp[/b] [i]<nombre>[/i] [color=gray]- Crea una nueva lista vacía.[/color]");
        reply("  [b]!addp[/b] [i]<id> <canción>[/i] [color=gray]- Guarda una canción en una lista.[/color]");
        reply("  [b]!pp[/b] [i]<nombre>[/i] [color=gray]- Carga una playlist completa a la cola.[/color]");

        reply(" "); // Espaciador

        reply("[color=orange][i]Tip: Si usas links directos de YouTube, el bot responde más rápido.[/i][/color]");
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

    // --- CICLO DE VIDA ---

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        try {
            setupIdentity();
            setupBotDetails();
            setupAudioPreferences();
            connect();
            this.running = true;
            runConsoleLoop();
        } catch (Exception e) { stop(); }
    }

    private void setupAudioPreferences() {
        int vol = 50;
        try {
            if (defaultConfig.has("audio")) vol = defaultConfig.getAsJsonObject("audio").get("volume").getAsInt();
        } catch (Exception ignored) {}
        player.setVolume(vol);
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