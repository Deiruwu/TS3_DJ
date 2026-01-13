package TS3Bot;

import TS3Bot.audio.MusicManager;
import TS3Bot.audio.TrackScheduler;
import TS3Bot.audio.YouTubeHelper;
import TS3Bot.config.JsonHelper;
import TS3Bot.db.PlaylistDAO;
import TS3Bot.db.TrackDAO;
import TS3Bot.db.StatsDAO;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TeamSpeakBot implements TS3Listener {
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
        this.trackDao = new TrackDAO();
        this.playlistDao = new PlaylistDAO();
        this.statsDao = new StatsDAO();
        this.musicManager = new MusicManager();

        this.allPlaylists = playlistDao.getAllPlaylists();

        setupTrackListener();
        registerCommands();

        YouTubeHelper.setDownloadListener(track -> {
            reply("[color=orange][b]Track fuera de la base de datos. Descargando...[/b][/color]");
        });
    }

    private void setupTrackListener() {
        player.setTrackStartListener((userUid, userName, trackUuid) -> {
            Track trackActual = trackDao.getTrack(trackUuid);

            String baseNick = "[" + JsonHelper.getString(defaultConfig, serverConfig, "bot.nickname") + "] - " + trackActual.getTitle();
            String request = (userName == null) ? "" :" request by " + userName;
            try {
                client.setDescription(trackActual + " [" + trackActual.getFormattedDuration() + "]" + request);
                client.setNickname(baseNick);
            } catch (Exception e) {
                System.err.println("Error cambiando nick: " + e.getMessage());
            }

            if (userUid == null) return;
            if (trackUuid == null) return;

            statsDao.registrarReproduccion(userUid, trackUuid);
            ensureUserPersonalPlaylist(userUid, userName, trackUuid);
        });
    }

    private void refreshPlaylists() {
        this.allPlaylists = playlistDao.getAllPlaylists();
    }

    private void registerCommands() {
        register("!skip", (args) -> {
            reply("[color=orange]Saltando canción...[/color]");
            player.next();
        }, "!s");

        register("!stop", (args) -> {
            reply("[color=red]Bot apagado.[/color]");
            player.shutdown();
        }, "!st");

        register("!queue", (args) -> {
            String actual = player.getCurrentSongName();
            reply("[color=blue][b]Sonando:[/b][/color] [i]" + actual + "[/i]");
            List <String> canciones = player.getQueueList();
            reply("\t[color=purple][b]En cola:[/b][/color]");
            for (int i = 0; i < canciones.size(); i++) {
                reply("[color=purple]\t#" + (i + 1) + ".  [/color] " + canciones.get(i));
            }
        }, "!q");

        register("!vol", this::handleVolume, "!v", "!volumen");
        register("!shuffle", (args) -> {
            player.shuffle();
            reply("[color=purple]Cola mezclada.[/color]");
        });

        register("!remove", this::handleRemove, "!rm");
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
        if (!e.getMessage().trim().startsWith("!")) return;

        String userUid = e.getInvokerUniqueId();
        String userName = e.getInvokerName();
        String[] parts = raw.split("\\s+", 2);
        String label = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        System.out.println(e.getInvokerName() + " " + e.getMessage());
        switch (label) {
            case "!play","!p" -> handlePlay(args, userUid, userName, false);
            case "!playnext", "!pn"  -> handlePlay(args, userUid, userName, true);
            case "!playlist", "!pl" -> handleListPlaylist(args);
            case "!playplaylist", "!pp" -> handlePlayPlaylist(args, userUid, userName);
            case "!newplaylist","!newp" -> handleCreatePlaylist(args, userUid);
            case "!addtoplaylist","!addp" -> handleAddSongToPlaylist(args, userUid);
            case "!removetoplaylist", "!rmp" -> handleRemoveFromPlaylist(args, userUid);
            case "!intersect" -> handleIntersect(args);
            case "!union" -> handleUnion(args);
            case "!symdiff" -> handleSymDiff(args);
            case "!dislike" -> handleDislike(args, userUid, userName);

            case "!topsongs", "!ts" -> handleListSongsByUser(args,userUid, userName, true);
            case "!leastsongs", "!ls" -> handleListSongsByUser(args,userUid, userName, false);
            case "!topglobal", "!tg" -> handleListGlobalSongs(args, true);
            case "!leastglobal", "!lg" -> handleListGlobalSongs(args, false);
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
                Track track = musicManager.resolve(query);
                QueuedTrack queuedTrack = new QueuedTrack(track, userUid, userName);

                if (insertNext) {
                    player.queueNext(queuedTrack);
                    reply("[color=lime]Siguiente:[/color] [i]" + track +"[/i]");
                } else {
                    player.queue(queuedTrack);
                    reply("[color=blue]Añadido:[/color] [i]" + track + "[/i]");
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

            boolean isMine = playlist.getOwnerUid().equals(userUid);

            for (String uuid : uuids) {
                try {
                    Track track = musicManager.resolve(uuid);
                    QueuedTrack queuedTrack;

                    if (isMine) {
                        queuedTrack = new QueuedTrack(track, userUid, userName);
                    } else {
                        queuedTrack = new QueuedTrack(track, null, null);
                    }

                    player.queue(queuedTrack);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
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

    private void handleRemoveFromPlaylist(String args, String userUid) {
        String[] parts = args.split("\\s+", 2);

        if (parts.length < 2) {
            reply("[color=gray]Uso: !rmp <playlist> <canción>[/color]");
            return;
        }

        new Thread(() -> {
            try {
                Playlist playlist = resolvePlaylist(parts[0]);
                if (playlist == null) {
                    reply("[color=red]Playlist no encontrada.[/color]");
                    return;
                }

                if (!playlist.getOwnerUid().equals(userUid)) {
                    reply("[color=red]No tienes permiso para modificar esta playlist.[/color]");
                    return;
                }

                Track track = musicManager.resolve(parts[1]);

                boolean success = playlistDao.removeSongFromPlaylist(playlist.getId(), track.getUuid());

                if (success) {
                    reply("[color=lime]Eliminada[/color] [i]" + track.getTitle() + "[/i] [color=lime]de[/color] [b]" + playlist.getName() + "[/b]");
                } else {
                    reply("[color=orange]Esa canción no estaba en la playlist.[/color]");
                }

            } catch (Exception e) {
                reply("[color=red]Error al eliminar: " + e.getMessage() + "[/color]");
            }
        }).start();
    }

    private void handleDislike(String args, String userUid, String userName) {
        new Thread(() -> {
            try {
                String playlistName = "Música de " + userName;
                Playlist userPlaylist = null;

                // Refrescamos por si acaso
                refreshPlaylists();
                for (Playlist p : allPlaylists) {
                    if (p.getName().equals(playlistName) && p.getOwnerUid().equals(userUid)) {
                        userPlaylist = p;
                        break;
                    }
                }

                if (userPlaylist == null) {
                    reply("[color=red]No tienes una playlist de favoritos todavía.[/color]");
                    return;
                }

                Track trackToRemove;

                // 2. Determinar qué canción borrar
                if (args.isEmpty()) {
                    // Si no hay argumentos, intentamos borrar la que está sonando ACTUALMENTE
                    QueuedTrack current = player.getCurrentTrack(); // Asumiendo que tienes este metodo público en Scheduler
                    if (current == null) {
                        reply("[color=gray]Nada sonando. Uso: !dislike <canción>[/color]");
                        return;
                    }
                    trackToRemove = current.getTrack();
                } else {
                    trackToRemove = musicManager.resolve(args);
                }

                boolean success = playlistDao.removeSongFromPlaylist(userPlaylist.getId(), trackToRemove.getUuid());

                if (success) {
                    reply("[color=green]Eliminada de tus favoritos:[/color] " + trackToRemove.getTitle());
                } else {
                    reply("[color=orange]Esa canción no estaba en tus favoritos.[/color]");
                }

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
        reply("[color=blue][b]Playlists[/b][/color]");
        for (int i = 0; i < allPlaylists.size(); i++) {
            String name = allPlaylists.get(i).getName();
            reply("  [color=purple]#" + (i + 1) + "[/color] " + name);
        }
        reply(" ");
    }

    private void handleListPlaylist(String input) {
        Playlist playlist = resolvePlaylist(input);
        if (playlist == null) {
            reply("[color=red]Playlist no encontrada.[/color]");
            return;
        }
        List <Track>tracks = playlistDao.getTracksFromPlaylist(playlist);

        reply("[color=blue][b]" + playlist.getName() + ":[/b][/color]");
        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            reply("  [color=purple]\t#" + (i + 1) + ".[/color] " + track.getTitle() + " [color=purple]by[/color] " + track.getArtist());
        }
    }

    public void handleListSongsByUser(String arg,String userUid, String userName, boolean top) {
        int limit = arg.isEmpty() ? 5 : Integer.parseInt(arg);
        String playOrder = top ? "más" : "menos";
        List <PlayStats> topSongs = statsDao.getSongsByUser(userUid, limit, top);
        reply("[color=#ff0080][b]♕ Top " + playOrder + " escuchados de " + userName + " ♕[/b][/color]");
        for (int i = 0; i < topSongs.size(); i++) {
            reply("[color=darkgreen]" + "\t[b]#" + (i + 1)+ ".[/b][/color] " + topSongs.get(i).getTrack() + " ["+ topSongs.get(i).getPlayCount()+ "]");
        }
    }

    public void handleListGlobalSongs(String arg, boolean top) {
        int limit = arg.isEmpty() ? 5 : Integer.parseInt(arg);
        String playOrder = top ? "más" : "menos";
        List <PlayStats> topSongs = statsDao.getGlobalSongs(limit, top);

        reply("[color=#ff0080][b]♕ Top " + playOrder + " escuchados del servidor ♕[/b][/color]");
        for (int i = 0; i < topSongs.size(); i++) {
            reply("[color=darkgreen]" + "\t[b]#" + (i + 1)+ "[/b][/color] " + topSongs.get(i).getTrack()+ " ["+ topSongs.get(i).getPlayCount()+ "]");
        }
    }

    /*
     *  Conjuto de metodos para manejar los comandos basados
     *  en teoría de conjuntos para crear mezclas de playlists.
     */

    // Helper para obtener las listas de canciones ya barajadas y listas para usar
    private List<List<Track>> getPreprocessedPlaylists(String args) {
        // 1. Parsear IDs únicos
        List<Playlist> playlists = Arrays.stream(args.split("\\s+"))
                .map(s -> {
                    try { return Integer.parseInt(s.trim()) - 1; } catch (Exception e) { return -1; }
                })
                .filter(idx -> idx >= 0 && idx < allPlaylists.size())
                .map(idx -> allPlaylists.get(idx))
                .distinct()
                .toList();

        List<List<Track>> result = new ArrayList<>();

        // 2. Cargar Tracks y BARAJAR (Shuffle)
        for (Playlist p : playlists) {
            List<Track> tracks = playlistDao.getTracksFromPlaylist(p);
            if (!tracks.isEmpty()) {
                Collections.shuffle(tracks);
                result.add(tracks);
            }
        }
        return result;
    }

    public void handleIntersect(String args) {
        new Thread(() -> {
            List<List<Track>> lists = getPreprocessedPlaylists(args);
            if (lists.size() < 2) {
                reply("[color=red]Intersect requiere al menos 2 playlists.[/color]");
                return;
            }

            // Mapa de Frecuencia: UUID -> Cuántas playlists lo tienen
            Map<String, Integer> frequencyMap = new HashMap<>();

            // Llenamos el mapa
            for (List<Track> playlist : lists) {
                // Usamos un Set temporal para evitar contar doble si una playlist tiene la misma canción 2 veces
                Set<String> uniqueInPlaylist = new HashSet<>();
                for (Track t : playlist) uniqueInPlaylist.add(t.getUuid());

                for (String uuid : uniqueInPlaylist) {
                    frequencyMap.put(uuid, frequencyMap.getOrDefault(uuid, 0) + 1);
                }
            }

            // Filtramos: Solo pasan las que están en TODAS las listas
            int totalPlaylists = lists.size();
            List<Track> finalQueue = new ArrayList<>();

            // Para mantener el orden aleatorio, iteramos sobre la primera lista barajada
            // y verificamos si cumple la condición.
            for (Track t : lists.get(0)) {
                if (frequencyMap.getOrDefault(t.getUuid(), 0) == totalPlaylists) {
                    finalQueue.add(t);
                }
            }

            if (finalQueue.isEmpty()) {
                reply("[color=orange]No hay canciones en común (Intersección vacía).[/color]");
            } else {
                queueTracksAsync(finalQueue);
            }
        }).start();
    }

    public void handleUnion(String args) {
        new Thread(() -> {
            List<List<Track>> lists = getPreprocessedPlaylists(args);
            if (lists.isEmpty()) { reply("[color=red]No playlists válidas.[/color]"); return; }

            List<Track> finalQueue = new ArrayList<>();
            Set<String> seenUuids = new HashSet<>();

            // Convertimos a Iterators para el Round-Robin
            List<Iterator<Track>> iterators = new ArrayList<>();
            for (List<Track> l : lists) iterators.add(l.iterator());

            boolean elementsRemaining = true;
            while (elementsRemaining) {
                elementsRemaining = false;
                for (Iterator<Track> it : iterators) {
                    if (it.hasNext()) {
                        Track t = it.next();
                        // Solo pasa si es NUEVA (Set.add devuelve true)
                        if (seenUuids.add(t.getUuid())) {
                            finalQueue.add(t);
                        }
                        elementsRemaining = true;
                    }
                }
            }

            queueTracksAsync(finalQueue); // Tu metodo de encolar (con sleep 50ms)
        }).start();
    }

    public void handleSymDiff(String args) {
        new Thread(() -> {
            List<List<Track>> lists = getPreprocessedPlaylists(args);
            if (lists.size() < 2) {
                reply("[color=red]SymDiff requiere al menos 2 playlists.[/color]");
                return;
            }

            // 1. Calcular Frecuencias (Igual que en Intersect)
            Map<String, Integer> frequencyMap = new HashMap<>();
            for (List<Track> playlist : lists) {
                Set<String> uniqueInPlaylist = new HashSet<>();
                for (Track t : playlist) uniqueInPlaylist.add(t.getUuid());

                for (String uuid : uniqueInPlaylist) {
                    frequencyMap.put(uuid, frequencyMap.getOrDefault(uuid, 0) + 1);
                }
            }

            List<Track> finalQueue = new ArrayList<>();
            Set<String> processedUuids = new HashSet<>(); // Para no repetir

            // 2. Round-Robin (Para intercalar resultados de A, B y C)
            List<Iterator<Track>> iterators = new ArrayList<>();
            for (List<Track> l : lists) iterators.add(l.iterator());

            boolean elementsRemaining = true;
            while (elementsRemaining) {
                elementsRemaining = false;
                for (Iterator<Track> it : iterators) {
                    if (it.hasNext()) {
                        Track t = it.next();
                        String uuid = t.getUuid();

                        // CONDICIÓN MÁGICA:
                        // 1. No la hemos procesado ya
                        // 2. Su frecuencia es EXACTAMENTE 1 (Solo existe en esta playlist)
                        if (!processedUuids.contains(uuid)) {
                            if (frequencyMap.getOrDefault(uuid, 0) == 1) {
                                finalQueue.add(t);
                            }
                            processedUuids.add(uuid); // Marcamos como vista para no re-evaluar
                        }
                        elementsRemaining = true;
                    }
                }
            }

            if (finalQueue.isEmpty()) {
                reply("[color=orange]No hay canciones únicas. Todas se repiten en otras playlists.[/color]");
            } else {
                queueTracksAsync(finalQueue);
            }
        }).start();
    }

    private void queueTracksAsync(List<Track> tracks) {
        // 1. Validación inicial
        if (tracks.isEmpty()) {
            reply("[color=red]La operación resultó en una lista vacía (0 canciones).[/color]");
            return;
        }

        reply("[color=blue]Procesando y cargando " + tracks.size() + " canciones...[/color]");

        int successCount = 0;
        int failCount = 0;

        // 2. Iteración segura
        for (Track t : tracks) {
            try {
                Track trackToQueue = t;

                // 3. Verificación de Integridad (Auto-Healing)
                // Si el archivo físico fue borrado del disco, intentamos recuperarlo con resolve()
                File file = (t.getPath() != null) ? new File(t.getPath()) : null;

                if (file == null || !file.exists()) {
                    System.out.println("[Auto-Fix] Archivo perdido para: " + t.getTitle() + ". Re-descargando...");
                    // Esto fuerza a que Python/yt-dlp lo bajen de nuevo
                    trackToQueue = musicManager.resolve(t.getUuid());
                }

                // 4. Encolado
                // Pasamos null en usuario porque es una acción del sistema (playlist automática)
                player.queue(new QueuedTrack(trackToQueue, null, null));
                successCount++;

                // 5. Anti-Congelamiento (Throttle)

                Thread.sleep(50);

            } catch (Exception e) {
                failCount++;
                System.err.println("Error cargando track en batch: " + t.getUuid());
            }
        }

        // 6. Reporte Final
        if (failCount > 0) {
            reply("[color=lime]Carga finalizada.[/color] [color=orange](✅ " + successCount + " | ❌ " + failCount + ")[/color]");
        } else {
            reply("[color=lime]¡Carga completada exitosamente! (" + successCount + " canciones)[/color]");
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
        reply("[color=blue][b]Comandos[/b][/color]\n");
        reply(" ");
        reply("[color=purple]Reproducción[/color]");
        reply("  !play or !p <canción>\t Añadir a la cola");
        reply("  !playnext or !pn <canción>\t Poner como siguiente");
        reply("  !skip or !s\t Saltar canción");
        reply("  !stop\t Detener todo");
        reply("  !queue or !q\t Ver cola actual");
        reply("  !remove <#> or !rm\t Quitar de la cola");
        reply("  !vol <0-100>\t Ajustar volumen");
        reply("  !shuffle\t Mezclar cola");
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