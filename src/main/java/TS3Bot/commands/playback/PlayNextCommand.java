package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

/**
 * Comando para añadir canciones con prioridad a la cola de reproducción.
 *
 * A diferencia del comando play estándar, este comando inserta la canción
 * solicitada en la primera posición de la cola, haciendo que sea la próxima
 * en reproducirse inmediatamente después de la canción actual.
 *
 * @version 1.0
 */
public class PlayNextCommand extends AsyncCommand {

    public PlayNextCommand(TeamSpeakBot bot) {
        super(bot);
    }

    @Override
    public String getName() {
        return "!playnext";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!pn"};
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
        return "Añade una canción como siguiente en la cola, otorgándole prioridad sobre las demás canciones en espera";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            reply("[color=orange]Uso correcto: " + getUsage() + "[/color]");
            return;
        }

        try {
            Track track = bot.getMusicManager().resolve(ctx.getArgs());
            QueuedTrack queuedTrack = new QueuedTrack(track, ctx.getUserUid(), ctx.getUserName());

            bot.getPlayer().queueNext(queuedTrack);
            reply("[color=lime]Siguiente:[/color] [i]" + track + "[/i]");

        } catch (Exception e) {
            System.out.println("Error al resolver track: " + e.getMessage());
            reply("[color=red]No se pudo encontrar o cargar la canción. Verifica el enlace o intenta con otros términos de búsqueda.[/color]");
        }
    }
}