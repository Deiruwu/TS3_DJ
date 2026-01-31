package TS3Bot.commands.options;

import TS3Bot.commands.CommandContext;

public class PlayOptions {

    private boolean raw;

    private PlayOptions() {
    }

    public static PlayOptions defaults() {
        return new PlayOptions();
    }

    public static PlayOptions fromContext(CommandContext ctx) {
        PlayOptions options = new PlayOptions();
        options.raw = ctx.hasFlag("raw");
        return options;
    }

    public boolean isRaw() {
        return raw;
    }
}
