package TS3Bot.commands.utils;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.Playlist;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.ShuffleMode;
import TS3Bot.model.Track;
import TS3Bot.services.AutoDjService;
import TS3Bot.utils.HarmonicUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class PlaylistSetUtils {

    private final TeamSpeakBot bot;
    private final AutoDjService autoDjService;

    public PlaylistSetUtils(TeamSpeakBot bot) {
        this.bot = bot;
        this.autoDjService = new AutoDjService();
    }

    public void handleIntersect(CommandContext ctx) {
        new Thread(() -> {
            ShuffleMode mode = resolveShuffleMode(ctx);
            List<Playlist> selectedPlaylists = resolvePlaylists(ctx.getArgs());

            if (selectedPlaylists.size() < 2) {
                bot.replyError("Intersect requiere al menos 2 playlists validas.");
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
            List<Track> intersection = new ArrayList<>();
            for (Track t : lists.get(0)) {
                if (frequencyMap.getOrDefault(t.getUuid(), 0) == totalPlaylists) {
                    intersection.add(t);
                }
            }

            if (intersection.isEmpty()) {
                bot.replyWarning("No hay canciones en comun.");
            } else {
                List<Track> sorted = autoDjService.shuffleTracks(intersection, mode);
                queueTracksAsync(sorted, creditsMap, mode);
            }
        }).start();
    }

    public void handleUnion(CommandContext ctx) {
        new Thread(() -> {
            ShuffleMode mode = resolveShuffleMode(ctx);
            List<Playlist> selectedPlaylists = resolvePlaylists(ctx.getArgs());

            if (selectedPlaylists.isEmpty()) {
                bot.replyError("No se encontraron playlists validas.");
                return;
            }

            Map<String, Set<String>> creditsMap = buildCreditsMap(selectedPlaylists);
            List<List<Track>> lists = extractTracksFromPlaylists(selectedPlaylists);

            List<Track> finalQueue = smartInterleave(lists, mode);

            queueTracksAsync(finalQueue, creditsMap, mode);
        }).start();
    }

    public void handleSymDiff(CommandContext ctx) {
        new Thread(() -> {
            ShuffleMode mode = resolveShuffleMode(ctx);
            List<Playlist> selectedPlaylists = resolvePlaylists(ctx.getArgs());

            if (selectedPlaylists.size() < 2) {
                bot.replyError("SymDiff requiere al menos 2 playlists validas.");
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

            List<List<Track>> uniqueLists = new ArrayList<>();
            for (List<Track> originalList : lists) {
                List<Track> uniqueInThis = new ArrayList<>();
                for (Track t : originalList) {
                    if (frequencyMap.getOrDefault(t.getUuid(), 0) == 1) {
                        uniqueInThis.add(t);
                    }
                }
                uniqueLists.add(uniqueInThis);
            }

            List<Track> finalQueue = smartInterleave(uniqueLists, mode);

            if (finalQueue.isEmpty()) {
                bot.replyWarning("No hay canciones unicas.");
            } else {
                queueTracksAsync(finalQueue, creditsMap, mode);
            }
        }).start();
    }

    private List<Track> smartInterleave(List<List<Track>> sources, ShuffleMode mode) {
        List<Track> result = new ArrayList<>();
        Set<String> seenUuids = new HashSet<>();

        List<LinkedList<Track>> pools = new ArrayList<>();
        for (List<Track> src : sources) {
            pools.add(new LinkedList<>(src));
        }

        if (pools.isEmpty()) return result;

        if (mode == ShuffleMode.CHAOS) {
            return legacyInterleave(pools, seenUuids);
        }

        int currentPoolIndex = 0;
        Track lastTrack = null;
        boolean tracksRemaining = true;

        if (!pools.get(0).isEmpty()) {
            for(LinkedList<Track> p : pools) Collections.shuffle(p);

            Track start = pools.get(0).pop();
            if (seenUuids.add(start.getUuid())) {
                result.add(start);
                lastTrack = start;
            }
            currentPoolIndex = 1 % pools.size();
        }

        while (tracksRemaining) {
            tracksRemaining = false;

            for (int i = 0; i < pools.size(); i++) {
                LinkedList<Track> currentPool = pools.get(currentPoolIndex);
                currentPool.removeIf(t -> seenUuids.contains(t.getUuid()));

                if (!currentPool.isEmpty()) {
                    tracksRemaining = true;
                    Track pick;

                    if (lastTrack == null) {
                        pick = currentPool.pop();
                    } else {
                        pick = findBestMatch(lastTrack, currentPool, mode);
                        currentPool.remove(pick);
                    }

                    if (seenUuids.add(pick.getUuid())) {
                        result.add(pick);
                        lastTrack = pick;
                    }
                }

                currentPoolIndex = (currentPoolIndex + 1) % pools.size();
            }
        }

        return result;
    }

    private Track findBestMatch(Track current, List<Track> candidates, ShuffleMode mode) {
        if (mode == ShuffleMode.RISING || mode == ShuffleMode.FALLING || mode == ShuffleMode.WAVE) {
            return findClosestBpm(current, candidates);
        }

        Track bestMatch = null;
        int highestScore = -1;

        for (Track candidate : candidates) {
            int score = HarmonicUtils.getCompatibilityScore(current, candidate);
            if (score >= 90) return candidate;

            if (score > highestScore) {
                highestScore = score;
                bestMatch = candidate;
            }
        }

        if (highestScore <= 0 || bestMatch == null) {
            return findClosestBpm(current, candidates);
        }

        return bestMatch;
    }

    private Track findClosestBpm(Track current, List<Track> candidates) {
        Track best = candidates.get(0);
        int minDiff = Integer.MAX_VALUE;

        for (Track t : candidates) {
            if (current.getBpm() == 0 || t.getBpm() == 0) continue;
            int diff = Math.abs(current.getBpm() - t.getBpm());
            if (diff < minDiff) {
                minDiff = diff;
                best = t;
            }
        }
        return best;
    }

    private List<Track> legacyInterleave(List<LinkedList<Track>> pools, Set<String> seenUuids) {
        List<Track> result = new ArrayList<>();
        boolean remaining = true;
        for(LinkedList<Track> p : pools) Collections.shuffle(p);

        int idx = 0;
        while(remaining) {
            remaining = false;
            for(int i=0; i<pools.size(); i++) {
                LinkedList<Track> pool = pools.get(idx);
                if(!pool.isEmpty()) {
                    Track t = pool.pop();
                    if(seenUuids.add(t.getUuid())) {
                        result.add(t);
                    }
                    remaining = true;
                }
                idx = (idx + 1) % pools.size();
            }
        }
        return result;
    }

    private ShuffleMode resolveShuffleMode(CommandContext ctx) {
        if (ctx.hasFlag("harmonic") || ctx.hasFlag("h")) return ShuffleMode.HARMONIC;
        if (ctx.hasFlag("rising") || ctx.hasFlag("r")) return ShuffleMode.RISING;
        if (ctx.hasFlag("falling") || ctx.hasFlag("f")) return ShuffleMode.FALLING;
        if (ctx.hasFlag("wave") || ctx.hasFlag("w")) return ShuffleMode.WAVE;
        return ShuffleMode.CHAOS;
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
                result.add(new ArrayList<>(tracks));
            }
        }
        return result;
    }

    private Map<String, Set<String>> buildCreditsMap(List<Playlist> playlists) {
        Map<String, Set<String>> map = new HashMap<>();
        for (Playlist p : playlists) {
            String ownerName = p.getOwnerName();
            if (ownerName == null || ownerName.isEmpty()) {
                if (p.getName().startsWith("Musica de ")) {
                    ownerName = p.getName().substring("Musica de ".length());
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

    private void queueTracksAsync(List<Track> tracks, Map<String, Set<String>> creditsMap, ShuffleMode mode) {
        if (tracks.isEmpty()) {
            bot.replyWarning("La operacion resulto en una lista vacia.");
            return;
        }

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

        String modeSuffix = (mode != ShuffleMode.CHAOS) ? " usando algoritmo " + mode.name() : "";

        if (failCount > 0) {
            bot.replyWarning("Carga finalizada. (" + successCount + " | " + failCount + ")" + modeSuffix);
        } else {
            bot.replySuccess(successCount + " canciones cargadas a la cola" + modeSuffix);
        }
    }
}