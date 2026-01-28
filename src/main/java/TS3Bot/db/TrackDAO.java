package TS3Bot.db;

import TS3Bot.model.Track;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrackDAO {

    public void saveTrack(Track track) {
        String sql = "INSERT INTO tracks(uuid, title, artist, album, thumbnail, duration, path, bpm, camelot_key) " +
                "VALUES(?,?,?,?,?,?,?,?,?) " +
                "ON CONFLICT(uuid) DO UPDATE SET " +
                "title=excluded.title, artist=excluded.artist, album=excluded.album, " +
                "thumbnail=excluded.thumbnail, duration=excluded.duration, path=excluded.path, " +
                "bpm=excluded.bpm, camelot_key=excluded.camelot_key";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, track.getUuid());
            pstmt.setString(2, track.getTitle());
            pstmt.setString(3, track.getArtist());
            pstmt.setString(4, track.getAlbum());
            pstmt.setString(5, track.getThumbnail());
            pstmt.setLong(6, track.getDuration());
            pstmt.setString(7, track.getPath());
            pstmt.setInt(8, track.getBpm());
            pstmt.setString(9, track.getCamelotKey());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[TrackDAO] Error guardando track: " + e.getMessage());
        }
    }

    public Track getTrack(String uuid) {
        String sql = "SELECT * FROM tracks WHERE uuid = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTrack(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[TrackDAO] Error obteniendo track: " + e.getMessage());
        }

        return null;
    }

    public boolean deleteTrack(String uuid) {
        String sql = "DELETE FROM tracks WHERE uuid = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[TrackDAO] Error borrando track: " + e.getMessage());
            return false;
        }
    }

    public List<Track> getAllTracks() {
        String sql = "SELECT * FROM tracks";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            ResultSet rs = pstmt.executeQuery();
            List<Track> tracks = new ArrayList<>();

            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
            return tracks;
        } catch (SQLException e) {
            System.err.println("[TrackDAO] Error obteniendo todos los tracks: " + e.getMessage());
            return null;
        }
    }

    public static Track mapResultSetToTrack(ResultSet rs) throws SQLException {
        Track track = new Track(
                rs.getString("uuid"),
                rs.getString("title"),
                rs.getString("artist")
        );

        track.setAlbum(rs.getString("album"))
                .setThumbnail(rs.getString("thumbnail"))
                .setPath(rs.getString("path"))
                .setDuration(rs.getLong("duration"))
                .setBpm(rs.getInt("bpm"))
                .setCamelotKey(rs.getString("camelot_key"));

        return track;
    }
}