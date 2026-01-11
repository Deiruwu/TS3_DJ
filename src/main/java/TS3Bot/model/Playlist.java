package TS3Bot.model;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private int id;
    private String name;
    private String ownerUid;
    private PlaylistType type;

    private List<Track> tracks = new ArrayList<>();

    public Playlist(int id, String name, String ownerUid, PlaylistType type) {
        this.id = id;
        this.name = name;
        this.ownerUid = ownerUid;
        this.type = type;
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getOwnerUid() { return ownerUid; }
    public PlaylistType getType() { return type; }

    public List<Track> getTracks() { return tracks; }
    public void setTracks(List<Track> tracks) { this.tracks = tracks; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", type, name, ownerUid);
    }
}