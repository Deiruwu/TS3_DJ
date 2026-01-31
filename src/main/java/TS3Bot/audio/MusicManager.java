package TS3Bot.audio;

import TS3Bot.audio.validation.AutoTrackValidator;
import TS3Bot.commands.options.PlayOptions;
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

    public Track resolve(String query) throws Exception{
        return resolve(query, PlayOptions.defaults());
    }

    public Track resolve(String query, PlayOptions options) throws Exception {
        Track track = resolveTrackSource(query, options);

        if (track == null) {
            throw new Exception("No se pudo resolver el track.");
        }

        if (validator.validateAndRepair(track)) {
            dao.saveTrack(track);
        }

        if (track.isDownloaded()) {
            AnalysisScheduler.submit(track);
        }

        return track;
    }

    private Track resolveTrackSource(String query, PlayOptions options) throws Exception {
        query = query.trim();
        String uuid = null;

        // 1. UUID directo
        if (query.matches("^[a-zA-Z0-9_-]{11}$")) {
            Track cached = dao.getTrack(query);
            if (cached != null && cached.isDownloaded()) {
                System.out.println("[Manager] Track encontrado (UUID): " + cached);
                return cached;
            }
            uuid = query;
        }

        // 1.1. Link YouTube (Extraemos UUID)
        if (uuid == null && isYouTubeLink(query)) {
            uuid = extractUUID(query);
            if (uuid != null) {
                Track cached = dao.getTrack(uuid);
                if (cached != null && cached.isDownloaded()) {
                    System.out.println("[Manager] Track encontrado (Link): " + cached);
                    return cached;
                }
            }
        }

        // 2. Metadata (Buscamos los metadatos si no se encontraba en la base de datos o es una busqueda por texto)
        Track track = MetadataClient.getMetadata(
                uuid != null ? uuid : query,
                options
        );

        uuid = track.getUuid();

        // 3. Buscamos la canción en la base de datos tras resolver el nombre -> UUID
        Track cached = dao.getTrack(uuid);
        if (cached != null && cached.isDownloaded()) {
            System.out.println("[Manager] Track encontrado: " + cached);
            return cached;
        }

        // 4. Descargamos la canción todas fallan (no se encontraba en la base de datos)
        File file = MetadataClient.downloadCompressed(track);
        track.setPath(file.getAbsolutePath());
        dao.saveTrack(track);

        return track;
    }

    private boolean isYouTubeLink(String query) {
        return query.contains("youtube.com") || query.contains("youtu.be");
    }

    private String extractUUID(String url) {
        if (url.contains("v=")) return url.split("v=")[1].split("&")[0];
        if (url.contains("youtu.be/")) return url.split("youtu.be/")[1].split("\\?")[0];
        return null;
    }
}