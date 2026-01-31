package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

/**
 * Detiene la música actual y elimina la cola, deteniendo la música por completo.
 *
 * Commando utilizado para deter na la música totalmente, en esencia !skip y !clear
 * juntos en un solo comando.
 *
 * @version 1.0
 */
public class StopCommand extends Command {

    public StopCommand(TeamSpeakBot bot) { super(bot); }

    @Override
    public String getName() {
        return "!stop";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!st"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases());
    }

    @Override
    public String getCategory() {
        return "Reproducción";
    }

    @Override
    public String getDescription() {
        return "Detiene la canción actual y limpia la queue";
    }

    @Override
    public void execute(CommandContext ctx) {
        bot.getPlayer().shutdown();
        replySuccess("Música detenida por " + ctx.getUserName());
    }
}
