package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
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
 * @version 1.0
 */
public class RemoveToPlaylistCommand extends AsyncCommand {
    private final PlaylistUtils playlistUtils;

    public RemoveToPlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
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
            reply("[color=gray]Uso: " + getUsage() + "[/color]");
            return;
        }

        try {
            String[] args = ctx.getArgsArray();
            int playlistIndex = Integer.parseInt(args[0]);

            Playlist playlist = playlistUtils.getPlaylistByUserIndex(playlistIndex);
            if (playlist == null) {
                reply("[color=red]Playlist no encontrada. Verifica el ID con !playlists.[/color]");
                return;
            }

            if (!playlistUtils.isOwner(playlist, ctx.getUserUid())) {
                reply("[color=red]No tienes permiso para modificar [b]" + playlist.getName() + "[/b].[/color]");
                return;
            }

            String query = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
            Track track = bot.getMusicManager().resolve(query);

            if (track == null) {
                reply("[color=red]No se encontró la canción.[/color]");
                return;
            }

            // Remover usando utils
            boolean success = playlistUtils.removeTrackFromPlaylist(playlist, track);

            if (success) {
                reply("[color=green]Eliminada:[/color] [b]" + track.getTitle() +
                        "[/b] de [b]" + playlist.getName() + "[/b].");
            } else {
                reply("[color=orange]Esa canción no estaba en [b]" + playlist.getName() + "[/b].[/color]");
            }

        } catch (NumberFormatException e) {
            reply("[color=red]El ID debe ser numérico.[/color]");
        } catch (Exception e) {
            reply("[color=red]Error: " + e.getMessage() + "[/color]");
        }
    }
}