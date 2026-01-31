package TS3Bot.model;

import java.util.ArrayList;
import java.util.List;

public class ConnectedUser {
    private final int clientId;
    private final int databaseId;
    private final String uniqueId;
    private String nickname;
    private final List<Integer> serverGroups = new ArrayList<>();

    public ConnectedUser(int clientId, int databaseId, String uniqueId, String nickname, String serverGroups) {
        this.clientId = clientId;
        this.databaseId = databaseId;
        this.uniqueId = uniqueId;
        this.nickname = nickname;

        if (serverGroups != null) {
            for (String group : serverGroups.split(",")) {
                this.serverGroups.add(Integer.parseInt(group));
            }
        }
    }

    public boolean hasGroupId(int groupId){
        if (serverGroups.isEmpty()) return false;
        return serverGroups.contains(groupId);
    }

    public int getClientId() { return clientId; }
    public int getDatabaseId() { return databaseId; }
    public String getUniqueId() { return uniqueId; }
    public String getNickname() { return nickname; }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return nickname;
    }
}