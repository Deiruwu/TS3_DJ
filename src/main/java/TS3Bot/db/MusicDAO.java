package TS3Bot.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MusicDAO {

    // --- GESTIÓN DE CANCIONES ---

    public void insertarCancion(String uuid, String titulo, String artista, String album, String ruta) {
        String sql = "INSERT OR IGNORE INTO canciones(uuid, titulo, artista, album, ruta_archivo) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, titulo);
            pstmt.setString(3, artista);
            pstmt.setString(4, album);
            pstmt.setString(5, ruta);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public String getRutaPorUuid(String uuid) {
        String sql = "SELECT ruta_archivo FROM canciones WHERE uuid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("ruta_archivo");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // --- GESTIÓN DE PLAYLISTS ---

    public int crearPlaylist(String nombre, String creadorUid) {
        String sql = "INSERT INTO playlists(nombre, creador_uid) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, creadorUid);
            pstmt.executeUpdate();

            // En SQLite, esta es la forma más fiable de obtener el último ID insertado
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al crear playlist: " + e.getMessage());
        }
        return -1;
    }

    public void añadirCancionAPlaylist(int playlistId, String cancionUuid, String usuarioUid) {
        String sql = "INSERT OR REPLACE INTO playlist_contenido(playlist_id, cancion_uuid, agregado_por_uid) VALUES(?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            pstmt.setString(2, cancionUuid);
            pstmt.setString(3, usuarioUid);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- BÚSQUEDAS ---

    /**
     * Obtiene todos los UUIDs de una playlist por su nombre o ID.
     */
    public List<String> getUuidsDePlaylist(String nombrePlaylist) {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT cancion_uuid FROM playlist_contenido " +
                "JOIN playlists ON playlists.id = playlist_contenido.playlist_id " +
                "WHERE playlists.nombre = ? OR playlists.id = ? " +
                "ORDER BY playlist_contenido.posicion ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombrePlaylist);
            pstmt.setString(2, nombrePlaylist); // Intentamos por ID también
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                lista.add(rs.getString("cancion_uuid"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Busca playlists que coincidan con un texto (para sugerencias)
     */
    public List<String> buscarPlaylists(String query) {
        List<String> resultados = new ArrayList<>();
        String sql = "SELECT nombre FROM playlists WHERE nombre LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) resultados.add(rs.getString("nombre"));
        } catch (SQLException e) { e.printStackTrace(); }
        return resultados;
    }

    /**
     * Obtiene el ID de una playlist por su nombre
     */
    public int getPlaylistId(String nombre) {
        String sql = "SELECT id FROM playlists WHERE nombre = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<String> obtenerTodasLasPlaylists() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, creador_uid FROM playlists";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add("ID: " + rs.getInt("id") + " | [b]" + rs.getString("nombre") + "[/b]");
            }
        } catch (SQLException e) {
            System.err.println("Error al listar playlists: " + e.getMessage());
        }
        return lista;
    }
}