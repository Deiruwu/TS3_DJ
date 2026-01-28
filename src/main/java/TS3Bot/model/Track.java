package TS3Bot.model;

import java.io.File;
import java.util.Objects;

public class Track {

    // --- 1. DATOS OBLIGATORIOS (INMUTABLES) ---
    // Definen la identidad de la canción. Una canción sin UUID o Título no debería existir.
    private final String uuid;
    private final String title;
    private final String artist;

    // --- 2. DATOS MUTABLES (LLEGAN POR FASES) ---
    // Inicialmente pueden ser null o 0, y se llenan conforme el bot procesa la info.
    private String album;
    private String thumbnail; // URL de la imagen (cover)
    private String path;        // Ruta del archivo físico (.opus/.mp3)
    private long duration;      // En segundos

    // Metadatos avanzados (IA / Análisis)
    private int bpm;
    private String camelotKey;  // Ej: "11B", "8A"

    // --- CONSTRUCTOR ÚNICO ---
    public Track(String uuid, String title, String artist) {
        if (uuid == null || uuid.isEmpty()) throw new IllegalArgumentException("El UUID es obligatorio");
        if (title == null || title.isEmpty()) throw new IllegalArgumentException("El Título es obligatorio");

        this.uuid = uuid;
        this.title = title;
        // Si no hay artista, ponemos "Unknown" para evitar nulls molestos en la UI
        this.artist = (artist != null && !artist.isEmpty()) ? artist : "Unknown Artist";
    }

    // --- SETTERS FLUENT (RETORNAN THIS) ---
    // Esto permite hacer: new Track(...).setPath(...).setBpm(...)

    public Track setAlbum(String album) {
        this.album = album;
        return this;
    }

    public Track setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }

    public Track setPath(String path) {
        this.path = path;
        return this;
    }

    public Track setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public Track setBpm(int bpm) {
        this.bpm = bpm;
        return this;
    }

    public Track setCamelotKey(String camelotKey) {
        this.camelotKey = camelotKey;
        return this;
    }

    // --- GETTERS ---

    public String getUuid() { return uuid; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }

    public String getAlbum() { return album; }
    public String getThumbnail() { return thumbnail; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }
    public int getBpm() { return bpm; }
    public String getCamelotKey() { return camelotKey; }

    // --- MÉTODOS DE UTILIDAD (Lógica de Dominio) ---

    public File getFile() {
        return (path != null) ? new File(path) : null;
    }

    public boolean isDownloaded() {
        return path != null && new File(path).exists();
    }

    public boolean isAnalyzed() {
        return bpm > 0 && camelotKey != null;
    }

    public String getFormattedDuration() {
        long totalSeconds = this.duration;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public String toString() {
        // Formato con BBCode para TeamSpeak 3 (Colores)
        return title + " [color=#BD93F9]by[/color] " + artist;
    }

    public String toStringNotFormmat() {
        return title + " by " + artist;
    }

    // --- EQUALS & HASHCODE (BASADOS EN IDENTIDAD/UUID) ---
    // Vital para que List.contains(track) funcione bien en las Playlists

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return Objects.equals(uuid, track.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}