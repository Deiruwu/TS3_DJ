package TS3Bot.commands.playback;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaybackUtils;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;
import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrega canciones a tus favoritos usando índices de la cola.
 *
 * Comportamiento:
 * 1. Sin argumentos -> Guarda la canción actual.
 * 2. Con argumentos -> Guarda canciones de la cola por índice o rango.
 *
 * @version 1.0
 */
public class LikeCommand extends AsyncCommand {
    private final PlaylistUtils playlistUtils;
    private final PlaybackUtils playbackUtils;

    public LikeCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
        this.playbackUtils = new PlaybackUtils(bot);
    }

    @Override
    public String getName() {
        return "!like";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!fav", "!lk"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases()).concat(" <Vacío (Para la actual)> | <índice> | <desde> <hasta>");
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Añade la canción actual o un rango de la cola a favoritos.";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        Playlist favorites = playlistUtils.ensureUserFavoritesPlaylist(ctx.getUserUid(), ctx.getUserName());
        if (favorites == null) {
            replyError("No se pudo acceder a tu playlist de favoritos.");
            return;
        }

        List<Track> tracksToLike = new ArrayList<>();

        if (!ctx.hasArgs()) {
            QueuedTrack current = bot.getPlayer().getCurrentTrack();
            if (current != null) {
                tracksToLike.add(current.getTrack());
            } else {
                replyError("No hay nada sonando.");
                return;
            }
        } else {
            try {
                int[] indices = playbackUtils.resolveIndices(ctx.getArgsArray());
                List<QueuedTrack> queue = bot.getPlayer().getQueue();

                if (queue.isEmpty()) {
                    replyError("La cola está vacía, no puedes seleccionar índices.");
                    return;
                }

                int start = indices[0];
                int end = (indices.length > 1) ? indices[1] : start;

                if (start >= 0 && end < queue.size() && start <= end) {
                    for (int i = start; i <= end; i++) {
                        tracksToLike.add(queue.get(i).getTrack());
                    }
                } else {
                    replyError("Índices fuera de rango. La cola tiene " + queue.size() + " canciones.");
                    return;
                }

            } catch (NumberFormatException e) {
                replyError("Los argumentos deben ser números (índices de la cola).");
                return;
            } catch (IllegalArgumentException e) {
                return;
            }
        }

        if (tracksToLike.isEmpty()) return;

        int addedCount = 0;
        StringBuilder lastAdded = new StringBuilder();

        for (Track t : tracksToLike) {
            boolean added = bot.getPlaylistManager().addSongToPlaylist(favorites.getId(), t.getUuid());
            if (added) {
                addedCount++;
                lastAdded = new StringBuilder(t.getTitle());
            }
        }

        if (addedCount > 0) bot.refreshPlaylists();

        if (addedCount == 0) {
            replyAction("Las canciones seleccionadas ya estaban en tus favoritos.");
        } else if (addedCount == 1) {
            reply("Añadida a favoritos: [b]" + lastAdded + "[/b]");
        } else {
            reply("Añadidas [b]" + addedCount + "[/b] canciones a tus favoritos.");
        }
    }
}