package TS3Bot.commands.utils;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.Playlist;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase de utilidad para manejar operaciones de teoría de conjuntos en playlists.
 * Contiene la lógica para Intersección, Unión y Diferencia Simétrica.
 */
public class PlaylistSetUtils {

    private final TeamSpeakBot bot;

    public PlaylistSetUtils(TeamSpeakBot bot) {
        this.bot = bot;
    }

    // --- MÉTODOS PÚBLICOS (Handlers) ---

    public void handleIntersect(CommandContext ctx) {
        new Thread(() -> {
            List<List<Track>> lists = getPreprocessedPlaylists(ctx.getArgs());
            if (lists.size() < 2) {
                bot.reply("[color=red]Intersect requiere al menos 2 playlists válidas.[/color]");
                return;
            }

            // Mapa de Frecuencia: UUID -> Cuántas playlists lo tienen
            Map<String, Integer> frequencyMap = new HashMap<>();

            // Llenamos el mapa
            for (List<Track> playlist : lists) {
                Set<String> uniqueInPlaylist = new HashSet<>();
                for (Track t : playlist) uniqueInPlaylist.add(t.getUuid());

                for (String uuid : uniqueInPlaylist) {
                    frequencyMap.put(uuid, frequencyMap.getOrDefault(uuid, 0) + 1);
                }
            }

            int totalPlaylists = lists.size();
            List<Track> finalQueue = new ArrayList<>();

            // Iteramos sobre la primera lista barajada para mantener orden aleatorio
            for (Track t : lists.get(0)) {
                if (frequencyMap.getOrDefault(t.getUuid(), 0) == totalPlaylists) {
                    finalQueue.add(t);
                }
            }

            if (finalQueue.isEmpty()) {
                bot.reply("[color=orange]No hay canciones en común (Intersección vacía).[/color]");
            } else {
                queueTracksAsync(ctx, finalQueue);
            }
        }).start();
    }

    public void handleUnion(CommandContext ctx) {
        new Thread(() -> {
            List<List<Track>> lists = getPreprocessedPlaylists(ctx.getArgs());
            if (lists.isEmpty()) {
                bot.reply("[color=red]No se encontraron playlists válidas.[/color]");
                return;
            }

            List<Track> finalQueue = new ArrayList<>();
            Set<String> seenUuids = new HashSet<>();

            // Round-Robin
            List<Iterator<Track>> iterators = new ArrayList<>();
            for (List<Track> l : lists) iterators.add(l.iterator());

            boolean elementsRemaining = true;
            while (elementsRemaining) {
                elementsRemaining = false;
                for (Iterator<Track> it : iterators) {
                    if (it.hasNext()) {
                        Track t = it.next();
                        if (seenUuids.add(t.getUuid())) { // Solo si es nueva
                            finalQueue.add(t);
                        }
                        elementsRemaining = true;
                    }
                }
            }

            queueTracksAsync(ctx, finalQueue);
        }).start();
    }

    public void handleSymDiff(CommandContext ctx) {
        new Thread(() -> {
            List<List<Track>> lists = getPreprocessedPlaylists(ctx.getArgs());
            if (lists.size() < 2) {
                bot.reply("[color=red]SymDiff requiere al menos 2 playlists válidas.[/color]");
                return;
            }

            Map<String, Integer> frequencyMap = new HashMap<>();
            for (List<Track> playlist : lists) {
                Set<String> uniqueInPlaylist = new HashSet<>();
                for (Track t : playlist) uniqueInPlaylist.add(t.getUuid());

                for (String uuid : uniqueInPlaylist) {
                    frequencyMap.put(uuid, frequencyMap.getOrDefault(uuid, 0) + 1);
                }
            }

            List<Track> finalQueue = new ArrayList<>();
            Set<String> processedUuids = new HashSet<>();

            List<Iterator<Track>> iterators = new ArrayList<>();
            for (List<Track> l : lists) iterators.add(l.iterator());

            boolean elementsRemaining = true;
            while (elementsRemaining) {
                elementsRemaining = false;
                for (Iterator<Track> it : iterators) {
                    if (it.hasNext()) {
                        Track t = it.next();
                        String uuid = t.getUuid();

                        if (!processedUuids.contains(uuid)) {
                            // Solo agregamos si su frecuencia es 1 (única en su lista)
                            if (frequencyMap.getOrDefault(uuid, 0) == 1) {
                                finalQueue.add(t);
                            }
                            processedUuids.add(uuid);
                        }
                        elementsRemaining = true;
                    }
                }
            }

            if (finalQueue.isEmpty()) {
                bot.reply("[color=orange]No hay canciones únicas. Todas se repiten en otras playlists.[/color]");
            } else {
                queueTracksAsync(ctx, finalQueue);
            }
        }).start();
    }

    // --- HELPERS PRIVADOS ---

    private List<List<Track>> getPreprocessedPlaylists(String args) {
        List<Playlist> allPlaylists = bot.getAllPlaylists(); // Asumiendo este getter

        // 1. Parsear IDs únicos
        List<Playlist> playlists = Arrays.stream(args.split("\\s+"))
                .map(s -> {
                    try { return Integer.parseInt(s.trim()) - 1; } catch (Exception e) { return -1; }
                })
                .filter(idx -> idx >= 0 && idx < allPlaylists.size())
                .map(allPlaylists::get)
                .distinct()
                .collect(Collectors.toList());

        List<List<Track>> result = new ArrayList<>();

        // 2. Cargar Tracks y BARAJAR
        for (Playlist p : playlists) {
            // Asumiendo getter en manager o dao
            List<Track> tracks = bot.getPlaylistManager().getTracksFromPlaylist(p);
            // NOTA: Si usas getTracksFromPlaylist(p), ajusta la línea de arriba.

            if (tracks != null && !tracks.isEmpty()) {
                // Hacemos una copia para no desordenar la playlist original en memoria si fuera estática
                List<Track> copy = new ArrayList<>(tracks);
                Collections.shuffle(copy);
                result.add(copy);
            }
        }
        return result;
    }

    private void queueTracksAsync(CommandContext ctx, List<Track> tracks) {
        if (tracks.isEmpty()) {
            bot.reply("[color=red]La operación resultó en una lista vacía.[/color]");
            return;
        }

        bot.reply("[color=blue]Procesando y cargando " + tracks.size() + " canciones...[/color]");

        int successCount = 0;
        int failCount = 0;

        for (Track t : tracks) {
            try {
                Track trackToQueue = t;

                // Verificación de archivo físico
                File file = (t.getPath() != null) ? new File(t.getPath()) : null;
                if (file == null || !file.exists()) {
                    System.out.println("[Auto-Fix] Re-descargando: " + t.getTitle());
                    trackToQueue = bot.getMusicManager().resolve(t.getUuid());
                }

                bot.getPlayer().queue(new QueuedTrack(trackToQueue, null, null));
                successCount++;

                Thread.sleep(50); // Throttle

            } catch (Exception e) {
                failCount++;
                System.err.println("Error cargando track " + t.getUuid());
            }
        }

        if (failCount > 0) {
            bot.reply("[color=lime]Carga finalizada.[/color] [color=orange](✅ " + successCount + " | ❌ " + failCount + ")[/color]");
        } else {
            bot.reply("[color=lime]¡Carga completada exitosamente! (" + successCount + " canciones)[/color]");
        }
    }
}