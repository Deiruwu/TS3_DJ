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
 * Uso:
 *   !queue              -> Muestra las primeras 15 canciones
 *   !queue 20           -> Muestra las primeras 20 canciones
 *   !queue --latest     -> Muestra las últimas 15 canciones
 *   !queue 30 --latest  -> Muestra las últimas 30 canciones
 *
 * @version 2.0
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
        return getName() + getStrAliases() + " [cantidad] [--latest]";
    }

    @Override
    public String getCategory() {
        return "Reproducción";
    }

    @Override
    public String getDescription() {
        return "Muestra la canción actualmente en reproducción y todas las canciones en espera en la cola.\n" +
                "Usa --latest para ver las últimas canciones de la cola.";
    }

    @Override
    public void execute(CommandContext ctx) {
        String actual = bot.getPlayer().getCurrentTrack().toString();
        List<String> canciones = bot.getPlayer().getQueueList();

        replyNowPlaying(actual);

        boolean showLatest = ctx.hasAnyFlag("latest", "last");

        if (ctx.hasArgs()) {
            try {
                int limit = Integer.parseInt(ctx.getArgsArray()[0]);
                replyList(canciones, limit, showLatest);
            } catch (NumberFormatException e) {
                replyList(canciones, showLatest);
            }
        } else {
            replyList(canciones, showLatest);
        }
    }
}