package TS3Bot.commands.services;

import TS3Bot.TeamSpeakBot;
import TS3Bot.model.Track;

import java.util.Arrays;
import java.util.List;

public class PlaybackServices {
    private final TeamSpeakBot bot;

    public PlaybackServices(TeamSpeakBot bot) {
        this.bot = bot;
    }

    public int[] resolveIndices(String[] args) {
        return Arrays.stream(args)
                .mapToInt(s -> Integer.parseInt(s) - 1)
                .toArray();
    }

    public List<Track> getAllTracks() {
        return bot.getTrackDao().getAllTracks();
    }
}