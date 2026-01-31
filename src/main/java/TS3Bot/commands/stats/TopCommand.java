package TS3Bot.commands.stats;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.StatsServices;

/**
 * Top canciones escuchadas por el usuario
 *
 * Comando para mostrar las canciones más escuchadas por el usuario.
 *
 * @version 1.0
 */
public class TopCommand extends Command {

    private final StatsServices statsServices;

    public TopCommand(TeamSpeakBot bot) {
        super(bot);
        this.statsServices = new StatsServices(bot);
    }

    @Override
    public String getName() {
        return "!top";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!mytop", "!rank"};
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
        return "Muestra tu top personal de canciones más reproducidas";
    }

    @Override
    public void execute(CommandContext ctx) {
        statsServices.handleListSongsByUser(ctx, true);
    }
}