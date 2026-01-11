package TS3Bot.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:bot_database.db";
    private static final String SCHEMA_PATH = "/db/database.sql";

    public static void init() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            String sql = loadSqlFile();

            if (sql == null || sql.isEmpty()) {
                System.err.println("[DB Error] El archivo database.sql está vacío o no se encontró.");
                return;
            }

            stmt.executeUpdate(sql);

            System.out.println("[DB] base de datos iniciada");

        } catch (SQLException e) {
            System.err.println("[DB Error] Fallo SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[DB Error] Fallo al leer el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String loadSqlFile() {
        try (InputStream is = DatabaseManager.class.getResourceAsStream(SCHEMA_PATH)) {
            if (is == null) return null;
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}