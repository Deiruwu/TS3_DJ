package TS3Bot.commands;

public class CommandContext {
    private final String args;
    private final String userUid;
    private final String userName;
    private final String rawMessage;

    public CommandContext(String args, String userUid, String userName, String rawMessage) {
        this.args = args;
        this.userUid = userUid;
        this.userName = userName;
        this.rawMessage = rawMessage;
    }

    public String getArgs() { return args; }
    public String getUserUid() { return userUid; }
    public String getUserName() { return userName; }
    public String getRawMessage() { return rawMessage; }

    public boolean hasArgs() {
        return args != null && !args.isEmpty();
    }

    public String[] getArgsArray() {
        return args.split("\\s+");
    }

    // Helper para obtener partes específicas
    public String[] getSplitArgs(int limit) {
        return args.split("\\s+", limit);
    }
}