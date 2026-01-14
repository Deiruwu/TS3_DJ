package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
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
    private final PlaylistUtils playlistUtils;

    public DislikeCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
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
        // Buscar o crear playlist de favoritos usando utils
        Playlist favoritesPlaylist = playlistUtils.ensureUserFavoritesPlaylist(
                ctx.getUserUid(),
                ctx.getUserName()
        );

        if (favoritesPlaylist == null) {
            reply("[color=red]Error al acceder a tu playlist de favoritos.[/color]");
            return;
        }

        Track trackToRemove;

        try {
            if (!ctx.hasArgs()) {
                // Sin argumentos: remover la canción que está sonando
                QueuedTrack current = bot.getPlayer().getCurrentTrack();
                if (current == null) {
                    reply("[color=gray]Nada sonando. Uso: " + getUsage() + "[/color]");
                    return;
                }
                trackToRemove = current.getTrack();
            } else {
                // Con argumentos: buscar la canción especificada
                trackToRemove = bot.getMusicManager().resolve(ctx.getArgs());
            }

            // Remover usando utils
            boolean success = playlistUtils.removeTrackFromPlaylist(favoritesPlaylist, trackToRemove);

            if (success) {
                reply("[color=green]Eliminada de favoritos:[/color] " + trackToRemove.getTitle());
            } else {
                reply("[color=orange]Esa canción no estaba en tus favoritos.[/color]");
            }

        } catch (Exception e) {
            reply("[color=red]Error: " + e.getMessage() + "[/color]");
        }
    }
}