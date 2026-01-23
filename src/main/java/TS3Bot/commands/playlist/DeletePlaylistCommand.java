package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;
import TS3Bot.model.PlaylistType;

public class DeletePlaylistCommand extends Command {
    private final PlaylistUtils playlistUtils;
    private static final int CONFIRMATION_TIMEOUT = 20;

    public DeletePlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
    }

    @Override
    public String getName() {
        return "!deleteplaylist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!deletep", "!delpl", "!removepl"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases().concat(" <id_playlist>"));
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Elimina una playlist existente. Solo puedes eliminar tus propias playlists de tipo USER.";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        Playlist playlist = playlistUtils.resolvePlaylist(ctx.getArgs());
        if (playlist == null) {
            replyError("Playlist no encontrada.");
            return;
        }

        if (!playlistUtils.isOwner(playlist, ctx.getUserUid())) {
            replyWarning("Solo puedes eliminar tus propias playlists.");
            return;
        }

        if (playlist.getType() != PlaylistType.USER) {
            replyWarning("No puedes eliminar playlists de tipo " + playlist.getType() + ".");
            return;
        }

        int trackCount = playlistUtils.getTracksFromPlaylist(playlist).size();

        replyConfirmation(
                "Eliminar",
                playlist.getName(),
                trackCount + " canciones",
                CONFIRMATION_TIMEOUT
        );

        int clientId = bot.getClientIdByUid(ctx.getUserUid());
        if (clientId != -1) {
            replyPoke(clientId, "Confirma eliminación de playlist: " + playlist.getName());
        }

        bot.getConfirmationManager().requestConfirmation(
                ctx.getUserUid(),
                clientId,
                () -> performDeletion(playlist),
                () -> reply("[color=gray]Eliminación cancelada.[/color]"),
                CONFIRMATION_TIMEOUT
        );
    }

    private void performDeletion(Playlist playlist) {
        boolean success = bot.getPlaylistManager().deletePlaylist(playlist.getId());

        if (success) {
            bot.refreshPlaylists();
            replyPlaylistAction("Playlist eliminada: ", playlist.getName());
        } else {
            replyError("No se pudo eliminar la playlist.");
        }
    }
}