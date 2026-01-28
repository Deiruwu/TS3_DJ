package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.utils.PlaylistUtils;
import TS3Bot.model.Playlist;
import TS3Bot.model.PlaylistType;

public class RenamePlaylistCommand extends Command {
    private final PlaylistUtils playlistUtils;

    public RenamePlaylistCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistUtils = new PlaylistUtils(bot);
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

        Playlist playlist = playlistUtils.resolvePlaylist(args[0]);
        if (playlist == null) {
            replyError("Playlist no encontrada.");
            return;
        }

        if (!playlistUtils.isOwner(playlist, ctx.getUserUid())) {
            replyError("Solo puedes renombrar tus propias playlists.");
            return;
        }

        if (playlist.getType() != PlaylistType.USER) {
            replyError("Solo puedes renombrar playlists de tipo USER.");
            return;
        }

        String newName = args[1].trim();

        String validation = playlistUtils.validatePlaylistName(newName);
        if (validation != null) {
            replyError(validation);
            return;
        }

        if (playlistUtils.playlistNameExists(newName, ctx.getUserUid())) {
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
    }
}