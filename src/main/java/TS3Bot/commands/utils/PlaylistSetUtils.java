package TS3Bot.commands.utils;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.Playlist;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class PlaylistSetUtils {

    private final TeamSpeakBot bot;

    public PlaylistSetUtils(TeamSpeakBot bot) {
        this.bot = bot;
    }

    public void handleIntersect(CommandContext ctx) {
        new Thread(() -> {
            List<Playlist> selectedPlaylists = resolvePlaylists(ctx.getArgs());

            if (selectedPlaylists.size() < 2) {
                bot.replyError("Intersect requiere al menos 2 playlists válidas.");
                return;
            }

            Map<String, Set<String>> creditsMap = buildCreditsMap(selectedPlaylists);
            List<List<Track>> lists = extractTracksFromPlaylists(selectedPlaylists);

            Map<String, Integer> frequencyMap = new HashMap<>();
            for (List<Track> playlist : lists) {
                Set<String> uniqueInPlaylist = new HashSet<>();
                for (Track t : playlist) uniqueInPlaylist.add(t.getUuid());

                for (String uuid : uniqueInPlaylist) {
                    frequencyMap.put(uuid, frequencyMap.getOrDefault(uuid, 0) + 1);
                }
            }

            int totalPlaylists = lists.size();
            List<Track> finalQueue = new ArrayList<>();

            for (Track t : lists.get(0)) {
                if (frequencyMap.getOrDefault(t.getUuid(), 0) == totalPlaylists) {
                    finalQueue.add(t);
                }
            }

            if (finalQueue.isEmpty()) {
                bot.replyAction("No hay canciones en común (Intersección vacía).");
            } else {
                queueTracksAsync(finalQueue, creditsMap);
            }
        }).start();
    }

    public void handleUnion(CommandContext ctx) {
        new Thread(() -> {
            List<Playlist> selectedPlaylists = resolvePlaylists(ctx.getArgs());

            if (selectedPlaylists.isEmpty()) {
                bot.replyError("No se encontraron playlists válidas.");
                return;
            }

            Map<String, Set<String>> creditsMap = buildCreditsMap(selectedPlaylists);
            List<List<Track>> lists = extractTracksFromPlaylists(selectedPlaylists);

            List<Track> finalQueue = new ArrayList<>();
            Set<String> seenUuids = new HashSet<>();

            List<Iterator<Track>> iterators = new ArrayList<>();
            for (List<Track> l : lists) iterators.add(l.iterator());

            boolean elementsRemaining = true;
            while (elementsRemaining) {
                elementsRemaining = false;
                for (Iterator<Track> it : iterators) {
                    if (it.hasNext()) {
                        Track t = it.next();
                        if (seenUuids.add(t.getUuid())) {
                            finalQueue.add(t);
                        }
                        elementsRemaining = true;
                    }
                }
            }

            queueTracksAsync(finalQueue, creditsMap);
        }).start();
    }

    public void handleSymDiff(CommandContext ctx) {
        new Thread(() -> {
            List<Playlist> selectedPlaylists = resolvePlaylists(ctx.getArgs());

            if (selectedPlaylists.size() < 2) {
                bot.replyError("SymDiff requiere al menos 2 playlists válidas.");
                return;
            }

            Map<String, Set<String>> creditsMap = buildCreditsMap(selectedPlaylists);
            List<List<Track>> lists = extractTracksFromPlaylists(selectedPlaylists);

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
                bot.replyAction("No hay canciones únicas. Todas se repiten en otras playlists.");
            } else {
                queueTracksAsync(finalQueue, creditsMap);
            }
        }).start();
    }

    private List<Playlist> resolvePlaylists(String args) {
        List<Playlist> allPlaylists = bot.getAllPlaylists();
        return Arrays.stream(args.split("\\s+"))
                .map(s -> {
                    try { return Integer.parseInt(s.trim()) - 1; } catch (Exception e) { return -1; }
                })
                .filter(idx -> idx >= 0 && idx < allPlaylists.size())
                .map(allPlaylists::get)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<List<Track>> extractTracksFromPlaylists(List<Playlist> playlists) {
        List<List<Track>> result = new ArrayList<>();
        for (Playlist p : playlists) {
            List<Track> tracks = bot.getPlaylistManager().getTracksFromPlaylist(p);
            if (tracks != null && !tracks.isEmpty()) {
                List<Track> copy = new ArrayList<>(tracks);
                Collections.shuffle(copy);
                result.add(copy);
            }
        }
        return result;
    }

    private Map<String, Set<String>> buildCreditsMap(List<Playlist> playlists) {
        Map<String, Set<String>> map = new HashMap<>();

        for (Playlist p : playlists) {
            String ownerName = p.getOwnerName();

            if (ownerName == null || ownerName.isEmpty()) {
                if (p.getName().startsWith("Música de ")) {
                    ownerName = p.getName().substring("Música de ".length());
                } else {
                    ownerName = "Usuario";
                }
            }

            List<Track> tracks = bot.getPlaylistManager().getTracksFromPlaylist(p);
            if (tracks != null) {
                for (Track t : tracks) {
                    map.computeIfAbsent(t.getUuid(), k -> new LinkedHashSet<>()).add(ownerName);
                }
            }
        }
        return map;
    }

    private void queueTracksAsync(List<Track> tracks, Map<String, Set<String>> creditsMap) {
        if (tracks.isEmpty()) {
            bot.replyError("La operación resultó en una lista vacía.");
            return;
        }

        bot.reply("[color=blue]Procesando y cargando " + tracks.size() + " canciones...[/color]");

        int successCount = 0;
        int failCount = 0;

        for (Track t : tracks) {
            try {
                Track trackToQueue = t;

                File file = (t.getPath() != null) ? new File(t.getPath()) : null;
                if (file == null || !file.exists()) {
                    System.out.println("[Auto-Fix] Re-descargando: " + t.getTitle());
                    trackToQueue = bot.getMusicManager().resolve(t.getUuid());
                }

                String joinedNames = "";

                if (creditsMap != null && creditsMap.containsKey(t.getUuid())) {
                    Set<String> owners = creditsMap.get(t.getUuid());
                    joinedNames = String.join(" & ", owners);
                }

                bot.getPlayer().queue(new QueuedTrack(trackToQueue, null, joinedNames, false));
                successCount++;

            } catch (Exception e) {
                failCount++;
                System.err.println("Error cargando track " + t.getUuid());
            }
        }

        if (failCount > 0) {
            bot.reply("[color=lime]Carga finalizada.[/color] [color=orange](✅ " + successCount + " | ❌ " + failCount + ")[/color]");
        } else {
            bot.replySuccess("¡Carga completada exitosamente! (" + successCount + " canciones)");
        }
    }
}