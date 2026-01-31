package TS3Bot.model;

import TS3Bot.model.enums.PlaylistType;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private int id;
    private String name;
    private String ownerUid;
    private String ownerName;
    private PlaylistType type;
    private int cachedSize = 0;

    private List<Track> tracks = new ArrayList<>();

    public Playlist(int id, String name, String ownerUid, String ownerName, PlaylistType type, int count) {
        this.id = id;
        this.name = name;
        this.ownerUid = ownerUid;
        this.ownerName = ownerName;
        this.type = type;
        this.cachedSize = count;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getOwnerUid() { return ownerUid; }
    public String getOwnerName() { return ownerName; }
    public PlaylistType getType() { return type; }

    public List<Track> getTracks() { return tracks; }
    public void setTracks(List<Track> tracks) { this.tracks = tracks; }
    public int getSize() {
        if (!tracks.isEmpty()) {
            return tracks.size();
        }
        return cachedSize;
    }

    @Override
    public String toString() {
        return String.format("%s ([color=purple]%d[/color] canciones)", name, getSize());
    }
}