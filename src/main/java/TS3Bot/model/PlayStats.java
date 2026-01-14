package TS3Bot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlayStats {
    private String userUid;
    private Track track;
    private int playCount;
    private LocalDateTime lastPlayedAt;

    private static final DateTimeFormatter DB_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PlayStats(String userUid, Track track, int playCount, String lastPlayedAtStr) {
        this.userUid = userUid;
        this.track = track;
        this.playCount = playCount;

        if (lastPlayedAtStr != null) {
            this.lastPlayedAt = LocalDateTime.parse(lastPlayedAtStr, DB_DATE_FORMAT);
        } else {
            this.lastPlayedAt = LocalDateTime.now();
        }
    }

    public String getUserUid() { return userUid; }
    public Track getTrack() { return track; }
    public String getTrackUuid() { return track.getUuid(); }
    public int getPlayCount() { return playCount; }
    public LocalDateTime getLastPlayedAt() { return lastPlayedAt; }

    @Override
    public String toString() {
        return track + " [b][color=#ff0080][" + playCount + "][/color][/b] Escuchado última vez:" + lastPlayedAt;
    }
}