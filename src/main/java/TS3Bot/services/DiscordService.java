package TS3Bot.services;

import com.github.manevolent.ts3j.command.SingleCommand;
import com.github.manevolent.ts3j.command.parameter.CommandSingleParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DiscordService {
    private static DiscordService instance;

    private DiscordService() {
    }

    public static DiscordService getInstance() {
        if (instance == null) {
            instance = new DiscordService();
        }
        return instance;
    }

    private String webhookUrl;
    private String botUsername = "TS3 Bot";
    private final Map<Integer, String> connectedUsers = new ConcurrentHashMap<>();
    private String AVATAR_URL = "https://imgur.com/a/WwaNHU4.png";

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingTask;
    private static final int DELAY_SECONDS = 90;

    public void setConfig(String webhookUrl, String botUsername) {
        this.webhookUrl = webhookUrl;
        if (botUsername != null && !botUsername.isEmpty()) {
            this.botUsername = botUsername;
        }
    }

    public void fetchInitialList(LocalTeamspeakClientSocket client, String botUid) {
        try {
            SingleCommand cmd = new SingleCommand("clientlist", ProtocolRole.CLIENT, new CommandSingleParameter("-uid", ""));
            var response = client.executeCommand(cmd).get();

            this.connectedUsers.clear();

            for (SingleCommand row : response) {
                if (row.has("clid") && row.has("client_nickname") && row.has("client_unique_identifier")) {
                    int type = row.has("client_type") ? Integer.parseInt(row.get("client_type").getValue()) : 0;

                    if (type == 0) {
                        String uid = row.get("client_unique_identifier").getValue();
                        if (!uid.equals(botUid)) {
                            int id = Integer.parseInt(row.get("clid").getValue());
                            String nick = row.get("client_nickname").getValue();
                            this.connectedUsers.put(id, nick);
                        }
                    }
                }
            }
            System.out.println("DiscordService: Lista inicial cargada. Silencioso.");
        } catch (Exception e) {
            System.err.println("[DiscordService] Error fetchInitialList: " + e.getMessage());
        }
    }

    public void onUserJoin(int id, String name) {
        if (this.connectedUsers.containsKey(id)) return;
        this.connectedUsers.put(id, name);
        scheduleUpdate();
    }

    public void onUserLeave(int id) {
        if (this.connectedUsers.remove(id) != null) {
            scheduleUpdate();
        }
    }

    private synchronized void scheduleUpdate() {
        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
            System.out.println("[DiscordService] Cambio detectado. Reiniciando contador de " + DELAY_SECONDS + "s...");
        }

        pendingTask = scheduler.schedule(this::sendUpdateNow, DELAY_SECONDS, TimeUnit.SECONDS);
    }

    // Renombrado a sendUpdateNow para diferenciarlo
// ... dentro de DiscordService.java ...

    private void sendUpdateNow() {
        if (this.webhookUrl == null || this.webhookUrl.isEmpty()) return;

        System.out.println("[DiscordService] Tiempo de espera terminado. Preparando notificación...");

        try {
            List<String> userList = new ArrayList<>(this.connectedUsers.values());

            StringBuilder desc = new StringBuilder();
            if (userList.isEmpty()) {
                desc.append("*No hay nadie conectado...*");
            } else {
                for (String name : userList) {
                    desc.append("- ").append(name).append("\n");
                }
            }

            JsonObject embed = new JsonObject();
            embed.addProperty("title", "Usuarios conectados (" + userList.size() + ")");
            embed.addProperty("description", desc.toString());
            embed.addProperty("color", 15258703);

            JsonArray embeds = new JsonArray();
            embeds.add(embed);

            JsonObject root = new JsonObject();
            root.addProperty("content", "-# Actividad en Teamspeak");
            root.addProperty("username", this.botUsername);
            root.addProperty("avatar_url", this.AVATAR_URL); // Asegúrate de usar this.AVATAR_URL
            root.add("embeds", embeds);

            // --- DEBUG: IMPRIMIR JSON ANTES DE ENVIAR ---
            String jsonString = root.toString();
            System.out.println("[DiscordService] Enviando JSON: " + jsonString);
            // --------------------------------------------

            byte[] payload = jsonString.getBytes(StandardCharsets.UTF_8);

            URL url = new URL(this.webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            // Verificamos la respuesta también por si Discord devuelve error (ej. 400 Bad Request)
            int responseCode = conn.getResponseCode();
            System.out.println("[DiscordService] Respuesta del servidor: " + responseCode);

        } catch (Exception e) {
            System.err.println("[DiscordService] Error envio: " + e.getMessage());
        }
    }

    public void setAvatarUrl(String url) {
        System.out.println("[DiscordService] Avatar URL cambiado a " + url);
        this.AVATAR_URL = url;
    }
}