package TS3Bot.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:bot_database.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Habilitar Llaves Foráneas (Importante para el ON DELETE CASCADE)
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Crear tabla Canciones
            stmt.execute("CREATE TABLE IF NOT EXISTS canciones (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "titulo TEXT NOT NULL, " +
                    "artista TEXT DEFAULT 'Desconocido', " +
                    "album TEXT DEFAULT 'Single', " +
                    "ruta_archivo TEXT NOT NULL, " +
                    "duracion_segundos INTEGER, " +
                    "fecha_agregada DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // Crear tabla Playlists
            stmt.execute("CREATE TABLE IF NOT EXISTS playlists (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT UNIQUE NOT NULL, " +
                    "creador_uid TEXT, " +
                    "fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // Crear tabla Relacional
            stmt.execute("CREATE TABLE IF NOT EXISTS playlist_contenido (" +
                    "playlist_id INTEGER, " +
                    "cancion_uuid TEXT, " +
                    "agregado_por_uid TEXT, " +
                    "posicion INTEGER, " +
                    "PRIMARY KEY (playlist_id, cancion_uuid), " +
                    "FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (cancion_uuid) REFERENCES canciones(uuid) ON DELETE CASCADE);");

            System.out.println("[DB] Base de datos SQLite inicializada correctamente.");

        } catch (SQLException e) {
            System.err.println("[DB] Error inicializando base de datos: " + e.getMessage());
        }
    }
}