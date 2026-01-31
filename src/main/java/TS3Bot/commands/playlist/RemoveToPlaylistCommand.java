package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.PlaylistServices;
import TS3Bot.model.Playlist;
import TS3Bot.model.Track;

import java.util.Arrays;
import java.util.stream.Collectors;
/**
 * Elimina una canción de una playlist.
 *
 * Commando para eliminar una canción utilizando como parametro su índice en la lista de playlists.
 * Está pensando para eliminar canciones de una playlist personalizada creada por el usuario.
 * Verificar qué la playlist qué desees modificar haya sido creada por el usuario. Realiza una validación
 *
 * @version 1.1
 */
public class RemoveToPlaylistCommand extends AsyncCommand {
    private final PlaylistServices playlistServices;

    public RemoveToPlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistServices = new PlaylistServices(bot);
    }

    @Override
    public String getName() {
        return "!removefromplaylist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!plremove", "!plrm", "!delfrompl"};
    }

    @Override
    public String getUsage() {
        return getName() + " <id_playlist> <canción/url>";
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Elimina una canción de tu playlist";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        if (ctx.getArgsArray().length < 2) {
            replyUsage();
            return;
        }

        try {
            String[] args = ctx.getArgsArray();
            int playlistIndex = Integer.parseInt(args[0]);

            Playlist playlist = playlistServices.getPlaylistByUserIndex(playlistIndex);

            playlistServices.canModifyPlaylist(ctx.getUserId(), ctx.getUserUid(), playlist, ADMIN_GROUP_ID);

            if (playlist == null) {
                replyError("Playlist #" + playlistIndex + " no encontrada.");
                return;
            }

            if (!playlistServices.isOwner(playlist, ctx.getUserUid())) {
                replyError("Solo puedes eliminar tus propias playlists.");
                return;
            }

            String query = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
            Track track = bot.getMusicManager().resolve(query);

            if (track == null) {
                replyError("No se pudo encontrar la canción.");
                return;
            }

            // Remover usando utils
            boolean success = playlistServices.removeTrackFromPlaylist(playlist, track);

            if (success) {
                replyPlaylistAction(track.getTitle() + " eliminada de ", playlist.getName());
            } else {
                replyError("La canción no estaba en " + playlist.getName());
            }

        } catch (NumberFormatException e) {
            replyError("El ID debe ser numérico.");
        } catch (IllegalArgumentException e) {
          replyWarning("No puedes remover una canción de " + e.getMessage());
        } catch (Exception e) {
        }
    }
}