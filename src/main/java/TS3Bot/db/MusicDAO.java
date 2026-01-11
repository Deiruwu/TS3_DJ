package TS3Bot.db;

import TS3Bot.model.Track;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MusicDAO {

/*
    public void saveTrack(Track track) {
        String sql = "INSERT OR REPLACE INTO songs(uuid, title, artist, album, path, duration) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, track.getUuid());
            pstmt.setString(2, track.getTitle());
            pstmt.setString(3, track.getArtist());
            pstmt.setString(4, track.getAlbum());
            pstmt.setString(5, track.getPath());
            pstmt.setLong(6, track.getDuration());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error guardando track: " + e.getMessage());
        }
    }

    public Track getTrack(String uuid) {
        String sql = "SELECT * FROM songs WHERE uuid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Track(
                        rs.getString("uuid"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("album"),
                        rs.getString("path"),
                        rs.getLong("duration")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error recuperando track: " + e.getMessage());
        }
        return null;
    }

    // --- MÉTODOS DE PLAYLISTS (Esenciales para que funcione !listp y !pp) ---

    public int crearPlaylist(String name, String owner) {
        String sql = "INSERT INTO playlists(name, owner_uid) VALUES(?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, owner);
            pstmt.executeUpdate();
            return 1;
        } catch (SQLException e) { return -1; }
    }

    public void añadirCancionAPlaylist(int playlistId, String songUuid, String userUid) {
        // Aquí podrías validar userUid si quisieras permisos
        String sql = "INSERT INTO playlist_songs(playlist_id, song_uuid) VALUES(?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            pstmt.setString(2, songUuid);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public List<String> getUuidsDePlaylist(String playlistName) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT song_uuid FROM playlist_songs ps JOIN playlists p ON ps.playlist_id = p.id WHERE p.name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playlistName);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) list.add(rs.getString("song_uuid"));
        } catch(SQLException ignored) {}
        return list;
    }

    public List<String> obtenerTodasLasPlaylists() {
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM playlists")) {
            while (rs.next()) list.add("ID: " + rs.getInt("id") + " | " + rs.getString("name"));
        } catch (SQLException ignored) {}
        return list;
    }
*/
}
