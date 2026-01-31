package TS3Bot.commands.services;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.CommandContext;
import TS3Bot.interfaces.Replyable;
import TS3Bot.model.PlayStats;

import java.util.List;

public class StatsServices implements Replyable {

    private final TeamSpeakBot bot;

    public StatsServices(TeamSpeakBot bot) {
        this.bot = bot;
    }

    @Override
    public TeamSpeakBot getBot() { return bot; }

    public void handleListSongsByUser(CommandContext ctx, boolean top) {
        processRequest(ctx.getUserUid(), ctx.getUserName(), ctx.getArgs(), top, true);
    }

    public void handleListGlobalSongs(CommandContext ctx, boolean top) {
        processRequest(null, "del servidor", ctx.getArgs(), top, false);
    }

    private void processRequest(String uid, String name, String args, boolean top, boolean isUser) {
        int limit = parseLimit(args);

        List<PlayStats> stats = isUser
                ? bot.getStatsDao().getTracksByUser(uid, limit, top)
                : bot.getStatsDao().getGlobalTracks(limit, top);

        if (stats.isEmpty()) {
            replyWarning("No hay estadísticas registradas para " + name + ".");
            return;
        }

        String order = top ? "más" : "menos";
        replyListHeader("Top " + order + " escuchados " + (isUser ? "de " : "") + name);

        replyList(formatStatsToStrings(stats), limit);
    }

    private int parseLimit(String arg) {
        try {
            int val = arg.isEmpty() ? 5 : Integer.parseInt(arg.split("\\s+")[0]);
            return Math.min(val, 20);
        } catch (Exception e) { return 5; }
    }

    private List<String> formatStatsToStrings(List<PlayStats> stats) {
        return stats.stream()
                .map(s -> s.getTrack() + " [color=" + C_MUSIC + "][" + s.getPlayCount() + "][/color]")
                .toList();
    }
}