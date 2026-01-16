package TS3Bot.commands.utils;

import TS3Bot.TeamSpeakBot;

import java.util.Arrays;

public class PlaybackUtils {
    private final TeamSpeakBot bot;

    public PlaybackUtils(TeamSpeakBot bot) {
        this.bot = bot;
    }

    public int[] resolveIndices(String[] args) {
        return Arrays.stream(args)
                .mapToInt(s -> Integer.parseInt(s) - 1)
                .toArray();
    }
}