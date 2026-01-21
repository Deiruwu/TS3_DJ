package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

/**
 * Comando para añadir canciones a la cola de reproducción.
 *
 * Este comando busca y resuelve canciones mediante URLs directas de YouTube
 * o términos de búsqueda, añadiéndolas al final de la cola de reproducción.
 * Si no hay nada reproduciéndose actualmente, la canción comenzará de forma
 * automática. Cada canción añadida queda asociada al usuario que la solicitó
 * para efectos de estadísticas y gestión de permisos.
 *
 * @version 1.0
 */
public class PlayCommand extends AsyncCommand {

    public PlayCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!play";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!p"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <canción/url>");
    }

    @Override
    public String getCategory() {
        return "Reproducción";
    }

    @Override
    public String getDescription() {
        return "Añade una canción a la cola de reproducción mediante búsqueda o URL directa de YouTube";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        try {
            Track track = bot.getMusicManager().resolve(ctx.getArgs());
            QueuedTrack queuedTrack = new QueuedTrack(track, ctx.getUserUid(), ctx.getUserName(), true);

            bot.getPlayer().queue(queuedTrack);
            replyMusicAdded(track.toString());

        } catch (Exception e) {
            System.out.println("Error al resolver track: " + e.getMessage());
            replyError("No se pudo encontrar o cargar la canción");
        }
    }
}