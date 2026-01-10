package TS3Bot.audio;

import TS3Bot.model.Track;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Socket;

public class YouTubeHelper {
    private static final String CACHE_DIR = "cache";
    private static final String PYTHON_HOST = "127.0.0.1";
    private static final int PYTHON_PORT = 5005;

    public static Track getMetadataViaSocket(String query) throws Exception {
        try (Socket socket = new Socket(PYTHON_HOST, PYTHON_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(query);
            String jsonResponse = in.readLine();

            if (jsonResponse == null) throw new Exception("El servicio Python no respondió.");

            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (!json.has("status") || !json.get("status").getAsString().equals("ok")) {
                throw new Exception("No encontrado en YT/Spotify.");
            }


            long duration = 0;
            if (json.has("duration")) {
                try {
                    duration = json.get("duration").getAsLong();
                } catch (Exception e) {
                }
            }
            return new Track(
                    json.get("id").getAsString(),
                    json.get("title").getAsString(),
                    json.get("artist").getAsString(),
                    json.has("album") ? json.get("album").getAsString() : "Single",
                    null,
                    duration
            );
        }
    }

    public static File downloadCompressed(Track track) throws Exception {
        String uuid = track.getUuid();


        File cacheFolder = new File(CACHE_DIR);
        if (!cacheFolder.exists()) cacheFolder.mkdir();

        String outputTemplate = new File(cacheFolder, track + ".%(ext)s").getAbsolutePath();

        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "-x",
                "--extractor-args", "youtube:player_client=android",
                "-r", "2M",
                "-o", outputTemplate,
                "https://www.youtube.com/watch?v=" + uuid
        );

        pb.redirectErrorStream(true);
        Process p = pb.start();

        // --- CAMBIO AQUÍ: Guardamos todo el log por si hay error ---
        StringBuilder fullLog = new StringBuilder();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                // Guardamos la línea en memoria
                fullLog.append(line).append("\n");

                // Solo imprimimos progreso si es descarga normal
                if(line.contains("[download]") && line.contains("%")) {
                    System.out.print("\r[DL] " + line.trim());
                }
            }
        }
        System.out.println();

        // Si falla, imprimimos TODO lo que dijo yt-dlp
        if (p.waitFor() != 0) {
            System.err.println("\n[ERROR CRÍTICO YT-DLP]");
            System.err.println(fullLog.toString());
            throw new Exception("Error fatal en descarga yt-dlp (Mira el log arriba).");
        }

        File[] matches = cacheFolder.listFiles((dir, name) -> name.startsWith(track + "."));
        if (matches == null || matches.length == 0) throw new Exception("Archivo perdido tras descarga.");

        return matches[0];
    }
}