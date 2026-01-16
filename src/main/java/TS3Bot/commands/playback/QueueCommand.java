package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

import java.util.List;

/**
 * Comando para visualizar la cola de reproducción actual.
 *
 * Muestra la canción que está sonando en este momento y la lista completa
 * de canciones que están en espera para ser reproducidas.
 * También incluye quien pidió la canción o de qué playlist proviene.
 *
 * @version 1.1
 */
public class QueueCommand extends Command {

    public QueueCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!queue";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!q"};
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
        return "Muestra la canción actualmente en reproducción y todas las canciones en espera en la cola";
    }

    @Override
    public void execute(CommandContext ctx) {
        String actual = bot.getPlayer().getCurrentTrack().toString();
        List<String> canciones = bot.getPlayer().getQueueList();

        reply("[b][color=blue]Estás escuchando: [/color][/b]" + actual);
        replyList(canciones);
    }
}