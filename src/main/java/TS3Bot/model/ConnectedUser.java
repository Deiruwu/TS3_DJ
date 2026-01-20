package TS3Bot.model;

public class ConnectedUser {
    private final int clientId;
    private final int databaseId;
    private final String uniqueId;
    private String nickname;

    public ConnectedUser(int clientId, int databaseId, String uniqueId, String nickname) {
        this.clientId = clientId;
        this.databaseId = databaseId;
        this.uniqueId = uniqueId;
        this.nickname = nickname;
    }

    public int getClientId() { return clientId; }
    public int getDatabaseId() { return databaseId; }
    public String getUniqueId() { return uniqueId; }
    public String getNickname() { return nickname; }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}