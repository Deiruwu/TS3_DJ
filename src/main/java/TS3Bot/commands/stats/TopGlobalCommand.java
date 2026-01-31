package TS3Bot.commands.stats;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.StatsServices;

/**
 * Top canciones escuchadas por el servidor
 *
 * Comando para mostrar las canciones más escuchadas en todo el servidor.
 *
 * @version 1.0
 */
public class TopGlobalCommand extends Command {

    private final StatsServices statsServices;

    public TopGlobalCommand(TeamSpeakBot bot) {
        super(bot);
        this.statsServices = new StatsServices(bot);
    }

    @Override
    public String getName() {
        return "!topglobal";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!gtop", "!serverrank"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat( " <cantidad>");
    }

    @Override
    public String getCategory() {
        return "Estadísticas";
    }

    @Override
    public String getDescription() {
        return "Muestra el ranking global de canciones más populares del servidor";
    }

    @Override
    public void execute(CommandContext ctx) {
        statsServices.handleListGlobalSongs(ctx, true);
    }
}