package TS3Bot.commands.playlist;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.Command;
import TS3Bot.commands.CommandContext;
import TS3Bot.commands.services.PlaylistServices;
import TS3Bot.model.ConnectedUser;
import TS3Bot.model.Playlist;

public class InviteCollaboratorCommand extends Command {

    private final PlaylistServices playlistServices;
    private static final int INVITE_TIMEOUT = 30;
    private static final int ADMIN_GROUP_ID = 6;

    public InviteCollaboratorCommand(TeamSpeakBot bot) {
        super(bot);
        this.playlistServices = new PlaylistServices(bot);
    }

    @Override
    public String getName() {
        return "!collab";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!invite", "!addcollab"};
    }

    @Override
    public String getUsage() {
        return getName() + " <id_playlist> <nickname>";
    }

    @Override
    public String getCategory() {
        return "Playlist";
    }

    @Override
    public String getDescription() {
        return "Invita a un usuario conectado a colaborar en tu playlist.";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (!ctx.hasArgs() || ctx.getArgsArray().length < 2) {
            replyUsage();
            return;
        }

        String[] parts = ctx.getSplitArgs(2);

        Playlist playlist = playlistServices.resolvePlaylist(parts[0]);
        if (playlist == null) {
            replyError("Playlist #" + parts[0] + " no encontrada.");
            return;
        }

        playlistServices.canModifyPlaylist(ctx.getUserId(), ctx.getUserUid(), playlist, ADMIN_GROUP_ID);

        String targetNick = parts[1];

        ConnectedUser targetUser = bot.getUserByName(targetNick);

        if (targetUser == null) {
            replyError("El usuario [b]" + targetNick + "[/b] no existe o no está conectado.");
            return;
        }

        if (targetUser.getUniqueId().equals(ctx.getUserUid())) {
            replyError("No puedes invitarte a ti mismo.");
            return;
        }

        if (bot.getPlaylistManager().isCollaborator(playlist.getId(), targetUser.getUniqueId())) {
            replyError(targetUser.getNickname() + " ya es colaborador.");
            return;
        }

        replyInfo("Invitación enviada a [b]" + targetUser.getNickname() + "[/b]. Esperando respuesta...");

        replyPoke(targetUser.getClientId(), "Invitación: Colaborar en playlist '" + playlist.getName() + "'");

        replyConfirmation(
                "Colaborar con",
                playlist.getOwnerName(),
                "en " + playlist.getName(),
                INVITE_TIMEOUT
        );

        bot.getConfirmationManager().requestConfirmation(
                targetUser.getUniqueId(),
                targetUser.getClientId(),
                () -> handleAccept(playlist, targetUser, ctx),
                () -> handleReject(playlist, targetUser, ctx),
                INVITE_TIMEOUT
        );
    }

    private void handleAccept(Playlist playlist, ConnectedUser target, CommandContext ctx) {
        boolean success = bot.getPlaylistManager().addCollaborator(playlist.getId(), target.getUniqueId());

        if (success) {
            replySuccess("Ahora " + target + " es colaborador de: [b]" + playlist.getName() + "[/b]");
        } else {
            replyError("No se pudo agregar " + target + " como colaborador.");
        }
    }

    private void handleReject(Playlist playlist, ConnectedUser target, CommandContext ctx) {
        replyWarning(target + " no aceptó tu invitacón para colaborar en [b]" + playlist.getName() + "[/b].");
    }
}