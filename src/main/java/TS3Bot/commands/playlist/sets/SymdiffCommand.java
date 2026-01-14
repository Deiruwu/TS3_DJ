package TS3Bot.commands.playlist.sets;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistSetUtils;

/**
 * Carga a la cola las canciones qué no se repiten en TODAS las playlists indicadas.
 *
 * Comando que carga canciones únicas (Diferencia Simétrica).
 * Carga solo aquellas canciones que aparecen en UNA SOLA de las playlists indicadas,
 * ignorando cualquiera que se repita entre ellas.
 *
 * @version 1.0
 */
public class SymdiffCommand extends Command {

    private final PlaylistSetUtils setUtils;

    public SymdiffCommand(TeamSpeakBot bot) {
        super(bot);
        this.setUtils = new PlaylistSetUtils(bot);
    }

    @Override
    public String getName() {
        return "!symdiff";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!plunique", "!diff"};
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
        return "Carga canciones exclusivas de cada playlist, omitiendo las que compartan entre sí";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            reply("[color=orange]Uso: " + getUsage() + "[/color]");
            return;
        }
        setUtils.handleSymDiff(ctx);
    }
}