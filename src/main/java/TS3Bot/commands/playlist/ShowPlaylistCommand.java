package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;
import TS3Bot.model.Track;

import java.util.List;

/**
 * Muestra el conido de una playlist
 *
 * Comando para mostrar el contenido detallado de una playlist específica.
 * Lista todas las canciones almacenadas en la playlist seleccionada por ID.
 *
 * @version 1.0
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
        return getName().concat(getStrAliases()).concat(" <id_playlist>");
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Muestra la lista de canciones contenidas en una playlist específica";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        Playlist playlist = playlistUtils.resolvePlaylist(ctx.getArgs());
        if (playlist == null) {
            reply("[color=red]Playlist no encontrada.[/color]");
            return;
        }

        List<Track> tracks = playlistUtils.getTracksFromPlaylist(playlist);

        reply(playlistUtils.formatPlaylistHeader(playlist));
        for (int i = 0; i < tracks.size(); i++) {
            reply(playlistUtils.formatTrackEntry(i, tracks.get(i)));
        }
    }
}