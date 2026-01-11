package TS3Bot;

import TS3Bot.audio.MusicManager;
import TS3Bot.audio.TrackScheduler;
import TS3Bot.config.JsonHelper;
import TS3Bot.db.MusicDAO;
import TS3Bot.model.Track; // <--- IMPORTANTE: El nuevo modelo
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

    // Componentes de Lógica y Datos
    private final MusicManager musicManager;
    private final MusicDAO dao;

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

        // Inicializamos DB, DAO y Manager
        TS3Bot.db.DatabaseManager.init();
        this.dao = new MusicDAO();
        this.musicManager = new MusicManager();

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

        register("!vol", this::handleVolume, "!v");
        register("!shuffle", (args) -> { player.shuffle(); reply("[color=purple][b]🎲[/b][/color] ¡Cola mezclada!"); }, "!mix");

        // Playlists
        register("!pp", this::handlePlayPlaylist);
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
        String[] parts = raw.split("\\s+", 2);
        String label = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (label){
            case "!createp" -> handleCreatePlaylist(args, userUid);
            case "!addp" -> handleAddSongToPlaylist(args, userUid);
            default -> {
                Consumer<String> action = commandMap.get(label);
                if (action != null) action.accept(args);
            }
        }
    }

    // --- MANEJO DE MÚSICA Y PLAYLISTS ---



    /**
     * Maneja la reproduccion de una pista segun la consulta proporcionada.
     * El metodo resuelve la consulta para encontrar la pista correspondiente, la anade a la cola del reproductor de musica
     * y envia un mensaje al servidor con los detalles de la reproduccion o un mensaje de error en caso de fallo.
     *
     * @param query la consulta de busqueda o enlace directo utilizado para resolver y reproducir una pista.
     *              Si la consulta esta vacia, la ejecucion se termina.
     */
    private void handlePlay(String query) {
        if (query.isEmpty()) return;

        new Thread(() -> {
            try {
                reply("[color=orange][b]Buscando...[/b][/color]");
                Track track = musicManager.resolve(query);
                player.queue(track);

                reply("[color=purple][b]»[/b][/color] Reproduciendo: [b]" + track + "[/b]");

            } catch (Exception e) {
                e.printStackTrace();
                reply("[color=red][b]✘ Error:[/b][/color] " + e.getMessage());
            }
        }).start();
    }

    /**
     * Maneja la reproduccion de una lista de reproduccion dado su nombre. El metodo obtiene la lista de UUIDs de canciones
     * asociados a la lista de reproduccion, resuelve cada UUID en una pista, pone las pistas en cola para su reproduccion
     * y envia los mensajes correspondientes al servidor indicando el progreso y los resultados.
     *
     * @param playlistName el nombre de la lista de reproduccion a reproducir. Si el nombre esta vacio, el metodo termina la ejecucion.
     */
    private void handlePlayPlaylist(String playlistName) {
        if (playlistName.isEmpty()) { reply("[color=gray]Uso: !pp <nombre>[/color]"); return; }

        new Thread(() -> {
            List<String> uuids = dao.getUuidsDePlaylist(playlistName);
            if (uuids.isEmpty()) { reply("[color=red][b]![/b][/color] Playlist vacía o inexistente."); return; }

            reply("[color=blue][b]⌛[/b][/color] Se añadieron [b]" + uuids.size() + "[/b] canciones a la cola...");

            for (String uuid : uuids) {
                try {
                    // Resolvemos por UUID (esto verifica caché o descarga si falta)
                    Track track = musicManager.resolve(uuid);
                    player.queue(track);
                } catch (Exception ignored) {
                    System.err.println("Falló carga de track en playlist: " + uuid);
                }
            }
            reply("[color=green][b]✔[/b][/color] Playlist cargada.");
        }).start();
    }

    /**
     * Maneja la adicion de una cancion a una lista de reproduccion especificada por el usuario. El metodo analiza la entrada
     * para extraer el ID de la lista de reproduccion y el identificador o consulta de la cancion, resuelve los detalles de la cancion
     * usando la consulta proporcionada y anade la cancion resuelta a la lista de reproduccion especificada. Se envian mensajes de exito
     * o error al servidor al finalizar.
     *
     * @param args    Una cadena que contiene el ID de la lista de reproduccion y la consulta de la cancion separados por un espacio. El ID
     *                de la lista de reproduccion debe ser un entero y la consulta de la cancion debe identificar la cancion a anadir, ya sea
     *                mediante un enlace directo o una consulta de texto.
     * @param userUid El identificador unico del usuario que solicita la adicion, usado para seguimiento o permisos.
     */
    private void handleAddSongToPlaylist(String args, String userUid) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            reply("[color=gray]Uso: !addp <ID_Playlist> <Canción>[/color]");
            return;
        }

        new Thread(() -> {
            try {
                int pId = Integer.parseInt(parts[0]);
                // Resolvemos primero para asegurar que tenemos el UUID y Título correctos
                Track track = musicManager.resolve(parts[1]);

                // Usamos los getters del objeto Track
                dao.añadirCancionAPlaylist(pId, track.getUuid(), userUid);
                reply("[color=green][b]+[/b][/color] [i]" + track.getTitle() + "[/i] añadida a la playlist [b]#" + pId + "[/b]");
            } catch (Exception e) {
                reply("[color=red][b]✘ Error:[/b][/color] " + e.getMessage());
            }
        }).start();
    }

    /**
     * Maneja la creacion de una nueva lista de reproduccion con el nombre especificado y la asocia con el UID del usuario proporcionado.
     * Si el nombre de la lista de reproduccion esta vacio, se envia un mensaje informativo al usuario. Tras una creacion exitosa,
     * se envia un mensaje de confirmacion que incluye el ID de la lista de reproduccion. Si ocurre un error (por ejemplo, nombre duplicado
     * o problemas con la base de datos), se notifica al usuario con un mensaje de error.
     *
     * @param name    El nombre de la lista de reproduccion que se va a crear. No debe estar vacio.
     * @param userUid El identificador unico del usuario que esta creando la lista de reproduccion.
     */

    private void handleCreatePlaylist(String name, String userUid) {
        if (name.isEmpty()) { reply("[color=gray]Uso: !createp <nombre>[/color]"); return; }
        int id = dao.crearPlaylist(name, userUid);
        if (id != -1) {
            reply("[color=green][b]✔[/b][/color] Playlist [b]'" + name + "'[/b] creada. [i](ID: " + id + ")[/i]");
        } else {
            reply("[color=red][b]✘ Error:[/b][/color] Nombre duplicado o error DB.");
        }
    }

    /**
     * Maneja el listado de todas las listas de reproduccion disponibles. Este metodo obtiene todas las listas de reproduccion
     * de la base de datos a traves del DAO, las formatea y envia la lista resultante al servidor.
     *
     * Si no se encuentran listas de reproduccion, se envia un mensaje indicando la ausencia de listas de reproduccion y
     * proporcionando instrucciones para crear una nueva lista de reproduccion.
     *
     * El metodo utiliza mensajes con codigos de color para una mejor distincion visual en el chat del servidor.
     *
     * Nota: Este metodo depende de la capa DAO para obtener las listas de reproduccion y del metodo `reply` para enviar
     * mensajes al servidor.
     */
    private void handleListPlaylists() {
        List<String> playlists = dao.obtenerTodasLasPlaylists();
        if (playlists.isEmpty()) {
            reply("[color=gray]No hay playlists. Usa !createp <nombre>[/color]");
            return;
        }
        reply("[color=blue][b]======= PLAYLISTS =======[/b][/color]");
        for (String p : playlists) reply("[color=darkgreen]•[/color] " + p);
    }

    // --- SISTEMA ---

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
        reply("  [b]!createp[/b] [i]<nombre>[/i] | [b]!addp[/b] [i]<id> <canción>[/i]");
        reply("  [b]!pp[/b] [i]<nombre>[/i] | [b]!listp[/b]");
        reply("[color=royalblue][b]▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬[/b][/color]");
    }

    // --- CICLO DE VIDA Y UTILS ---

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