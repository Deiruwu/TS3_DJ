package TS3Bot.audio;

import TS3Bot.commands.CommandContext;
import TS3Bot.db.TrackDAO;
import TS3Bot.model.Track;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataClient {
    private static final String CACHE_DIR = "cache";
    private static final String PYTHON_HOST = "127.0.0.1";
    private static final int PYTHON_PORT = 5005;

    private static final TrackDAO dao = new TrackDAO();

    private static volatile Process currentDownloadProcess = null;
    private static volatile String currentDownloadUuid = null;

    // Interfaz de eventos
    public interface DownloadListener {
        void onStart(Track track);                      // Empieza a descargarse
        void onProgress(Track track, double percent);   // Está en proceso de descarga (tiene un valor)
        void onComplete(Track track, File file);        // Descarga completada
        void onError(Track track, Exception e);         // Error durante la descarga
    }

    private static DownloadListener downloadListener;

    public static void setDownloadListener(DownloadListener listener) {
        downloadListener = listener;
    }

    public static Track getMetadata(String query) throws Exception {
        return getMetadata(query, new String[]{});
    }

    public static Track getMetadata(String query, String[] flags) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("action", "metadata");
        request.addProperty("query", query);

        if (flags != null && flags.length > 0) {
            JsonArray flagsArray = new JsonArray();
            for (String flag : flags) {
                flagsArray.add(flag);
            }
            request.add("flags", flagsArray);
        }

        try (Socket socket = new Socket(PYTHON_HOST, PYTHON_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(request.toString());
            String jsonResponse = in.readLine();

            if (jsonResponse == null) {
                throw new Exception("El servicio Python no respondió.");
            }

            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (!response.has("status") || !response.get("status").getAsString().equals("ok")) {
                String message = response.has("message")
                        ? response.get("message").getAsString()
                        : "Error desconocido";
                throw new Exception(message);
            }

            JsonObject data = response.getAsJsonObject("data");

            return new Track(
                    data.get("id").getAsString(),
                    data.get("title").getAsString(),
                    data.get("artist").getAsString()
            )
                    .setAlbum(data.get("album").getAsString())
                    .setThumbnail(data.get("thumbnail").getAsString())
                    .setDuration(data.get("duration").getAsLong());
        }
    }

    public static Track getMetadata(CommandContext ctx) throws Exception {
        if (!ctx.hasArgs()) {
            throw new Exception("Query vacío");
        }

        String[] flags = ctx.hasFlag("raw") ? new String[]{"raw"} : new String[]{};
        return getMetadata(ctx.getArgs(), flags);
    }

    public static void analyze(Track track) throws Exception {
        if (track.isAnalyzed()) {
            System.out.println("[MetadataClient] Track ya analizado: " + track.getTitle());
            return;
        }

        if (track.getPath() == null || !track.isDownloaded()) {
            throw new Exception("No se puede analizar: archivo no existe");
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "analyze");
        request.addProperty("path", track.getPath());

        try (Socket socket = new Socket(PYTHON_HOST, PYTHON_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[MetadataClient] Analizando: " + track.getTitle());

            out.println(request.toString());
            String jsonResponse = in.readLine();

            if (jsonResponse == null) {
                throw new Exception("El servicio Python no respondió.");
            }

            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (!response.has("status") || !response.get("status").getAsString().equals("ok")) {
                String message = response.has("message")
                        ? response.get("message").getAsString()
                        : "Análisis falló";
                throw new Exception(message);
            }

            JsonObject data = response.getAsJsonObject("data");

            track.setBpm(data.get("bpm").getAsInt());
            track.setCamelotKey(data.get("camelotKey").getAsString());

            dao.saveTrack(track);

            System.out.println("[MetadataClient] Análisis completo: BPM=" + track.getBpm() +
                    ", Key=" + track.getCamelotKey());
        }
    }

    public static File downloadCompressed(Track track) throws Exception {
        String uuid = track.getUuid();

        File cacheFolder = new File(CACHE_DIR);
        if (!cacheFolder.exists()) cacheFolder.mkdir();

        // Template de salida para yt-dlp
        String outputTemplate = new File(cacheFolder, uuid + ".%(ext)s").getAbsolutePath();

        // 1. EVENTO START
        if (downloadListener != null) downloadListener.onStart(track);

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

        Pattern progressPattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)%");

        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                fullLog.append(line).append("\n");

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Descarga abortada.");
                }

                // 2. EVENTO PROGRESO
                if (downloadListener != null && line.contains("[download]") && line.contains("%")) {
                    Matcher m = progressPattern.matcher(line);
                    if (m.find()) {
                        try {
                            double percent = Double.parseDouble(m.group(1));
                            downloadListener.onProgress(track, percent);
                        } catch (NumberFormatException ignored) {}
                    }
                    // Imprimimos en consola también por si acaso
                    System.out.print("\r[DL] " + line.trim());
                }
            }
        } catch (IOException e) {
            if (currentDownloadProcess == null) {
                throw new InterruptedException("Matado externamente.");
            }
            // 3. EVENTO ERROR
            if (downloadListener != null) downloadListener.onError(track, e);
            throw e;
        } finally {
            currentDownloadProcess = null;
            currentDownloadUuid = null;
        }

        if (p.waitFor() != 0) {
            Exception e = new Exception("Error fatal en yt-dlp (Exit code " + p.exitValue() + ")");
            if (downloadListener != null) downloadListener.onError(track, e);

            System.err.println("\n[ERROR CRÍTICO YT-DLP]");
            System.err.println(fullLog.toString());
            throw e;
        }

        File[] matches = cacheFolder.listFiles((dir, name) -> name.startsWith(uuid + "."));
        if (matches == null || matches.length == 0) {
            Exception e = new Exception("Archivo no encontrado tras descarga.");
            if (downloadListener != null) downloadListener.onError(track, e);
            throw e;
        }

        File finalFile = matches[0];

        // 4. EVENTO COMPLETADO
        System.out.println("\n[MetadataClient] Descarga finalizada: " + finalFile.getName());
        if (downloadListener != null) {
            downloadListener.onComplete(track, finalFile);
        }

        return finalFile;
    }
    public static boolean cancelCurrentDownload() {
        Process p = currentDownloadProcess;
        String uuid = currentDownloadUuid;

        if (p != null && p.isAlive()) {
            p.destroyForcibly();
            currentDownloadProcess = null;

            System.out.println("\n[MetadataClient] Matando proceso de descarga...");

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