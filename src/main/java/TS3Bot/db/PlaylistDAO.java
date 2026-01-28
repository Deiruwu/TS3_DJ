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

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error creando playlist: " + e.getMessage());
        }

        return -1;
    }

    public boolean addTrackToPlaylist(int playlistId, String trackUuid, String addedByUid) {
        String sql = "INSERT OR IGNORE INTO playlist_tracks(playlist_id, track_uuid, added_by_uid) VALUES(?,?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);
            pstmt.setString(2, trackUuid);
            pstmt.setString(3, addedByUid);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error agregando track: " + e.getMessage());
            return false;
        }
    }

    public boolean removeTrackToPlaylist(int playlistId, String trackUuid) {
        String sql = "DELETE FROM playlist_tracks WHERE playlist_id = ? AND track_uuid = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);
            pstmt.setString(2, trackUuid);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error removiendo track: " + e.getMessage());
            return false;
        }
    }

    public List<Track> getTracksFromPlaylist(Playlist playlist) {
        return getTracksFromPlaylist(playlist.getId(), PlaylistType.USER);
    }

        public List<Track> getTracksFromPlaylist(int playlistId, PlaylistType type) {
        List<Track> tracks = new ArrayList<>();
        String sql;

        if (type == PlaylistType.SYSTEM) {
            sql = "SELECT * FROM tracks ORDER BY added_at DESC";
        } else {
            sql = "SELECT t.* FROM tracks t " +
                    "JOIN playlist_tracks pt ON t.uuid = pt.track_uuid " +
                    "WHERE pt.playlist_id = ? " +
                    "ORDER BY pt.added_at ASC";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (type != PlaylistType.SYSTEM) {
                pstmt.setInt(1, playlistId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tracks.add(TrackDAO.mapResultSetToTrack(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error obteniendo tracks: " + e.getMessage());
        }

        return tracks;
    }

    public List<String> getTrackUuids(int playlistId, PlaylistType type) {
        List<String> uuids = new ArrayList<>();
        String sql;

        if (type == PlaylistType.SYSTEM) {
            sql = "SELECT uuid FROM tracks ORDER BY added_at DESC";
        } else {
            sql = "SELECT track_uuid FROM playlist_tracks " +
                    "WHERE playlist_id = ? " +
                    "ORDER BY added_at ASC";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (type != PlaylistType.SYSTEM) {
                pstmt.setInt(1, playlistId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    uuids.add(rs.getString(1));
                }
            }

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error obteniendo UUIDs: " + e.getMessage());
        }

        return uuids;
    }

    public Playlist getPlaylist(int playlistId) {
        String sql = "SELECT p.*, u.last_known_name as owner_name, COUNT(pt.track_uuid) as track_count " +
                "FROM playlists p " +
                "JOIN users u ON p.owner_uid = u.uid " +
                "LEFT JOIN playlist_tracks pt ON p.id = pt.playlist_id " +
                "WHERE p.id = ? " +
                "GROUP BY p.id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Playlist(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("owner_uid"),
                            rs.getString("owner_name"),
                            PlaylistType.valueOf(rs.getString("type")),
                            rs.getInt("track_count")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error obteniendo playlist: " + e.getMessage());
        }

        return null;
    }

    public List<Playlist> getAllPlaylists() {
        List<Playlist> playlists = new ArrayList<>();

        String sql = "SELECT p.*, u.last_known_name as owner_name, COUNT(pt.track_uuid) as track_count " +
                "FROM playlists p " +
                "JOIN users u ON p.owner_uid = u.uid " +
                "LEFT JOIN playlist_tracks pt ON p.id = pt.playlist_id " +
                "GROUP BY p.id " +
                "ORDER BY p.id ASC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                playlists.add(new Playlist(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("owner_uid"),
                        rs.getString("owner_name"),
                        PlaylistType.valueOf(rs.getString("type")),
                        rs.getInt("track_count")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error obteniendo playlists: " + e.getMessage());
        }

        return playlists;
    }

    public Playlist getFavoritesPlaylist(String ownerUid) {
        String sql = "SELECT p.*, u.last_known_name as owner_name, COUNT(pt.track_uuid) as track_count " +
                "FROM playlists p " +
                "JOIN users u ON p.owner_uid = u.uid " +
                "LEFT JOIN playlist_tracks pt ON p.id = pt.playlist_id " +
                "WHERE p.owner_uid = ? AND p.type = 'FAVORITES' " +
                "GROUP BY p.id " +
                "LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ownerUid);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Playlist(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("owner_uid"),
                            rs.getString("owner_name"),
                            PlaylistType.valueOf(rs.getString("type")),
                            rs.getInt("track_count")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error obteniendo favoritos: " + e.getMessage());
        }

        return null;
    }

    public boolean updateName(int playlistId, String newName) {
        String sql = "UPDATE playlists SET name = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newName);
            pstmt.setInt(2, playlistId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error actualizando nombre: " + e.getMessage());
            return false;
        }
    }

    public boolean deletePlaylist(int playlistId) {
        String sql = "DELETE FROM playlists WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error eliminando playlist: " + e.getMessage());
            return false;
        }
    }

    public boolean addCollaborator(int playlistId, String userUid) {
        String sql = "INSERT OR IGNORE INTO playlist_collaborators(playlist_id, user_uid) VALUES(?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);
            pstmt.setString(2, userUid);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error agregando colaborador: " + e.getMessage());
            return false;
        }
    }

    public boolean removeCollaborator(int playlistId, String userUid) {
        String sql = "DELETE FROM playlist_collaborators WHERE playlist_id = ? AND user_uid = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);
            pstmt.setString(2, userUid);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error removiendo colaborador: " + e.getMessage());
            return false;
        }
    }

    public List<String> getCollaborators(int playlistId) {
        List<String> collaborators = new ArrayList<>();
        String sql = "SELECT user_uid FROM playlist_collaborators WHERE playlist_id = ? ORDER BY added_at ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    collaborators.add(rs.getString("user_uid"));
                }
            }

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error obteniendo colaboradores: " + e.getMessage());
        }

        return collaborators;
    }

    public boolean isCollaborator(int playlistId, String userUid) {
        String sql = "SELECT 1 FROM playlist_collaborators WHERE playlist_id = ? AND user_uid = ? LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playlistId);
            pstmt.setString(2, userUid);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[PlaylistDAO] Error verificando colaborador: " + e.getMessage());
        }

        return false;
    }
}