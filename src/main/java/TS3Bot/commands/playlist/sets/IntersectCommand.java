package TS3Bot.commands.playlist.sets;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.PlaylistSetServices;

/**
 * Carga a la cola las canciones que se repiten en TODAS las playlists indicadas.
 *
 * Comando que carga canciones que existen en TODAS las playlists seleccionadas.
 * Ejemplo: Si Playlist A tiene [1, 2, 3] y Playlist B tiene [2, 3, 4], carga [2, 3].
 *
 * @version 1.0
 */
public class IntersectCommand extends Command {

    private final PlaylistSetServices setUtils;

    public IntersectCommand(TeamSpeakBot bot) {
        super(bot);
        this.setUtils = new PlaylistSetServices(bot);
    }

    @Override
    public String getName() {
        return "!intersect";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!plintersect", "!inter"};
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
        return "Carga en la cola solo las canciones que se repiten en TODAS las playlists indicadas";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            reply("[color=orange]Uso: " + getUsage() + "[/color]");
            return;
        }
        setUtils.handleIntersect(ctx);
    }
}