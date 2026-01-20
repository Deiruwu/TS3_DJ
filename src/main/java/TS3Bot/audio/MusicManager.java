package TS3Bot.audio;

import TS3Bot.db.TrackDAO;
import TS3Bot.model.Track;
import java.io.File;

public class MusicManager {
    private final TrackDAO dao;

    public MusicManager() {
        this.dao = new TrackDAO();
    }

    public Track resolve(String query) throws Exception {
        String uuid = null;

        // Limpiamos espacios accidentales
        query = query.trim();

        // 0. VÍA DIRECTA (UUID: 11 chars)
        if (query.matches("^[a-zA-Z0-9_-]{11}$")) {
            Track t = dao.getTrack(query);
            if (t != null && t.getPath() != null && new File(t.getPath()).exists()) {
                System.out.println("[Manager] Track encontrado (UUID Directo): " + t);
                enrichMetadataAsync(t);
                return t;
            }
        }

        // 1. VÍA RÁPIDA (Link)
        if (query.contains("youtube.com") || query.contains("youtu.be")) {
            if (query.contains("v=")) uuid = query.split("v=")[1].split("&")[0];
            else if (query.contains("youtu.be/")) uuid = query.split("youtu.be/")[1].split("\\?")[0];

            if (uuid != null) {
                Track t = dao.getTrack(uuid);
                if (t != null && t.getPath() != null && new File(t.getPath()).exists()) {
                    System.out.println("[Manager] Track encontrado (Link): " + t);
                    enrichMetadataAsync(t);
                    return t;
                }
            }
        }

        // 2. CONSULTA A PYTHON (Metadatos frescos)
        Track track = YouTubeHelper.getMetadataViaSocket(query);
        uuid = track.getUuid();

        // 3. RE-VERIFICACIÓN DB (Cache local + Metadatos frescos)
        Track cached = dao.getTrack(uuid);
        if (cached != null && new File(cached.getPath()).exists()) {
            System.out.println("[Manager] Track encontrado (Búsqueda): " + cached);
            return cached;
        }

        // 4. DESCARGA
        File file = YouTubeHelper.downloadCompressed(track);
        System.out.println("[Manager] Track descargado: " + track);

        // 5. GUARDAR PATH
        track.setPath(file.getAbsolutePath());
        dao.saveTrack(track);

        return track;
    }

    /**
     * Actualiza metadatos (imagen/avatar) en segundo plano sin bloquear la música.
     */
    private void enrichMetadataAsync(Track track) {
        new Thread(() -> {
            try {
                Track meta = YouTubeHelper.getMetadataViaSocket(track.getUuid());
                if (meta != null) {
                    System.out.println("[Manager] Metadatos background OK: " + track.getTitle());
                }
            } catch (Exception e) {
                System.err.println("[Manager] Error metadata async: " + e.getMessage());
            }
        }).start();
    }
}