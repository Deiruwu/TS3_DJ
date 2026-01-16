package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

/**
 * Limpia completamente la cola de reproducción actual.
 *
 * Este comando elimina todas las canciones en espera, dejando únicamente
 * la pista que está sonando en este momento si existe alguna.
 *
 * @version 1.0
 */
public class ClearCommand extends Command {

    public ClearCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!clear";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!cl", "!vaciar"};
    }

    @Override
    public String getUsage() {
        return getName() + getStrAliases();
    }

    @Override
    public String getCategory() {
        return "Reproducción";
    }

    @Override
    public String getDescription() {
        return "Limpia toda la cola de reproducción, eliminando todas las canciones en espera sin detener la reproducción actual";
    }

    @Override
    public void execute(CommandContext ctx) {
        bot.getPlayer().clear();
        replyAction("Cola de reproducción vaciada por: ", ctx.getUserName());
    }
}