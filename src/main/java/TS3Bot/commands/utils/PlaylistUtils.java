package TS3Bot.commands.utils;

import TS3Bot.TeamSpeakBot;
import TS3Bot.model.Playlist;
import TS3Bot.model.PlaylistType;
import TS3Bot.model.Track;

import java.util.List;

/**
 * Utilidades centralizadas para operaciones comunes con playlists.
 *
 * Esta clase maneja:
 * - Resolución de playlists por nombre o índice
 * - Búsqueda de playlists de usuario (favoritos)
 * - Validaciones de permisos
 * - Operaciones CRUD comunes
 */
public class PlaylistUtils {
    private final TeamSpeakBot bot;

    public PlaylistUtils(TeamSpeakBot bot) {
        this.bot = bot;
    }

    // ========================================
    // BÚSQUEDA Y RESOLUCIÓN
    // ========================================

    /**
     * Busca una playlist por índice numérico o por nombre.
     *
     * @param input Puede ser "#3" o "Mi Playlist"
     * @return La playlist encontrada o null
     */
    public Playlist resolvePlaylist(String input) {
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < bot.getAllPlaylists().size()) {
                    return bot.getAllPlaylists().get(index);
                }
            } catch (NumberFormatException e) {
                for (Playlist p : bot.getAllPlaylists()) {
                    if (p.getName().equalsIgnoreCase(input)) {
                        return p;
                    }
                }
            }
            return null;
    }

    /**
     * Busca la playlist de favoritos de un usuario específico.
     * Por convención, se llama "Música de {userName}".
     *
     * @param userUid UID del usuario
     * @param userName Nombre del usuario
     * @return La playlist de favoritos o null si no existe
     */
    public Playlist findUserFavoritesPlaylist(String userUid, String userName) {
        String expectedName = "Música de " + userName;

        for (Playlist p : bot.getAllPlaylists()) {
            if (p.getName().equals(expectedName) &&
                    p.getOwnerUid().equals(userUid) &&
                    p.getType() == PlaylistType.FAVORITES) {
                return p;
            }
        }
        return null;
    }

    /**
     * Obtiene o crea la playlist de favoritos de un usuario.
     * Si no existe, la crea automáticamente.
     *
     * @param userUid UID del usuario
     * @param userName Nombre del usuario
     * @return La playlist de favoritos (existente o recién creada)
     */
    public Playlist ensureUserFavoritesPlaylist(String userUid, String userName) {
        Playlist existing = findUserFavoritesPlaylist(userUid, userName);
        if (existing != null) return existing;

        // Crear nueva playlist de favoritos
        String playlistName = "Música de " + userName;
        int newId = bot.getPlaylistManager().createPlaylist(playlistName, userUid, userName,PlaylistType.FAVORITES);

        if (newId == -1) return null; // Error al crear

        // Refrescar y buscar la recién creada
        bot.refreshPlaylists();
        return findPlaylistById(newId);
    }

    /**
     * Busca una playlist por su ID numérico.
     */
    public Playlist findPlaylistById(int id) {
        for (Playlist p : bot.getAllPlaylists()) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    // ========================================
    // VALIDACIONES
    // ========================================

    /**
     * Verifica si un usuario es dueño de una playlist.
     *
     * @param playlist La playlist a verificar
     * @param userUid UID del usuario
     * @return true si el usuario es el dueño
     */
    public boolean isOwner(Playlist playlist, String userUid) {
        return playlist.getOwnerUid().equals(userUid);
    }

    /**
     * Valida que el índice de playlist sea válido.
     *
     * @param index Índice (base 0)
     * @return true si es válido
     */
    public boolean isValidPlaylistIndex(int index) {
        return index >= 0 && index < bot.getAllPlaylists().size();
    }

    /**
     * Valida y devuelve una playlist por índice (base 1, como lo ve el usuario).
     *
     * @param oneBasedIndex Índice empezando en 1
     * @return La playlist o null si es inválido
     */
    public Playlist getPlaylistByUserIndex(int oneBasedIndex) {
        int index = oneBasedIndex - 1;
        if (!isValidPlaylistIndex(index)) return null;
        return bot.getAllPlaylists().get(index);
    }

    public String validatePlaylistName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "El nombre no puede estar vacío";
        }

        String trimmed = name.trim();

        if (trimmed.length() < 2) {
            return "El nombre debe tener al menos 2 caracteres";
        }

        if (trimmed.length() > 50) {
            return "El nombre es demasiado largo (máx. 50 caracteres)";
        }

        // Caracteres prohibidos
        if (trimmed.matches(".*[<>:\"/\\\\|?*].*")) {
            return "El nombre contiene caracteres no permitidos";
        }

        return null; // Válido
    }

    /**
     * Verifica si ya existe una playlist con ese nombre para el usuario.
     */
    public boolean playlistNameExists(String name, String userUid) {
        for (Playlist p : bot.getAllPlaylists()) {
            if (p.getName().equalsIgnoreCase(name) && p.getOwnerUid().equals(userUid)) {
                return true;
            }
        }
        return false;
    }

    // ========================================
    // OPERACIONES CRUD
    // ========================================

    /**
     * Agrega una canción a una playlist.
     *
     * @param playlist Playlist destino
     * @param track Canción a agregar
     * @return true si se agregó exitosamente
     */
    public boolean addTrackToPlaylist(Playlist playlist, Track track) {
        bot.getPlaylistManager().addSongToPlaylist(playlist.getId(), track.getUuid());
        return true; // El DAO no devuelve boolean, asumimos éxito
    }

    /**
     * Remueve una canción de una playlist.
     *
     * @param playlist Playlist origen
     * @param track Canción a remover
     * @return true si se removió, false si no estaba
     */
    public boolean removeTrackFromPlaylist(Playlist playlist, Track track) {
        return bot.getPlaylistManager().removeSongFromPlaylist(playlist.getId(), track.getUuid());
    }

    /**
     * Obtiene todas las canciones de una playlist.
     */
    public List<Track> getTracksFromPlaylist(Playlist playlist) {
        return bot.getPlaylistManager().getTracksFromPlaylist(playlist);
    }

    /**
     * Verifica si una canción existe en una playlist.
     */
    public boolean playlistContainsTrack(Playlist playlist, Track track) {
        List<Track> tracks = getTracksFromPlaylist(playlist);
        return tracks.stream().anyMatch(t -> t.getUuid().equals(track.getUuid()));
    }

    // ========================================
    // HELPERS DE FORMATO
    // ========================================

    /**
     * Genera un mensaje formateado para mostrar una playlist.
     * Ejemplo: "#3 - Mi Playlist (15 canciones)"
     */
    public String formatPlaylistInfo(Playlist playlist, int displayIndex) {
        int trackCount = getTracksFromPlaylist(playlist).size();
        return String.format("#%d - %s (%d canciones)",
                displayIndex,
                playlist.getName(),
                trackCount);
    }

    /**
     * Genera el encabezado para mostrar el contenido de una playlist.
     */
    public String formatPlaylistHeader(Playlist playlist) {
        return "[color=blue][b]" + playlist.getName() + ":[/b][/color]";
    }

    /**
     * Formatea una entrada de track en una lista numerada.
     * Ejemplo: "  #1. Canción by Artista"
     */
    public String formatTrackEntry(int index, Track track) {
        return String.format("  [color=purple]#%d.[/color] %s [color=purple]by[/color] %s",
                index + 1,
                track.getTitle(),
                track.getArtist());
    }
}