-- =======================================================
-- PASO 1: CREAR TABLA DE USUARIOS (Igual que antes)
-- =======================================================
CREATE TABLE users (
                       uid TEXT PRIMARY KEY,
                       last_known_name TEXT NOT NULL,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Migrar usuarios existentes (Dueños de playlists)
INSERT OR IGNORE INTO users (uid, last_known_name)
SELECT DISTINCT owner_uid, owner_name FROM playlists;

-- =======================================================
-- PASO 2: CREAR TABLA TRACKS (Antes 'songs')
-- =======================================================
CREATE TABLE tracks (
                        uuid TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT,
                        album TEXT,               -- Ahora permitimos NULL si no es un album real
                        thumbnail TEXT,           -- NUEVO: URL de la imagen
                        duration INTEGER,
                        path TEXT,                -- Se mantiene la ruta del archivo
                        bpm INTEGER,              -- NUEVO
                        camelot_key TEXT,         -- NUEVO
                        added_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- MIGRACIÓN DE DATOS DE SONGS A TRACKS
-- Aquí aplicamos tu lógica: Si el album es "YouTube Video", lo guardamos como NULL
INSERT INTO tracks (uuid, title, artist, album, duration, path, added_at)
SELECT
    uuid,
    title,
    artist,
    CASE WHEN album = 'YouTube Video' THEN NULL ELSE album END, -- Limpieza de datos
    duration,
    path,
    added_at
FROM songs;

-- =======================================================
-- PASO 3: RECREAR TABLA PLAYLISTS (Sin 'is_public', con FK a users)
-- =======================================================
CREATE TABLE playlists_new (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               name TEXT NOT NULL,
                               owner_uid TEXT NOT NULL,
                               type TEXT NOT NULL DEFAULT 'USER' CHECK( type IN ('USER', 'SYSTEM', 'FAVORITES') ),
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY(owner_uid) REFERENCES users(uid) ON DELETE CASCADE,
                               UNIQUE(name, owner_uid)
);

-- Copiar datos viejos
INSERT INTO playlists_new (id, name, owner_uid, type)
SELECT id, name, owner_uid, type FROM playlists;

-- =======================================================
-- PASO 4: CREAR TABLA DE COLABORADORES
-- =======================================================
CREATE TABLE playlist_collaborators (
                                        playlist_id INTEGER,
                                        user_uid TEXT,
                                        added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY(playlist_id, user_uid),
                                        FOREIGN KEY(playlist_id) REFERENCES playlists_new(id) ON DELETE CASCADE,
                                        FOREIGN KEY(user_uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- =======================================================
-- PASO 5: CREAR TABLA PLAYLIST_TRACKS (Antes 'playlist_songs')
-- =======================================================
CREATE TABLE playlist_tracks (
                                 playlist_id INTEGER,
                                 track_uuid TEXT,           -- Renombrado para coincidir con la tabla 'tracks'
                                 added_by_uid TEXT,         -- Para saber si fue el dueño o un colaborador
                                 added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY(playlist_id) REFERENCES playlists_new(id) ON DELETE CASCADE,
                                 FOREIGN KEY(track_uuid) REFERENCES tracks(uuid) ON DELETE CASCADE,
                                 UNIQUE(playlist_id, track_uuid)
);

-- Migrar datos de la tabla vieja playlist_songs
-- Asumimos que el 'added_by' inicial es el dueño de la playlist
INSERT INTO playlist_tracks (playlist_id, track_uuid, added_at, added_by_uid)
SELECT
    ps.playlist_id,
    ps.song_uuid,
    ps.added_at,
    p.owner_uid
FROM playlist_songs ps
         JOIN playlists p ON ps.playlist_id = p.id;

-- =======================================================
-- PASO 6: ACTUALIZAR STATS (Para que apunten a 'tracks')
-- =======================================================
CREATE TABLE user_play_stats_new (
                                     user_uid TEXT,
                                     track_uuid TEXT,           -- Renombrado
                                     play_count INTEGER DEFAULT 1,
                                     last_played_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     FOREIGN KEY(user_uid) REFERENCES users(uid) ON DELETE CASCADE,
                                     FOREIGN KEY(track_uuid) REFERENCES tracks(uuid) ON DELETE CASCADE,
                                     PRIMARY KEY(user_uid, track_uuid)
);

INSERT INTO user_play_stats_new (user_uid, track_uuid, play_count, last_played_at)
SELECT user_uid, song_uuid, play_count, last_played_at FROM user_play_stats;

-- =======================================================
-- PASO 7: LIMPIEZA FINAL (Borrar tablas viejas)
-- =======================================================
DROP TABLE user_play_stats;
DROP TABLE playlist_songs;
DROP TABLE songs;
DROP TABLE playlists;

-- Renombrar las nuevas para que sean las oficiales
ALTER TABLE playlists_new RENAME TO playlists;
ALTER TABLE user_play_stats_new RENAME TO user_play_stats;