package TS3Bot.db;

import TS3Bot.model.PlayStats;
import TS3Bot.model.Track;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatsDAO {

    /**
     * Registra que una canción ha empezado a sonar.
     * Si es la primera vez, crea el registro. Si no, suma +1 al contador y actualiza la fecha.
     */
    public void registrarReproduccion(String userUid, String songUuid) {
        String sql = "INSERT INTO user_play_stats (user_uid, song_uuid, play_count, last_played_at) " +
                "VALUES (?, ?, 1, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(user_uid, song_uuid) " +
                "DO UPDATE SET " +
                "play_count = play_count + 1, " +
                "last_played_at = CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userUid);
            pstmt.setString(2, songUuid);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[StatsDAO] Error actualizando estadísticas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene el Top de canciones más escuchadas por un usuario específico.
     * Devuelve una lista de objetos PlayStats COMPLETOS (con el Track dentro).
     */
    public List<PlayStats> getTopSongsByUser(String userUid, int limit) {
        List<PlayStats> stats = new ArrayList<>();

        String sql = "SELECT s.*, ups.play_count, ups.last_played_at, ups.user_uid " +
                "FROM user_play_stats ups " +
                "JOIN songs s ON ups.song_uuid = s.uuid " +
                "WHERE ups.user_uid = ? " +
                "ORDER BY ups.play_count DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userUid);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Track track = new Track(
                            rs.getString("uuid"),
                            rs.getString("title"),
                            rs.getString("artist"),
                            rs.getString("album"),
                            rs.getString("path"),
                            rs.getLong("duration")
                    );

                    PlayStats stat = new PlayStats(
                            rs.getString("user_uid"),
                            track,
                            rs.getInt("play_count"),
                            rs.getString("last_played_at")
                    );

                    stats.add(stat);
                }
            }
        } catch (SQLException e) {
            System.err.println("[StatsDAO] Error obteniendo top user: " + e.getMessage());
        }
        return stats;
    }


    public List<PlayStats> getGlobalTopSongs(int limit) {
        List<PlayStats> stats = new ArrayList<>();

        String sql = "SELECT s.*, SUM(ups.play_count) as total_plays " +
                "FROM user_play_stats ups " +
                "JOIN songs s ON ups.song_uuid = s.uuid " +
                "GROUP BY ups.song_uuid " +
                "ORDER BY total_plays DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Track track = new Track(
                            rs.getString("uuid"),
                            rs.getString("title"),
                            rs.getString("artist"),
                            rs.getString("album"),
                            rs.getString("path"),
                            rs.getLong("duration")
                    );

                    // Usamos "GLOBAL" como userUid para indicar que es un agregado
                    PlayStats stat = new PlayStats(
                            "SERVER_GLOBAL",
                            track,
                            rs.getInt("total_plays"),
                            null
                    );

                    stats.add(stat);
                }
            }
        } catch (SQLException e) {
            System.err.println("[StatsDAO] Error obteniendo top global: " + e.getMessage());
        }
        return stats;
    }
}