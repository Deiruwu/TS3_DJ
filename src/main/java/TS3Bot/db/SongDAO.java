package TS3Bot.db;

import TS3Bot.model.Track;
import java.sql.*;

public class SongDAO {

    public void saveTrack(Track track) {
        String sql = "INSERT OR REPLACE INTO songs(uuid, title, artist, album, duration, path) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, track.getUuid());
            pstmt.setString(2, track.getTitle());
            pstmt.setString(3, track.getArtist());
            pstmt.setString(4, track.getAlbum());
            pstmt.setLong(5, track.getDuration());
            pstmt.setString(6, track.getPath());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error en SongDAO (save): " + e.getMessage());
        }
    }

    public Track getTrack(String uuid) {
        String sql = "SELECT * FROM songs WHERE uuid = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTrack(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error en SongDAO (get): " + e.getMessage());
        }
        return null;
    }

    public static Track mapResultSetToTrack(ResultSet rs) throws SQLException {
        return new Track(
                rs.getString("uuid"),
                rs.getString("title"),
                rs.getString("artist"),
                rs.getString("path"),
                rs.getLong("duration")
        );
    }
}