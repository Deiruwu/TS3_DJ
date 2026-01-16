package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.PlaylistType;

/**
 * Crea una nueva playlist con un nombre unico
 *
 * Comando para crear una nueva playlist vacía.
 * Asigna automáticamente la playlist al usuario que la crea.
 *
 * @version 1.0
 */
public class CreatePlaylistCommand extends Command {
    private final PlaylistUtils playlistUtils;

    public CreatePlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
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
            reply("[color=gray]Uso: " + getUsage() + "[/color]");
            return;
        }

        String name = ctx.getArgs().trim();

        // Validar nombre usando utils
        String validationError = playlistUtils.validatePlaylistName(name);
        if (validationError != null) {
            reply("[color=red]" + validationError + ".[/color]");
            return;
        }

        // Verificar duplicados
        if (playlistUtils.playlistNameExists(name, ctx.getUserUid())) {
            reply("[color=red]Ya tienes una playlist llamada [b]" + name + "[/b].[/color]");
            return;
        }

        // Crear playlist
        int id = bot.getPlaylistManager().createPlaylist(
                name,
                ctx.getUserUid(),
                ctx.getUserName(),
                PlaylistType.USER
        );

        if (id != -1) {
            bot.refreshPlaylists();
            reply("[color=lime]Playlist creada:[/color] [b]" + name + "[/b]");
        } else {
            replyError("Error al crear la playlist");
        }
    }
}