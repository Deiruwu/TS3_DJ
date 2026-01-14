package TS3Bot.commands;

import TS3Bot.TeamSpeakBot;

public abstract class AsyncCommand extends Command {

    public AsyncCommand(TeamSpeakBot bot) {
        super(bot);
    }

    public abstract void executeAsync(CommandContext ctx);

    @Override
    public void execute(CommandContext ctx) {
        new Thread(() -> {
            try {
                executeAsync(ctx);
            } catch (Exception e) {
                e.printStackTrace();
                reply("[color=red]Error ejecutando comando: " + e.getMessage() + "[/color]");
            }
        }).start();
    }
}