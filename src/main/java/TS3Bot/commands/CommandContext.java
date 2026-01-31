package TS3Bot.commands;

import java.util.*;

public class CommandContext {
    private final String args;
    private final String userUid;
    private final int userId;
    private final String userName;
    private final String rawMessage;
    private final Map<String, String> flags;

    public CommandContext(String args, String userUid, int userId,String userName,  String rawMessage, Map<String, String> flags) {
        this.args = args;
        this.userUid = userUid;
        this.userId = userId;
        this.userName = userName;
        this.rawMessage = rawMessage;
        this.flags = flags != null ? flags : new HashMap<>();
    }

    public String getArgs() { return args; }
    public String getUserUid() { return userUid; }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getRawMessage() { return rawMessage; }

    public boolean hasArgs() {
        return args != null && !args.isEmpty();
    }

    public boolean hasFlag(String flag) {
        return flags.containsKey(flag.toLowerCase());
    }

    public boolean hasAnyFlag(String... flagNames) {
        for (String flag : flagNames) {
            if (hasFlag(flag)) return true;
        }
        return false;
    }

    public String[] getArgsArray() {
        return args.split("\\s+");
    }

    public String[] getSplitArgs(int limit) {
        return args.split("\\s+", limit);
    }
}