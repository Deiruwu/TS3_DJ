package TS3Bot.db;

import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    // Asegúrate de que este nombre coincida con tu archivo real
    private static final String URL = "jdbc:sqlite:bot_database.db";

    public static void init() {
        try {
            // Configuración de Flyway
            Flyway flyway = Flyway.configure()
                    .dataSource(URL, "", "") // SQLite no usa usuario/pass
                    .locations("classpath:db/migration") // Donde pusiste V1 y V2

                    // --- LA PARTE CRÍTICA PARA NO PERDER DATOS ---
                    // 1. Si encuentra una DB existente sin historial de Flyway...
                    .baselineOnMigrate(true)
                    // 2. ...asume que esa DB ya está en la versión 1.
                    // (Esto hace que se SALTE el archivo V1__Esquema_Legacy.sql
                    // y empiece a ejecutar directamente el V2__Migracion...)
                    .baselineVersion("1")

                    .load();

            // Ejecuta las migraciones pendientes (En tu caso, correrá la V2)
            flyway.migrate();

            System.out.println("[DB] Base de datos migrada y lista (Flyway).");

        } catch (Exception e) {
            System.err.println("[DB Error] Fallo crítico al migrar la base de datos:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Connection getConnection() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        return DriverManager.getConnection(URL, config.toProperties());
    }
}