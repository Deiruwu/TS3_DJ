package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

/**
 * Comando para saltar la canción actual.
 *
 * Detiene la reproducción de la pista actual inmediatamente y comienza
 * a reproducir la siguiente canción disponible en la cola. Si la cola
 * está vacía, la reproducción se detendrá.
 *
 * @version 1.0
 */
public class SkipCommand extends Command {

    public SkipCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!skip";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!s", "!next", "!n"};
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
        return "Salta la canción que se está reproduciendo actualmente y pasa a la siguiente";
    }

    @Override
    public void execute(CommandContext ctx) {
        reply("[color=orange]Saltando canción...[/color]");
        bot.getPlayer().next();
        bot.refreshPlaylists();
    }
}