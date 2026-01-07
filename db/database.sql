-- 1. TABLA MAESTRA DE CANCIONES
-- Usamos el UUID de 11 caracteres como llave primaria.
CREATE TABLE canciones (
                           uuid TEXT PRIMARY KEY,          -- dQw4w9WgXcQ
                           titulo TEXT NOT NULL,
                           artista TEXT DEFAULT 'Desconocido',
                           album TEXT DEFAULT 'Single',
                           ruta_archivo TEXT NOT NULL,     -- /music/dQw4w9WgXcQ.wav
                           duracion_segundos INTEGER,
                           fecha_agregada DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. TABLA DE PLAYLISTS
-- Aquí sí dejamos un INTEGER id porque los nombres de las playlists
-- pueden ser largos o contener caracteres que no queremos en las llaves foráneas.
CREATE TABLE playlists (
                           id INTEGER PRIMARY KEY AUTOINCREMENT,
                           nombre TEXT UNIQUE NOT NULL,
                           creador_uid TEXT,               -- El UID del usuario de TS3
                           fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. TABLA DE RELACIÓN (COLA / CONTENIDO)
CREATE TABLE playlist_contenido (
                                    playlist_id INTEGER,
                                    cancion_uuid TEXT,              -- Referencia directa al ID de YouTube
                                    agregado_por_uid TEXT,
                                    posicion INTEGER,               -- Para el orden de la playlist

                                    PRIMARY KEY (playlist_id, cancion_uuid),
                                    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
                                    FOREIGN KEY (cancion_uuid) REFERENCES canciones(uuid) ON DELETE CASCADE
);