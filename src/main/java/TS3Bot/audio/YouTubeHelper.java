package TS3Bot.audio;

import TS3Bot.model.Track;
import TS3Bot.services.DiscordService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class YouTubeHelper {
    private static final String CACHE_DIR = "cache";
    private static final String PYTHON_HOST = "127.0.0.1";
    private static final int PYTHON_PORT = 5005;

    // --- VARIABLES DE CONTROL PARA CANCELACIÓN ---
    private static volatile Process currentDownloadProcess = null;
    private static volatile String currentDownloadUuid = null;
    // ---------------------------------------------

    public interface DownloadListener {
        void onDownloadStart(Track track);
    }

    private static DownloadListener downloadListener;

    public static void setDownloadListener(DownloadListener listener) {
        downloadListener = listener;
    }

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
                try { duration = json.get("duration").getAsLong(); } catch (Exception e) {}
            }

            DiscordService discordService = new DiscordService();

            discordService.setAvatarUrl(json.get("thumbnail").getAsString());

            return new Track(
                    json.get("id").getAsString(),
                    json.get("title").getAsString(),
                    json.get("artist").getAsString(),
                    json.get("album").getAsString(),
                    null,
                    duration
            );
        }
    }

    public static File downloadCompressed(Track track) throws Exception {
        String uuid = track.getUuid();

        File cacheFolder = new File(CACHE_DIR);
        if (!cacheFolder.exists()) cacheFolder.mkdir();

        String outputTemplate = new File(cacheFolder, uuid + ".%(ext)s").getAbsolutePath();

        if (downloadListener != null) downloadListener.onDownloadStart(track);

        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "-x",
                "--extractor-args", "youtube:player_client=android",
                "-r", "2M",
                "-o", outputTemplate,
                "https://www.youtube.com/watch?v=" + uuid
        );

        pb.redirectErrorStream(true);

        currentDownloadUuid = uuid;
        Process p = pb.start();
        currentDownloadProcess = p;

        StringBuilder fullLog = new StringBuilder();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                fullLog.append(line).append("\n");

                // Si detectamos que el hilo fue interrumpido (por el CancelCommand), salimos del loop
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Descarga abortada durante la lectura.");
                }

                if(line.contains("[download]") && line.contains("%")) {
                    System.out.print("\r[DL] " + line.trim());
                }
            }
        } catch (IOException e) {
            // Si el proceso muere mientras leemos, el stream se cierra
            if (currentDownloadProcess == null) {
                throw new InterruptedException("Proceso matado externamente.");
            }
            throw e;
        } finally {
            currentDownloadProcess = null;
            currentDownloadUuid = null;
        }

        if (p.waitFor() != 0) {
            // Verificamos si fue una cancelación intencional
            int exitValue = p.exitValue();
            if (exitValue == 143 || exitValue == 137 || exitValue == 1) {
                throw new Exception("Descarga cancelada por el usuario.");
            }

            System.err.println("\n[ERROR CRÍTICO YT-DLP]");
            System.err.println(fullLog.toString());
            throw new Exception("Error fatal en descarga yt-dlp.");
        }

        File[] matches = cacheFolder.listFiles((dir, name) -> name.startsWith(uuid + "."));
        if (matches == null || matches.length == 0) throw new Exception("Archivo perdido tras descarga.");

        return matches[0];
    }

    public static boolean cancelCurrentDownload() {
        Process p = currentDownloadProcess;
        String uuid = currentDownloadUuid;

        if (p != null && p.isAlive()) {
            p.destroyForcibly();
            currentDownloadProcess = null;

            System.out.println("\n[YouTubeHelper] Matando proceso de descarga...");

            if (uuid != null) {
                File cacheFolder = new File(CACHE_DIR);
                File[] leftovers = cacheFolder.listFiles((dir, name) -> name.startsWith(uuid));

                if (leftovers != null) {
                    for (File f : leftovers) {
                        if (f.delete()) {
                            System.out.println("[Cleanup] Residuo borrado: " + f.getName());
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }
}