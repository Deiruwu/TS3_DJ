package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.PlaylistServices;
import TS3Bot.model.Playlist;
import TS3Bot.model.enums.PlaylistType;

public class DeletePlaylistCommand extends Command {
    private final PlaylistServices playlistServices;
    private static final int CONFIRMATION_TIMEOUT = 20;

    public DeletePlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistServices = new PlaylistServices(bot);
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

        Playlist playlist = playlistServices.resolvePlaylist(ctx.getArgs());
        if (playlist == null) {
            replyError("Playlist no encontrada.");
            return;
        }

        try {
            playlistServices.canModifyPlaylist(ctx.getUserId(), ctx.getUserUid(), playlist, ADMIN_GROUP_ID);

            int trackCount = playlistServices.getTracksFromPlaylist(playlist).size();

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
        } catch (IllegalStateException e) {
            replyError("No puedes eliminar " + e.getMessage());
        }
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