package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;
import TS3Bot.model.Track;

/**
 * Añade una cancion a una playlist
 *
 * Comando para agregar una canción a una playlist existente.
 * Busca y resuelve una canción para guardarla directamente en la base de datos.
 *
 * @version 1.0
 */
public class AddToPlaylistCommand extends AsyncCommand {
    private final PlaylistUtils playlistUtils;


    public AddToPlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
    }

    @Override
    public String getName() {
        return "!addtoplaylist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!pladd", "!ap", "!addp"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <id_playlist> <canción/url>");
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Añade una canción a una playlist específica";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        String[] parts = ctx.getSplitArgs(2);
        if (parts.length < 2) {
            replyUsage();
            return;
        }

        try {
            Playlist playlist = playlistUtils.getPlaylistByUserIndex(Integer.parseInt(parts[0]));
            if (playlist == null) {
                reply("[color=red]Playlist no encontrada.[/color]");
                return;
            }

            Track track = bot.getMusicManager().resolve(parts[1]);
            playlistUtils.addTrackToPlaylist(playlist, track);

            reply("[color=lime]Añadido a[/color] [b]" + playlist.getName() + "[/b]");
        } catch (Exception e) {
            reply("[color=red]Error: " + e.getMessage() + "[/color]");
        }
    }
}