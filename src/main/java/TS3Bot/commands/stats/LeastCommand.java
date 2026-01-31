package TS3Bot.commands.stats;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.StatsServices;

/**
 * Top menos escuchadas por el usuario
 *
 * Comando para mostrar las canciones menos escuchadas por el usuario.
 *
 * @version 1.0
 */
public class LeastCommand extends Command {

    private final StatsServices statsServices;

    public LeastCommand(TeamSpeakBot bot) {
        super(bot);
        this.statsServices = new StatsServices(bot);
    }

    @Override
    public String getName() {
        return "!least";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!myleast", "!worst"};
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
        return "Muestra tus canciones con menos reproducciones registradas";
    }

    @Override
    public void execute(CommandContext ctx) {
        statsServices.handleListSongsByUser(ctx, false);
    }
}