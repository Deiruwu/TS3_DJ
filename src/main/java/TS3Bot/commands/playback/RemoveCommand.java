package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaybackUtils;

import java.util.Arrays;

/**
 * Comando para eliminar canciones específicas de la cola de reproducción.
 *
 * Permite remover una canción individual mediante su índice o un rango
 * de canciones especificando posiciones de inicio y fin. Los índices
 * se basan en la numeración mostrada por el comando queue.
 *
 * @version 1.0
 */
public class RemoveCommand extends Command {
    private final PlaybackUtils playbackutils;

    public RemoveCommand(TeamSpeakBot bot) {
        super(bot);
        this.playbackutils = new PlaybackUtils(bot);
    }

    @Override
    public String getName() {
        return "!remove";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!rm"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <índice> | <desde> <hasta>");
    }

    @Override
    public String getCategory() {
        return "Reproducción";
    }

    @Override
    public String getDescription() {
        return "Elimina una canción específica o un rango de canciones de la cola usando sus índices de posición";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        try {
            int[] idx = playbackutils.resolveIndices(ctx.getArgsArray());

            boolean ok = idx.length == 1
                    ? bot.getPlayer().removeFromQueue(idx[0])
                    : bot.getPlayer().removeFromQueue(idx[0], idx[1]);

            if (ok) {
                String message = idx.length == 1
                        ? "Canción #" + (idx[0] + 1) + " eliminada de la cola."
                        : "Canciones #" + (idx[0] + 1) + " a #" + (idx[1] + 1) + " eliminadas de la cola.";
                replyAction(message);
            }

        } catch (NumberFormatException e) {
            replyError("Los índices deben ser números enteros.");
        }
    }
}