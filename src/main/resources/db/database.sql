-- 1. CANCIONES
CREATE TABLE IF NOT EXISTS songs (
                                     uuid TEXT PRIMARY KEY,
                                     title TEXT,
                                     artist TEXT,
                                     duration INTEGER,
                                     path TEXT,
                                     added_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. PLAYLISTS
CREATE TABLE IF NOT EXISTS playlists (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         name TEXT NOT NULL,
                                         owner_uid TEXT NOT NULL,
                                         type TEXT NOT NULL DEFAULT 'USER' CHECK( type IN ('USER', 'SYSTEM', 'FAVORITES') ),
    UNIQUE(name, owner_uid)
    );

-- 3. CONTENIDO DE PLAYLISTS
CREATE TABLE IF NOT EXISTS playlist_songs (
                                              playlist_id INTEGER,
                                              song_uuid TEXT,
                                              added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                              FOREIGN KEY(playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY(song_uuid) REFERENCES songs(uuid) ON DELETE CASCADE,
    UNIQUE(playlist_id, song_uuid)
    );

-- 4. ESTADÍSTICAS
CREATE TABLE IF NOT EXISTS user_play_stats (
                                               user_uid TEXT,
                                               song_uuid TEXT,
                                               play_count INTEGER DEFAULT 1,
                                               last_played_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                               FOREIGN KEY(song_uuid) REFERENCES songs(uuid) ON DELETE CASCADE,
    PRIMARY KEY(user_uid, song_uuid)
    );