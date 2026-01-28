package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.ShuffleMode;

/**
 * Comando para mezclar la cola de reproducción con diferentes algoritmos.
 *
 * Modos disponibles (como flags):
 * default (sin flags): Aleatorio puro
 * --harmonic: Mezcla armónica basada en Camelot Wheel y BPM
 * --rising: Orden ascendente por BPM (energía creciente)
 * --falling: Orden descendente por BPM (energía decreciente)
 * --wave: Alterna entre BPMs altos y bajos (montaña rusa)
 *
 * @version 2.0
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
        return getName() + getStrAliases() + " [--harmonic|--rising|--falling|--wave]";
    }

    @Override
    public String getCategory() {
        return "Reproducción";
    }

    @Override
    public String getDescription() {
        return "Mezcla la cola con diferentes algoritmos (usa flags: --harmonic, --rising, --falling, --wave)";
    }

    @Override
    public void execute(CommandContext ctx) {
        ShuffleMode mode = ShuffleMode.CHAOS;

        if (ctx.hasFlag("harmonic") || ctx.hasFlag("h")) {
            mode = ShuffleMode.HARMONIC;
        } else if (ctx.hasFlag("rising") || ctx.hasFlag("r")) {
            mode = ShuffleMode.RISING;
        } else if (ctx.hasFlag("falling") || ctx.hasFlag("f")) {
            mode = ShuffleMode.FALLING;
        } else if (ctx.hasFlag("wave") || ctx.hasFlag("w")) {
            mode = ShuffleMode.WAVE;
        }

        bot.getPlayer().shuffle(mode);

        String modeDisplay = getModeDisplayName(mode);
        replySuccess("Cola mezclada en modo " + modeDisplay + " por: " + ctx.getUserName());
    }

    private String getModeDisplayName(ShuffleMode mode) {
        switch (mode) {
            case HARMONIC: return "[color=#BD93F9]Harmonic[/color]";
            case RISING: return "[color=Lime]Rising[/color]";
            case FALLING: return "[color=Orange]Falling[/color]";
            case WAVE: return "[color=DeepPink]Wave[/color]";
            case CHAOS:
            default: return "[color=Red]Chaos[/color]";
        }
    }
}