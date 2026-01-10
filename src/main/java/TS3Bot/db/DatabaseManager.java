package TS3Bot.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:bot_database.db";

    public static void init() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Tabla ajustada a tu modelo Track (sin thumbnail)
            String sqlCanciones = "CREATE TABLE IF NOT EXISTS songs (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "title TEXT, " +
                    "artist TEXT, " +
                    "album TEXT, " +
                    "path TEXT, " +
                    "duration INTEGER, " +
                    "added_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(sqlCanciones);

            // Tablas de Playlists (Se mantienen igual, son relacionales)
            String sqlPlaylists = "CREATE TABLE IF NOT EXISTS playlists (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE, " +
                    "owner_uid TEXT" +
                    ")";
            stmt.execute(sqlPlaylists);

            String sqlPlaylistSongs = "CREATE TABLE IF NOT EXISTS playlist_songs (" +
                    "playlist_id INTEGER, " +
                    "song_uuid TEXT, " +
                    "FOREIGN KEY(playlist_id) REFERENCES playlists(id), " +
                    "FOREIGN KEY(song_uuid) REFERENCES songs(uuid)" +
                    ")";
            stmt.execute(sqlPlaylistSongs);

            System.out.println("[DB] Tablas inicializadas.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}