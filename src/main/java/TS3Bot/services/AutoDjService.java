package TS3Bot.services;

import TS3Bot.model.QueuedTrack;
import TS3Bot.model.ShuffleMode;
import TS3Bot.model.Track;
import TS3Bot.utils.HarmonicUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AutoDjService {

    private final Random random = new Random();

    /**
     * Shufflea lista de Tracks (para playlists sin encolar).
     */
    public List<Track> shuffleTracks(List<Track> tracks, ShuffleMode mode) {
        if (tracks == null || tracks.size() <= 1) return tracks;

        List<Track> playlist = new ArrayList<>(tracks);

        switch (mode) {
            case HARMONIC:
                return smartHarmonicShuffle(playlist);
            case RISING:
                return bpmSort(playlist, true);
            case FALLING:
                return bpmSort(playlist, false);
            case WAVE:
                return waveShuffle(playlist);
            case CHAOS:
            default:
                Collections.shuffle(playlist);
                return playlist;
        }
    }

    /**
     * Shufflea QueuedTracks (para cola actual) preservando metadatos.
     */
    public List<QueuedTrack> shuffleQueue(List<QueuedTrack> queuedTracks, ShuffleMode mode) {
        if (queuedTracks == null || queuedTracks.size() <= 1) {
            return queuedTracks;
        }

        List<Track> justTracks = queuedTracks.stream()
                .map(QueuedTrack::getTrack)
                .collect(Collectors.toList());

        List<Track> reordered = shuffleTracks(justTracks, mode);

        Map<String, QueuedTrack> uuidMap = new HashMap<>();
        for (QueuedTrack qt : queuedTracks) {
            uuidMap.put(qt.getTrack().getUuid(), qt);
        }

        List<QueuedTrack> result = new ArrayList<>();
        for (Track t : reordered) {
            result.add(uuidMap.get(t.getUuid()));
        }

        return result;
    }

    // --- 1. HARMONIC (El DJ Inteligente) ---
    // Busca el camino más suave entre canciones usando BPM y Key
    private List<Track> smartHarmonicShuffle(List<Track> input) {
        LinkedList<Track> pool = new LinkedList<>(input);
        List<Track> sorted = new ArrayList<>();

        // Empezamos con una al azar
        Track current = pool.remove(random.nextInt(pool.size()));
        sorted.add(current);

        while (!pool.isEmpty()) {
            Track bestMatch = null;
            int highestScore = -1;

            // Buscamos la mejor pareja para la canción actual
            for (Track candidate : pool) {
                int score = HarmonicUtils.getCompatibilityScore(current, candidate);

                // Si encontramos un "mix perfecto" (ej: score >= 90), lo tomamos ya (Greedy)
                if (score >= 90) {
                    bestMatch = candidate;
                    break;
                }

                if (score > highestScore) {
                    highestScore = score;
                    bestMatch = candidate;
                }
            }

            // Si no hay buen match (score 0), metemos algo random para romper
            if (highestScore <= 0 || bestMatch == null) {
                bestMatch = pool.get(random.nextInt(pool.size()));
            }

            pool.remove(bestMatch);
            sorted.add(bestMatch);
            current = bestMatch;
        }
        return sorted;
    }

    // --- 2. RISING / FALLING (Energía) ---
    // Ordena por BPM. Las canciones sin BPM (0) van al final.
    private List<Track> bpmSort(List<Track> input, boolean ascending) {
        List<Track> withBpm = input.stream()
                .filter(t -> t.getBpm() > 0)
                .collect(Collectors.toList());

        List<Track> noBpm = input.stream()
                .filter(t -> t.getBpm() == 0)
                .collect(Collectors.toList());

        // Ordenamos
        withBpm.sort(Comparator.comparingInt(Track::getBpm));

        if (!ascending) {
            Collections.reverse(withBpm);
        }

        // Unimos: Primero las ordenadas, luego las "basura" (sin BPM)
        withBpm.addAll(noBpm);
        return withBpm;
    }

    // --- 3. WAVE (Montaña Rusa) ---
    // Alterna: Lento -> Rápido -> Lento -> Rápido...
    private List<Track> waveShuffle(List<Track> input) {
        List<Track> withBpm = input.stream()
                .filter(t -> t.getBpm() > 0)
                .sorted(Comparator.comparingInt(Track::getBpm))
                .collect(Collectors.toList());

        List<Track> noBpm = input.stream()
                .filter(t -> t.getBpm() == 0)
                .collect(Collectors.toList());

        List<Track> result = new ArrayList<>();

        int left = 0;               // Puntero al más lento
        int right = withBpm.size() - 1; // Puntero al más rápido
        boolean takeFromLeft = true;

        while (left <= right) {
            if (takeFromLeft) {
                result.add(withBpm.get(left++));
            } else {
                result.add(withBpm.get(right--));
            }
            takeFromLeft = !takeFromLeft; // Alternar
        }

        result.addAll(noBpm);
        return result;
    }
}