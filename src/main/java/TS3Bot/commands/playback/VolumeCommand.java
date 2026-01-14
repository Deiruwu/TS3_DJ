package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

/**
 * Comando para ajustar el volumen de reproducción.
 *
 * Permite establecer el volumen del bot en un rango de 0 a 100 (o más si se desea ganancia).
 * El cambio se aplica inmediatamente y se guarda en la configuración para persistir
 * tras reinicios.
 *
 * @version 1.0
 */
public class VolumeCommand extends Command {

    public VolumeCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!volume";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!vol", "!v"};
    }

    @Override
    public String getUsage() {
        String usage = getName();
        String[] aliases = getAliases();

        if (aliases != null && aliases.length > 0) {
            usage = usage.concat(" (").concat(String.join(", ", aliases)).concat(")");
        }

        return usage.concat(" <0-100>");
    }

    @Override
    public String getCategory() {
        return "Reproducción";
    }

    @Override
    public String getDescription() {
        return "Ajusta el volumen de reproducción del bot y guarda la preferencia";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            reply("[color=orange]Uso correcto: " + getUsage() + "[/color]");
            return;
        }

        try {
            int vol = Integer.parseInt(ctx.getArgsArray()[0]);
            if (vol < 0 || vol > 150) {
                reply("[color=red]Por favor ingresa un valor entre 0 y 150.[/color]");
                return;
            }

            bot.getPlayer().setVolume(vol);

            bot.saveVolumeConfig(vol);

            replyImportant("Volumen ajustado al " + vol + "% por " + ctx.getUserName());

        } catch (NumberFormatException e) {
            reply("[color=red]El volumen debe ser un número entero.[/color]");
        } catch (Exception e) {
            reply("[color=red]Error al ajustar volumen: " + e.getMessage() + "[/color]");
        }
    }
}