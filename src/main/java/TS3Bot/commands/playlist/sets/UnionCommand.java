package TS3Bot.commands.playlist.sets;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistSetUtils;

/**
 * Carga a la cola las canciones de todas las playlists indicadas.
 *
 * Comando que mezcla todas las canciones de las playlists seleccionadas sin repetir.
 * Utiliza un algoritmo Round-Robin para intercalar los resultados equitativamente.
 *
 * @version 1.0
 */
public class UnionCommand extends Command {

    private final PlaylistSetUtils setUtils;

    public UnionCommand(TeamSpeakBot bot) {
        super(bot);
        this.setUtils = new PlaylistSetUtils(bot);
    }

    @Override
    public String getName() {
        return "!union";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!plunion", "!mixall"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <id_playlist1> <id_playlist2> ...");
    }

    @Override
    public String getCategory() {
        return "Playlist Avanzada";
    }

    @Override
    public String getDescription() {
        return "Mezcla múltiples playlists en la cola, eliminando duplicados y barajando el resultado";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }
        setUtils.handleUnion(ctx);
    }
}