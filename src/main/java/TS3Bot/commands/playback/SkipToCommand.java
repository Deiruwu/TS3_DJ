package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

/**
 * Comando para saltar hasta una posición específica de la cola.
 *
 * Omite todas las canciones anteriores al índice indicado y comienza
 * a reproducir inmediatamente la canción seleccionada.
 *
 * @version 1.0
 */
public class SkipToCommand extends Command {

    public SkipToCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!skipto";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!jump", "!goto"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <índice>");
    }

    @Override
    public String getCategory() {
        return "Reproducción";
    }

    @Override
    public String getDescription() {
        return "Salta todas las canciones de la cola hasta llegar a la posición indicada";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        try {
            int targetIndex = Integer.parseInt(ctx.getArgsArray()[0]) - 1;

            if (targetIndex < 0 || targetIndex >= bot.getPlayer().getQueueList().size()) {
                replyError("El índice está fuera de rango. Verifica la cola con !queue.");
                return;
            }

            replySuccess("Saltando hasta la canción #" + (targetIndex + 1) + "...");

            bot.getPlayer().skipTo(targetIndex);
            bot.refreshPlaylists();

        } catch (NumberFormatException e) {
            replyError("El índice debe ser un número entero válido.");
        } catch (Exception e) {
        }
    }
}