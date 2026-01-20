package TS3Bot.services;

import TS3Bot.model.ConnectedUser;
import com.github.manevolent.ts3j.event.ClientJoinEvent;
import com.github.manevolent.ts3j.event.ClientLeaveEvent;
import com.github.manevolent.ts3j.event.ClientUpdatedEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserStateManager {
    private final Map<Integer, ConnectedUser> users = new ConcurrentHashMap<>();

    private int botClientId = -1;

    public void setBotClientId(int id) {
        this.botClientId = id;
    }

    public void addUser(ClientJoinEvent e) {
        users.put(e.getClientId(), new ConnectedUser(
                e.getClientId(), e.getClientDatabaseId(),
                e.getUniqueClientIdentifier(), e.getClientNickname()
        ));
    }

    public void removeUser(ClientLeaveEvent e) {
        users.remove(e.getClientId());
    }

    public boolean updateUser(ClientUpdatedEvent e) {
        ConnectedUser user = users.get(e.getClientId());
        // (Si el bot cambia su propio nick a "Bot - Cancion", no queremos spamear a Discord)
        if (user != null && e.getMap().containsKey("client_nickname")) {
            user.setNickname(e.getMap().get("client_nickname"));

            return e.getClientId() != botClientId;
        }
        return false;
    }

    public void clearAndSet(Map<Integer, String> rawMap) {
        users.clear();
        rawMap.forEach((id, nick) ->
                users.put(id, new ConnectedUser(id, 0, "unknown", nick))
        );
    }

    public List<String> getUserNames() {
        return users.values().stream()
                .filter(u -> u.getClientId() != botClientId)
                .map(ConnectedUser::getNickname)
                .collect(Collectors.toList());
    }
}