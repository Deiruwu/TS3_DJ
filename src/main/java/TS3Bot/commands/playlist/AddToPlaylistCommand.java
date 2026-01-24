package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

/**
 * Añade una cancion a una playlist
 *
 * Comando para agregar una canción a una playlist existente.
 * Busca y resuelve una canción para guardarla directamente en la base de datos.
 * Se añadió la funcionalidad de escribir solamente el id de la playlist para añadir la canción actual
 * Además, se añade a tus favoritos para tener seguimiento
 *
 * @version 1.1
 */
public class AddToPlaylistCommand extends AsyncCommand {
    private final PlaylistUtils playlistUtils;


    public AddToPlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
    }

    @Override
    public String getName() {
        return "!addtoplaylist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!pladd", "!ap", "!addp"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <id_playlist> [canción/url]");
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Añade una canción a una playlist específica";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        String[] parts = ctx.getSplitArgs(2);

        try {
            Playlist targetPlaylist = playlistUtils.getPlaylistByUserIndex(Integer.parseInt(parts[0]));

            if (targetPlaylist == null) {
                replyError("Playlist #" + parts[0] + " no encontrada.");
                return;
            }

            QueuedTrack current = bot.getPlayer().getCurrentTrack();
            Track trackToAdd;

            if (parts.length == 2) {
                trackToAdd = bot.getMusicManager().resolve(parts[1]);
            } else {
                if (current == null) {
                    replyWarning("No hay nada sonando ahora mismo.");
                    return;
                }
                trackToAdd = current.getTrack();
            }

            boolean addedToTarget = playlistUtils.addTrackToPlaylist(targetPlaylist, trackToAdd);

            Playlist favorites = playlistUtils.ensureUserFavoritesPlaylist(ctx.getUserUid(), ctx.getUserName());
            boolean addedToFavorites = false;
            if (favorites != null && favorites.getId() != targetPlaylist.getId()) {
                addedToFavorites = playlistUtils.addTrackToPlaylist(favorites, trackToAdd);
            }

            if (addedToTarget && addedToFavorites) {
                replySuccess("[b]" + trackToAdd.getTitle() + "[/b] añadida a " + targetPlaylist.getName() + " y a [i]tus favoritos[/i]");
            } else if (addedToTarget) {
                replySuccess("[b]" + trackToAdd.getTitle() + "[/b] añadida a " + targetPlaylist.getName());
            } else {
                replyWarning("[b]" + trackToAdd.getTitle() + "[/b] ya estaba en " + targetPlaylist.getName());
            }

        } catch (NumberFormatException e) {
            replyError("El ID de la playlist debe ser un número.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }}