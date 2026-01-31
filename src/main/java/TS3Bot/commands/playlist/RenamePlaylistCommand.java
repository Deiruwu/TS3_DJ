package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.PlaylistServices;
import TS3Bot.model.Playlist;
import TS3Bot.model.enums.PlaylistType;

public class RenamePlaylistCommand extends Command {
    private final PlaylistServices playlistServices;

    public RenamePlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistServices = new PlaylistServices(bot);
    }

    @Override
    public String getName() {
        return "!renameplaylist";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!renamep", "!rnp"};
    }

    @Override
    public String getUsage() {
        return getName().concat( getStrAliases()).concat(" <id_playlist> <nuevo_nombre>");
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Renombra una playlist existente. Solo puedes renombrar tus propias playlists de tipo USER.";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs()) {
            replyUsage();
            return;
        }

        String[] args = ctx.getSplitArgs(2);
        if (args.length < 2) {
            replyUsage();
            return;
        }

        Playlist playlist = playlistServices.resolvePlaylist(args[0]);

        try {
            playlistServices.canModifyPlaylist(ctx.getUserId(), ctx.getUserUid(), playlist, ADMIN_GROUP_ID);

            if (playlist == null) {
                replyError("Playlist no encontrada.");
                return;
            }

            if (!playlistServices.isOwner(playlist, ctx.getUserUid())) {
                replyError("Solo puedes renombrar tus propias playlists.");
                return;
            }

            if (playlist.getType() != PlaylistType.USER) {
                replyError("Solo puedes renombrar playlists de tipo USER.");
                return;
            }

            String newName = args[1].trim();

            String validation = playlistServices.validatePlaylistName(newName);
            if (validation != null) {
                replyError(validation);
                return;
            }

            if (playlistServices.playlistNameExists(newName, ctx.getUserUid())) {
                replyError("Ya tienes una playlist con ese nombre.");
                return;
            }

            boolean success = bot.getPlaylistManager().updateName(playlist.getId(), newName);

            if (success) {
                bot.refreshPlaylists();
                replyPlaylistAction(playlist.getName() + " fue renombrada a ", newName);
            } else {
                replyError("No se pudo renombrar la playlist.");
            }
        } catch (IllegalStateException e) {
            replyWarning("No puedes renombrar " + e.getMessage());
        }
    }
}