package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

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

    public RemoveCommand(TeamSpeakBot bot) {
        super(bot);
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
            reply("[color=orange]Uso correcto: " + getUsage() + "[/color]");
            return;
        }

        try {
            int[] idx = Arrays.stream(ctx.getArgsArray())
                    .limit(2)
                    .mapToInt(s -> Integer.parseInt(s) - 1)
                    .toArray();

            if (idx.length == 0 || idx.length > 2) {
                throw new IllegalArgumentException("Número de argumentos inválido");
            }

            boolean ok = idx.length == 1
                    ? bot.getPlayer().removeFromQueue(idx[0])
                    : bot.getPlayer().removeFromQueue(idx[0], idx[1]);

            if (ok) {
                String message = idx.length == 1
                        ? "[color=orange]Canción #" + (idx[0] + 1) + " eliminada de la cola.[/color]"
                        : "[color=orange]Canciones #" + (idx[0] + 1) + " a #" + (idx[1] + 1) + " eliminadas de la cola.[/color]";
                reply(message);
            } else {
                reply("[color=red]Índice o rango inválido. Verifica los números con " + getName() + ".[/color]");
            }

        } catch (NumberFormatException e) {
            reply("[color=red]Los índices deben ser números enteros.[/color]");
        } catch (Exception e) {
            reply("[color=red]Uso correcto: " + getUsage() + "[/color]");
        }
    }
}