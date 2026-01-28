package TS3Bot.audio;

import TS3Bot.audio.validation.AutoTrackValidator;
import TS3Bot.db.TrackDAO;
import TS3Bot.model.Track;
import java.io.File;

public class MusicManager {
    private final TrackDAO dao;
    private final AutoTrackValidator validator;

    public MusicManager() {
        this.dao = new TrackDAO();
        this.validator = new AutoTrackValidator();
    }

    public Track resolve(String query) throws Exception {
        Track track = resolveTrackSource(query); // (Lógica extraída abajo para limpieza)

        if (track == null) throw new Exception("No se pudo resolver el track.");

        // 1. VALIDACIÓN RÁPIDA (Síncrona)
        // Repara foto, album, titulo si faltan. Es rápido.
        if (validator.validateAndRepair(track)) {
            dao.saveTrack(track);
        }

        // 2. ANÁLISIS PROFUNDO (Asíncrono / Cola)
        // Esto obtiene BPM y Key en segundo plano sin detener la música.
        if (track.isDownloaded()) {
            AnalysisScheduler.submit(track);
        }

        return track;
    }

    // Tu lógica original de resolución, organizada:
    private Track resolveTrackSource(String query) throws Exception {
        query = query.trim();
        String uuid = null;

        // A. Vía Directa (UUID)
        if (query.matches("^[a-zA-Z0-9_-]{11}$")) {
            Track t = dao.getTrack(query);
            if (t != null && t.isDownloaded()) return t;
            uuid = query; // Si no está descargado, usamos el UUID para bajarlo
        }

        // B. Vía Link
        if (uuid == null && (query.contains("youtube.com") || query.contains("youtu.be"))) {
            uuid = extractUUID(query);
            if (uuid != null) {
                Track t = dao.getTrack(uuid);
                if (t != null && t.isDownloaded()) return t;
            }
        }

        // C. Descarga (Si no estaba en DB o es búsqueda)
        // Si ya tenemos UUID pero no archivo, evitamos llamar a Python metadata innecesariamente
        Track track;
        if (uuid != null) {
            // Si tenemos UUID, podemos ir directo a metadata por ID
            track = MetadataClient.getMetadata(uuid);
        } else {
            // Búsqueda de texto normal
            track = MetadataClient.getMetadata(query);
        }

        // Descargamos
        File file = MetadataClient.downloadCompressed(track);
        track.setPath(file.getAbsolutePath());
        dao.saveTrack(track);

        return track;
    }

    private String extractUUID(String url) {
        // Tu metodo extractUUID original
        if (url.contains("v=")) return url.split("v=")[1].split("&")[0];
        else if (url.contains("youtu.be/")) return url.split("youtu.be/")[1].split("\\?")[0];
        return null;
    }
}