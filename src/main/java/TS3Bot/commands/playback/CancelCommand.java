package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.audio.MetadataClient;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;

/**
 * Cancela la descarga en curso.
 *
 * Commando para cancelar una descarga en curso.
 * Elimina todo rastro en la base de datos de la descarga y del disco
 *
 * @version 1.0
 */
public class CancelCommand extends Command {

    public CancelCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!cancel";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!stopdw", "!abort"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases());
    }

    @Override
    public String getCategory() {
        return "Maintenance";
    }

    @Override
    public String getDescription() {
        return "Cancela inmediatamente la descarga en curso.";
    }

    @Override
    public void execute(CommandContext ctx) {
        boolean killed = MetadataClient.cancelCurrentDownload();

        if (killed) {
            replySuccess("[b]Descarga abortada.[/b] El proceso ha sido eliminado.");
        } else {
            replyError("No hay ninguna descarga activa en este momento.");
        }
    }
}