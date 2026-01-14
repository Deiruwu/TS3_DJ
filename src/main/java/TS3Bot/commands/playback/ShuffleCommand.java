package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

/**
 * Comando para mezclar aleatoriamente la cola de reproducción.
 *
 * Este comando reorganiza al azar todas las canciones que se encuentran
 * actualmente en espera en la cola, sin afectar a la canción que se
 * está reproduciendo en ese momento.
 *
 * @version 1.0
 */
public class ShuffleCommand extends Command {

    public ShuffleCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!shuffle";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!sh", "!mix"};
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
        return "Mezcla aleatoriamente el orden de las canciones en la cola de reproducción";
    }

    @Override
    public void execute(CommandContext ctx) {
        bot.getPlayer().shuffle();
        replyImportant("La cola de reproducción ha sido mezclada por " + ctx.getUserName());
    }
}