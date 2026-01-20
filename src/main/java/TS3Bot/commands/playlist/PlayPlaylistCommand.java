package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

import java.io.File;
import java.util.List;

/**
 * Añade a la cola todas las canciones de una playlist.
 *
 * Comando para cargar una playlist completa a la cola de reproducción.
 * Verifica la propiedad de la playlist y asegura la integridad de los archivos de audio.
 *
 * @version 1.0
 */
public class PlayPlaylistCommand extends AsyncCommand {
    private final PlaylistUtils playlistUtils;

    public PlayPlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
    }

    @Override
    public String getName() {
        return "!playplaylist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!pp", "!loadpl", "!playp"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <id_playlist>");
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Carga todas las canciones de una playlist en la cola";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        try {
            // Resolver playlist usando utils
            int playlistIndex = Integer.parseInt(ctx.getArgsArray()[0]);
            Playlist playlist = playlistUtils.getPlaylistByUserIndex(playlistIndex);

            if (playlist == null) {
                reply("[color=red]Playlist no encontrada. Verifica el ID con !playlists.[/color]");
                return;
            }

            // Obtener tracks usando utils
            List<Track> tracks = playlistUtils.getTracksFromPlaylist(playlist);

            if (tracks.isEmpty()) {
                reply("[color=red]La playlist [b]" + playlist.getName() + "[/b] está vacía.[/color]");
                return;
            }

            reply("[color=blue]Cargando " + tracks.size() + " canciones de [b]" + playlist.getName() + "[/b]...[/color]");

            // Determinar si es del usuario (para attribution)
            boolean isMine = playlistUtils.isOwner(playlist, ctx.getUserUid());
            int loaded = 0;

            for (Track t : tracks) {
                try {
                    Track trackToQueue = ensureTrackFileExists(t);

                    if (trackToQueue != null) {
                        QueuedTrack queuedTrack = createQueuedTrack(
                                trackToQueue,
                                isMine,
                                ctx.getUserUid(),
                                ctx.getUserName()
                        );

                        bot.getPlayer().queue(queuedTrack);
                        loaded++;
                    }
                } catch (Exception e) {
                    System.err.println("Error cargando track: " + t.getTitle());
                }
            }

            reply("[color=lime]Playlist cargada exitosamente (" + loaded + " canciones).[/color]");

        } catch (NumberFormatException e) {
            reply("[color=red]El ID de la playlist debe ser numérico.[/color]");
        }
    }

    /**
     * Asegura que el archivo de audio exista físicamente.
     * Si no existe, intenta re-descargarlo.
     */
    private Track ensureTrackFileExists(Track track) {
        File file = (track.getPath() != null) ? new File(track.getPath()) : null;

        if (file != null && file.exists()) {
            return track;
        }

        System.out.println("[Auto-Fix] Recuperando archivo perdido: " + track.getTitle());
        try {
            return bot.getMusicManager().resolve(track.getUuid());
        } catch (Exception e) {
            System.err.println("Falló recuperación de: " + track.getUuid());
            return null;
        }
    }

    /**
     * Crea un QueuedTrack con atribución correcta.
     * Si la playlist es del usuario, se marca como "request by {user}".
     * Si no, se encola sin atribución (sistema).
     */
    private QueuedTrack createQueuedTrack(Track track, boolean isMine, String userUid, String userName) {
        if (isMine) {
            return new QueuedTrack(track, userUid, userName, true);
        } else {
            return new QueuedTrack(track, null, null, true);
        }
    }
}