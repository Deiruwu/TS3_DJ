package TS3Bot.commands;

import TS3Bot.TeamSpeakBot;

import java.util.List;

public abstract class Command {
    protected final TeamSpeakBot bot;

    public Command(TeamSpeakBot bot) {
        this.bot = bot;
    }

    public abstract String getName();
    public abstract String[] getAliases();
    public abstract String getUsage();
    public abstract String getCategory();
    public abstract String getDescription();

    public abstract void execute(CommandContext ctx);

    public String getStrAliases() {
        if (getAliases().length == 0) return "";
        String aliases = String.join(", ", getAliases());

        return " (".concat(aliases).concat(")");
    }

    protected void reply(String message) {
        bot.reply(message);
    }

    protected void replyImportant(String message) {
        reply("[color=DeepPink][b]Action:[/b][/color] " + message);
    }

    protected void replyList(String tittle, List<String> list) {
        if (list.isEmpty()) return;

        reply("[color=purple][b]" + tittle + "[/b][/color]");
        for (int i = 0; i < list.size(); i++) {
            reply("[b][color=purple]\t#" + (i + 1) + ".  [/color][/b]" + list.get(i));
        }
    }

    protected void replyError(String message) {
        reply("[color=red]" + message + "[/color]");
    }
}