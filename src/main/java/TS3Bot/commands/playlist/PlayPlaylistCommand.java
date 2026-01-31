package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.PlaylistServices;
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
    private final PlaylistServices playlistServices;

    public PlayPlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistServices = new PlaylistServices(bot);
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
            Playlist playlist = playlistServices.getPlaylistByUserIndex(playlistIndex);

            if (playlist == null) {
                replyError("Playlist #" + playlistIndex + " no encontrada.");
                return;
            }

            List<Track> tracks = playlistServices.getTracksFromPlaylist(playlist);

            if (tracks.isEmpty()) {
                replyWarning("La playlist [b]" + playlist.getName() + "[/b] está vacía.");
                return;
            }


            boolean isMine = playlistServices.isOwner(playlist, ctx.getUserUid());
            int loaded = 0;

            for (Track t : tracks) {
                try {
                    Track trackToQueue = ensureTrackFileExists(t);

                    if (trackToQueue != null) {
                        QueuedTrack queuedTrack = createQueuedTrack(
                                trackToQueue,
                                isMine,
                                ctx.getUserUid(),
                                (isMine) ? ctx.getUserName() : playlist.getOwnerName()
                        );

                        bot.getPlayer().queue(queuedTrack);
                        loaded++;
                    }
                } catch (Exception e) {
                    System.err.println("Error cargando track: " + t.getTitle());
                }
            }

            replySuccess(loaded + " canciones cargando de [b]" + playlist.getName() + "[/b]...");

        } catch (NumberFormatException e) {
            replyError("El ID de la playlist debe ser numérico.");
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
            return new QueuedTrack(track, null, userName, true);
        }
    }
}