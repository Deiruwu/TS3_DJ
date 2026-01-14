package TS3Bot.commands.utils;

import TS3Bot.TeamSpeakBot;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.PlayStats;

import java.util.List;

/**
 * Clase de utilidad para manejar la lógica de visualización de estadísticas.
 * Centraliza las consultas de Top/Least tanto globales como por usuario.
 */
public class StatsUtils {

    private final TeamSpeakBot bot;

    public StatsUtils(TeamSpeakBot bot) {
        this.bot = bot;
    }

    public void handleListSongsByUser(CommandContext ctx, boolean top) {
        String arg = ctx.getArgs();
        String userUid = ctx.getUserUid();
        String userName = ctx.getUserName();

        int limit;
        try {
            limit = arg.isEmpty() ? 5 : Integer.parseInt(arg.split("\\s+")[0]);
            if (limit > 20) limit = 20;
        } catch (NumberFormatException e) {
            limit = 5;
        }

        String playOrder = top ? "más" : "menos";
        // Asumiendo que getStatsManager() o getStatsDao() existe en el bot
        List<PlayStats> topSongs = bot.getStatsDao().getSongsByUser(userUid, limit, top);

        if (topSongs.isEmpty()) {
            bot.reply("[color=orange]No hay estadísticas registradas para " + userName + ".[/color]");
            return;
        }

        bot.reply("[color=#ff0080][b]♕ Top " + playOrder + " escuchados de " + userName + " ♕[/b][/color]");
        for (int i = 0; i < topSongs.size(); i++) {
            PlayStats stat = topSongs.get(i);
            bot.reply("[color=darkgreen]\t[b]#" + (i + 1)+ ".[/b][/color] " + stat.getTrack() + " [color=blue][" + stat.getPlayCount() + "][/color]");
        }
    }

    public void handleListGlobalSongs(CommandContext ctx, boolean top) {
        String arg = ctx.getArgs();

        int limit;
        try {
            limit = arg.isEmpty() ? 5 : Integer.parseInt(arg.split("\\s+")[0]);
            if (limit > 20) limit = 20;
        } catch (NumberFormatException e) {
            limit = 5;
        }

        String playOrder = top ? "más" : "menos";
        List<PlayStats> topSongs = bot.getStatsDao().getGlobalSongs(limit, top);

        if (topSongs.isEmpty()) {
            bot.reply("[color=orange]No hay estadísticas globales registradas aún.[/color]");
            return;
        }

        bot.reply("[color=#ff0080][b]♕ Top " + playOrder + " escuchados del servidor ♕[/b][/color]");
        for (int i = 0; i < topSongs.size(); i++) {
            PlayStats stat = topSongs.get(i);
            bot.reply("[color=darkgreen]\t[b]#" + (i + 1)+ "[/b][/color] " + stat.getTrack() + " [color=blue][" + stat.getPlayCount() + "][/color]");
        }
    }
}