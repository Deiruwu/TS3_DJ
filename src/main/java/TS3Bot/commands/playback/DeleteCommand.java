package TS3Bot.commands.maintenance;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DeleteCommand extends Command {

    public DeleteCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!delete";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!dl", "!borrar"};
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
        return "DETIENE la canción actual y la elimina FÍSICAMENTE de la BD y el disco.";
    }

    @Override
    public void execute(CommandContext ctx) {
        QueuedTrack current = bot.getPlayer().getCurrentTrack();

        if (current == null) {
            replyError("No hay nada sonando para borrar.");
            return;
        }

        Track trackToDelete = current.getTrack();
        String uuid = trackToDelete.getUuid();
        String title = trackToDelete.getTitle();

        bot.getPlayer().next();
        replyAction("Deteniendo reproducción para eliminación segura...");

        File file = new File(trackToDelete.getPath());
        boolean fileDeleted = false;

        if (file.exists()) {
            try {
                Thread.sleep(250);
                if (!file.delete()) {
                    System.gc();
                    Thread.sleep(100);
                    fileDeleted = file.delete();
                } else {
                    fileDeleted = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fileDeleted = true;
        }

        boolean dbDeleted = bot.getTrackDao().deleteTrack(uuid);

        if (dbDeleted) {
            replySuccess("[b]" + title + "[/b] ha sido eliminada de la base de datos y del disco.");
        } else {
            replyError("No se pudo borrar de la base de datos.");
        }
    }
}