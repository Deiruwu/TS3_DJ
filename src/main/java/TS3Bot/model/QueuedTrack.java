package TS3Bot.model;

public class QueuedTrack {
    private final Track track;
    private final String requestedByUid;
    private final String requestedByName;
    private final boolean isUserRequest;

    public QueuedTrack(Track track, String requestedByUid, String requestedByName, boolean isUserRequest) {
        this.track = track;
        this.requestedByUid = requestedByUid;
        this.requestedByName = requestedByName;
        this.isUserRequest = isUserRequest;
    }

    public Track getTrack() { return track; }
    public String getRequestedByUid() { return requestedByUid; }
    public String getRequestedByName() { return requestedByName; }
    public boolean isUserRequest() { return isUserRequest; }
    public String getTrackUuid() { return track.getUuid(); }
    public String getTrackTitle() { return track.getTitle(); }

    public String getRequestString() {
        if (isUserRequest && requestedByName != null) {
            return " request by " + requestedByName;
        }
        return "";
    }

    public String getRequestStringFormatted() {
        if (isUserRequest && requestedByName != null) {
            return " [color=gray]request by[/color] " + requestedByName;
        }
        return "";
    }

    public String getCourtesyString() {
        if (!isUserRequest) {
            return " courtesy of " + requestedByName;
        }
        return "";
    }

    public String getCourtesyStringFormatted() {
        if (!isUserRequest && requestedByName != null) {
            return " [color=gray]courtesy of[/color] " + requestedByName;
        }
        return "";
    }

    public String getRequestInfo() {
        if (isUserRequest) {
            return getRequestString();
        } else {
            return getCourtesyString();
        }
    }

    public String getRequestInfoFormatted() {
        if (isUserRequest) {
            return getRequestStringFormatted();
        } else {
            return getCourtesyStringFormatted();
        }
    }

    @Override
    public String toString() {
        return track.toString() + getRequestInfoFormatted();
    }
}