package TS3Bot.commands.stats;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.StatsServices;

/**
 * Top menos escuchadas por el servidor
 *
 * Comando para mostrar las canciones menos escuchadas en todo el servidor.
 *
 * @version 1.0
 */
public class LeastGlobalCommand extends Command {

    private final StatsServices statsServices;

    public LeastGlobalCommand(TeamSpeakBot bot) {
        super(bot);
        this.statsServices = new StatsServices(bot);
    }

    @Override
    public String getName() {
        return "!leastglobal";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!gleast", "!serverworst"};
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
        return "Muestra el ranking global de canciones con menos reproducciones";
    }

    @Override
    public void execute(CommandContext ctx) {
        statsServices.handleListGlobalSongs(ctx, false);
    }
}