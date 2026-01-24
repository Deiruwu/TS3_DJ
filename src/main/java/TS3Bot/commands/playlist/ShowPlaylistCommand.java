package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;
import TS3Bot.model.Track;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Muestra el contenido de una playlist
 *
 * Comando para mostrar el contenido detallado de una playlist específica.
 * Lista todas las canciones almacenadas en la playlist seleccionada por ID.
 *
 * Uso:
 *   !showp 10              -> Muestra las primeras 15 canciones de la playlist 10
 *   !showp 10 20           -> Muestra las primeras 20 canciones de la playlist 10
 *   !showp 10 --latest     -> Muestra las últimas 15 canciones de la playlist 10
 *   !showp 10 30 --latest  -> Muestra las últimas 30 canciones de la playlist 10
 *
 * @version 2.0
 */
public class ShowPlaylistCommand extends Command {
    private final PlaylistUtils playlistUtils;

    public ShowPlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
    }

    @Override
    public String getName() {
        return "!showplaylist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!showp", "!viewpl", "!listtracks", "!spl"};
    }

    @Override
    public String getUsage() {
        return getName() + getStrAliases() + " <id_playlist> [cantidad] [--latest]";
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Muestra la lista de canciones contenidas en una playlist específica.\n" +
                "Usa --latest para ver las últimas canciones añadidas.";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        String[] args = ctx.getArgsArray();

        Playlist playlist = playlistUtils.resolvePlaylist(args[0]);
        if (playlist == null) {
            replyError("Playlist no encontrada.");
            return;
        }

        List<Track> tracks = playlistUtils.getTracksFromPlaylist(playlist);

        if (tracks.isEmpty()) {
            replyError("La playlist " + playlist.getName() + " está vacía.");
            return;
        }

        List<String> formattedTracks = tracks.stream()
                .map(Track::toString)
                .collect(Collectors.toList());

        boolean showLatest = ctx.hasAnyFlag("latest", "last");

        replyListHeader(playlist.getName());

        if (args.length > 1) {
            try {
                int limit = Integer.parseInt(args[1]);
                replyList(formattedTracks, limit, showLatest);
            } catch (NumberFormatException e) {
                replyList(formattedTracks, showLatest);
            }
        } else {
            replyList(formattedTracks, showLatest);
        }
    }
}