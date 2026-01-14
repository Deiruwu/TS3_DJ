package TS3Bot.commands.stats;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.StatsUtils;

/**
 * Top canciones escuchadas por el usuario
 *
 * Comando para mostrar las canciones más escuchadas por el usuario.
 *
 * @version 1.0
 */
public class TopCommand extends Command {

    private final StatsUtils statsUtils;

    public TopCommand(TeamSpeakBot bot) {
        super(bot);
        this.statsUtils = new StatsUtils(bot);
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
        statsUtils.handleListSongsByUser(ctx, true);
    }
}