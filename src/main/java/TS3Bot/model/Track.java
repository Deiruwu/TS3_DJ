package TS3Bot.model;

import java.io.File;

public class Track {
    private String uuid;
    private String title;
    private String artist;
    private String album;
    private String path;      // Puede ser null al principio
    private long duration;

    // Campo volátil para saber quién la pidió
    private String requester = "Sistema";

    public Track(String uuid, String title, String artist, String album, String path, long duration) {
        this.uuid = uuid;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.duration = duration;
    }

    public Track(String uuid, String title, String artist, String path, long duration) {
        this.uuid = uuid;
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
    }

    public void setPath(String path) {
        this.path = path;
    }

    // Getters y Setter de Requester (igual que antes)
    public void setRequester(String requester) { this.requester = requester; }
    public String getRequester() { return requester; }

    public String getUuid() { return uuid; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }

    // Check de seguridad para no crashear si path es null
    public File getFile() {
        return (path != null) ? new File(path) : null;
    }

    @Override
    public String toString() { return title + " by " + artist; }
}