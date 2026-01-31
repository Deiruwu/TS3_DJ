package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.PlaylistServices;
import TS3Bot.model.Playlist;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

/**
 *
 * Elimina una canción de tus "me gusta"
 *
 * Commando para eliminar una canción de tus "Música de <Name>"
 * La cual es una categoría personalizada de TS3Bot para guardar
 * Todas las canciones que has pedido/reproducido.
 * Si no especificas la canción, toma como argumento la canción
 * qué está sonando actualmente.
 *
 * @version 1.0
 */

public class DislikeCommand extends AsyncCommand {
    private final PlaylistServices playlistServices;

    public DislikeCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistServices = new PlaylistServices(bot);
    }

    @Override
    public String getName() {
        return "!dislike";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!unlike", "!removefav"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <Vacío (Para la actual)> | <canción>");
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Elimina una canción de tus favoritos (canción actual si no se especifica)";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        Playlist favoritesPlaylist = playlistServices.ensureUserFavoritesPlaylist(
                ctx.getUserUid(),
                ctx.getUserName()
        );

        if (favoritesPlaylist == null) {
            replyError("No se pudo acceder a tu playlist de favoritos.");
            return;
        }

        Track trackToRemove;

        try {
            if (!ctx.hasArgs()) {
                QueuedTrack current = bot.getPlayer().getCurrentTrack();
                if (current == null) {
                    replyWarning("No hay nada sonando ahora mismo.");
                    return;
                }
                trackToRemove = current.getTrack();
            } else {
                trackToRemove = bot.getMusicManager().resolve(ctx.getArgs());
            }

            boolean success = playlistServices.removeTrackFromPlaylist(favoritesPlaylist, trackToRemove);

            if (success) {
                replyPlaylistAction("Eliminada de favoritos: ", trackToRemove.getTitle());
            } else {
                replyWarning("La canción no estaba en tus favoritos.");
            }

        } catch (Exception e) {
        }
    }
}