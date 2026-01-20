package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lista todas las playlists disponibles.
 *
 * Comando para listar todas las playlists disponibles en el sistema.
 * Muestra el ID numérico y el nombre de cada lista.
 *
 * @version 1.0
 */
public class ListPlaylistsCommand extends Command {
    private final PlaylistUtils playlistUtils;

    public ListPlaylistsCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
    }

    @Override
    public String getName() {
        return "!playlists";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!pls", "!listp"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases());
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Muestra todas las playlists disponibles";
    }

    @Override
    public void execute(CommandContext ctx) {
        List<Playlist> allPlaylists = bot.getAllPlaylists();

        if (allPlaylists.isEmpty()) {
            reply("[color=gray]No hay playlists. Usa !createp <nombre> para comenzar.[/color]");
            return;
        }

        reply("[color=blue][b]Playlists Disponibles:[/b][/color]");

        for (int i = 0; i < allPlaylists.size(); i++) {
            replyList(allPlaylists.stream()
                    .map(Playlist::toString)
                    .collect(Collectors.toList()));
        }
    }
}