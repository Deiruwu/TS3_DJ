package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.PlaylistServices;
import TS3Bot.model.enums.PlaylistType;

/**
 * Crea una nueva playlist con un nombre unico
 *
 * Comando para crear una nueva playlist vacía.
 * Asigna automáticamente la playlist al usuario que la crea.
 *
 * @version 1.0
 */
public class CreatePlaylistCommand extends Command {
    private final PlaylistServices playlistServices;

    public CreatePlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistServices = new PlaylistServices(bot);
    }

    @Override
    public String getName() {
        return "!createplaylist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!createp", "!mkpl", "!newpl"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <nombre>");
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Crea una nueva playlist personal vacía";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        String name = ctx.getArgs().trim();

        String validationError = playlistServices.validatePlaylistName(name);
        if (validationError != null) {
            replyError(validationError);
            return;
        }

        if (playlistServices.playlistNameExists(name, ctx.getUserUid())) {
            replyWarning("Ya existe una playlist con ese nombre.");
            return;
        }

        int id = bot.getPlaylistManager().createPlaylist(
                name,
                ctx.getUserUid(),
                PlaylistType.USER
        );

        if (id != -1) {
            bot.refreshPlaylists();
            replyPlaylistAction("Playlist creada: ", name);
        } else {
            replyError("Error al crear la playlist");
        }
    }
}