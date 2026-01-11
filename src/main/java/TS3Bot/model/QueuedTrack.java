package TS3Bot.model;

public class QueuedTrack {
    private final Track track;
    private final String requestedByUid;
    private final String requestedByName;

    public QueuedTrack(Track track, String requestedByUid, String requestedByName) {
        this.track = track;
        this.requestedByUid = requestedByUid;
        this.requestedByName = requestedByName;
    }

    public Track getTrack() { return track; }
    public String getRequestedByUid() { return requestedByUid; }
    public String getRequestedByName() { return requestedByName; }
    public String getTrackUuid() { return track.getUuid(); }
    public String getTrackTitle() { return track.getTitle(); }

    @Override
    public String toString() {
        return track + " (Solicitado por " + requestedByName + ")";
    }
}