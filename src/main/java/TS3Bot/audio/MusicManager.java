package TS3Bot.audio;

import TS3Bot.db.TrackDAO;
import TS3Bot.model.Track;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class MusicManager {
    private final TrackDAO dao;

    public MusicManager() {
        this.dao = new TrackDAO();
    }

    public Track resolve(String query) throws Exception {
        String uuid = null;

        // Limpiamos espacios accidentales
        query = query.trim();

        // 0. VÍA DIRECTA (Si me pasas el ID crudo: "dQw4w9WgXcQ")
        // Un ID de YouTube tiene exactamente 11 caracteres y solo letras/numeros/-/_
        if (query.matches("^[a-zA-Z0-9_-]{11}$")) {
            Track t = dao.getTrack(query);
            // Si existe en DB y el archivo físico está ahí, retornamos YA.
            if (t != null && t.getPath() != null && new File(t.getPath()).exists()) {
                System.out.println("[Manager] Track encontrado (UUID Directo): " + t);
                return t;
            }
            // Si no está en la DB, no pasa nada, dejamos que el código siga
            // para que Python descargue los metadatos (Título, Artista) de ese ID.
        }

        // 1. VIA RÁPIDA (Link (contiene el id))
        if (query.contains("youtube.com") || query.contains("youtu.be")) {
            if (query.contains("v=")) uuid = query.split("v=")[1].split("&")[0];

            else if (query.contains("youtu.be/")) uuid = query.split("youtu.be/")[1].split("\\?")[0];

            if (uuid != null) {
                Track t = dao.getTrack(uuid);
                // PROTECCIÓN: Validamos que t.getPath() no sea null
                if (t != null && t.getPath() != null && new File(t.getPath()).exists()) {
                    System.out.println("[Manager] Track encontrado (Link): " + t);
                    return t;
                }
            }
        }

        // 2. CONSULTA A PYTHON (Recibimos Track sin path)
        Track track = YouTubeHelper.getMetadataViaSocket(query);
        uuid = track.getUuid();

        // 3. RE-VERIFICACIÓN DB (Por si buscamos texto de algo que ya teníamos)
        Track cached = dao.getTrack(uuid);
        if (cached != null && new File(cached.getPath()).exists()) {
            System.out.println("[Manager] Track encontrado (Búsqueda): " + cached   );
            return cached;
        }

        // 4. DESCARGA
        File file = YouTubeHelper.downloadCompressed(track);
        System.out.println("[Manager] Track descargado: " + track);

        // 5. COMPLETAR Y GUARDAR
        track.setPath(file.getAbsolutePath());
        dao.saveTrack(track);

        return track;
    }
}