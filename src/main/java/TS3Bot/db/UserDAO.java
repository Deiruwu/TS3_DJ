package TS3Bot.db;

import java.sql.*;

public class UserDAO {

    public void saveUser(String uid, String name) {
        String sql = "INSERT INTO users(uid, last_known_name) VALUES(?,?) " +
                "ON CONFLICT(uid) DO UPDATE SET last_known_name = excluded.last_known_name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uid);
            pstmt.setString(2, name);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error guardando usuario: " + e.getMessage());
        }
    }

    public String getUserName(String uid) {
        String sql = "SELECT last_known_name FROM users WHERE uid = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uid);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("last_known_name");
                }
            }

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error obteniendo nombre: " + e.getMessage());
        }

        return null;
    }
}