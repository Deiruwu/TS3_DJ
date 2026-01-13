package TS3Bot.db;

import TS3Bot.model.Playlist;
import TS3Bot.model.PlaylistType;
import TS3Bot.model.Track;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDAO {

    public int createPlaylist(String name, String ownerUid, PlaylistType type) {
        String sql = "INSERT INTO playlists(name, owner_uid, type) VALUES(?,?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, ownerUid);
            pstmt.setString(3, type.toString());
            pstmt.executeUpdate();

            // Recuperar el último ID insertado
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error creando playlist: " + e.getMessage());
        }
        return -1;
    }

    public boolean addSongToPlaylist(int playlistId, String songUuid) {
        String sql = "INSERT OR IGNORE INTO playlist_songs(playlist_id, song_uuid) VALUES(?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);
            pstmt.setString(2, songUuid);
            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error agregando canción a playlist: " + e.getMessage());
            return false;
        }
    }

    public boolean removeSongFromPlaylist(int playlistId, String trackUuid) {
        String sql = "DELETE FROM playlist_tracks WHERE playlist_id = ? AND track_uuid = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, playlistId);
            ps.setString(2, trackUuid);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Track> getTracksFromPlaylist(Playlist playlist) {
        List<Track> tracks = new ArrayList<>();
        String sql;

        if (playlist.getType() == PlaylistType.SYSTEM)
            sql = "SELECT * FROM songs ORDER BY added_at DESC";

        else
            sql = "SELECT s.* FROM songs s " +
                    "JOIN playlist_songs ps ON s.uuid = ps.song_uuid " +
                    "WHERE ps.playlist_id = ? " +
                    "ORDER BY ps.added_at ASC";


        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (playlist.getType() != PlaylistType.SYSTEM) {
                pstmt.setInt(1, playlist.getId());
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tracks.add(TrackDAO.mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error recuperando tracks de playlist: " + e.getMessage());
        }
        return tracks;
    }

    public List<String> getTrackUuidsFromPlaylist(Playlist playlist) {
        List<String> uuids = new ArrayList<>();
        String sql;

        if (playlist.getType() == PlaylistType.SYSTEM)
            sql = "SELECT uuid FROM songs ORDER BY added_at DESC";
        else
            sql = "SELECT ps.song_uuid FROM playlist_songs ps " +
                    "WHERE ps.playlist_id = ? " +
                    "ORDER BY ps.added_at ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (playlist.getType() != PlaylistType.SYSTEM) {
                pstmt.setInt(1, playlist.getId());
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                uuids.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Error recuperando UUIDs de playlist: " + e.getMessage());
        }
        return uuids;
    }

    public Playlist getPlaylistById(int playlistId) {
        String sql = "SELECT * FROM playlists WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Playlist(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("owner_uid"),
                        PlaylistType.valueOf(rs.getString("type"))
                );
            }
        } catch (SQLException e) {
            System.err.println("Error buscando playlist por ID: " + e.getMessage());
        }
        return null;
    }

    public List<Playlist> getAllPlaylists() {
        List<Playlist> playlists = new ArrayList<>();

        String sql = "SELECT * FROM playlists ORDER BY id ASC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                playlists.add(new Playlist(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("owner_uid"),
                        PlaylistType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo todas las playlists: " + e.getMessage());
        }
        return playlists;
    }
}