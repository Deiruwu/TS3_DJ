package TS3Bot.model;

import java.io.File;

public class Track {
    private String uuid;
    private String title;
    private String artist;
    private String album;
    private String path;
    private String albumUrl;
    private long duration;

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

    public Track(String uuid, String title, String artist, String album, String path, String albumUrl, long duration) {
        this.uuid = uuid;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.albumUrl = albumUrl;
        this.duration = duration;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUuid() { return uuid; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getPath() { return path; }
    public String getAlbumUrl() { return albumUrl; }
    public long getDuration() { return duration; }

    public void setAlbumUrl(String albumUrl) { this.albumUrl = albumUrl; }

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

    public File getFile() {
        return (path != null) ? new File(path) : null;
    }

    @Override
    public String toString() { return title + " [color=#BD93F9]by[/color] " + artist; }

    public String toStringNotFormmat() { return title + " by " + artist; }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Track && uuid.equals(((Track) obj).uuid);
    }
}