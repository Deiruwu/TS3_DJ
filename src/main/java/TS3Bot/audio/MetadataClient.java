package TS3Bot.audio;

import TS3Bot.commands.options.PlayOptions;
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

    private static volatile Process currentDownloadProcess;
    private static volatile String currentDownloadUuid;

    /* ===================== LISTENERS ===================== */

    public interface DownloadListener {
        void onStart(Track track);
        void onProgress(Track track, double percent);
        void onComplete(Track track, File file);
        void onError(Track track, Exception e);
    }

    private static DownloadListener downloadListener;

    public static void setDownloadListener(DownloadListener listener) {
        downloadListener = listener;
    }

    /* ===================== METADATA ===================== */

    /**
     *  Metodo para obtener metadatos sin flags por medios externos,
     */
    public static Track getMetadata(String query) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("action", "metadata");
        request.addProperty("query", query);
        return sendRequest(request);
    }

    /**
     *  Metodo para buscar los metadatos con una canción con flags utilizando comandos
     */
    public static Track getMetadata(String query, PlayOptions options) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("action", "metadata");
        request.addProperty("query", query);

        if (options != null && options.isRaw()) {
            JsonArray flags = new JsonArray();
            flags.add("raw");
            request.add("flags", flags);
        }

        return sendRequest(request);
    }

    private static Track sendRequest(JsonObject request) throws Exception {
        try (
                Socket socket = new Socket(PYTHON_HOST, PYTHON_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(request.toString());

            String jsonResponse = in.readLine();
            if (jsonResponse == null) {
                throw new Exception("El servicio Python no respondió");
            }

            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
            if (!"ok".equals(response.get("status").getAsString())) {
                throw new Exception(response.get("message").getAsString());
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

    /* ===================== ANALYSIS ===================== */

    public static void analyze(Track track) throws Exception {
        if (track.isAnalyzed()) return;

        JsonObject request = new JsonObject();
        request.addProperty("action", "analyze");
        request.addProperty("path", track.getPath());

        Track analyzed = sendRequest(request);
        track.setBpm(analyzed.getBpm());
        track.setCamelotKey(analyzed.getCamelotKey());

        dao.saveTrack(track);
    }

    /* ===================== DOWNLOAD ===================== */

    public static File downloadCompressed(Track track) throws Exception {
        String uuid = track.getUuid();
        currentDownloadUuid = uuid;

        File cacheFolder = new File(CACHE_DIR);
        if (!cacheFolder.exists()) cacheFolder.mkdir();

        String outputTemplate = new File(cacheFolder, uuid + ".%(ext)s").getAbsolutePath();

        if (downloadListener != null) {
            downloadListener.onStart(track);
        }

        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "-x",
                "--extractor-args", "youtube:player_client=android",
                "-r", "2M",
                "-o", outputTemplate,
                "https://www.youtube.com/watch?v=" + uuid
        );

        pb.redirectErrorStream(true);
        currentDownloadProcess = pb.start();

        Pattern progressPattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)%");

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(currentDownloadProcess.getInputStream()))) {

            String line;
            while ((line = r.readLine()) != null) {
                Matcher m = progressPattern.matcher(line);
                if (m.find() && downloadListener != null) {
                    double percent = Double.parseDouble(m.group(1));
                    downloadListener.onProgress(track, percent);
                }
            }

            if (currentDownloadProcess.waitFor() != 0) {
                throw new Exception("yt-dlp falló");
            }

            File[] matches = cacheFolder.listFiles((d, n) -> n.startsWith(uuid));
            if (matches == null || matches.length == 0) {
                throw new Exception("Archivo no encontrado");
            }

            File file = matches[0];

            if (downloadListener != null) {
                downloadListener.onComplete(track, file);
            }

            return file;

        } catch (Exception e) {
            if (downloadListener != null) {
                downloadListener.onError(track, e);
            }
            throw e;

        } finally {
            currentDownloadProcess = null;
            currentDownloadUuid = null;
        }
    }

    public static boolean cancelCurrentDownload() {
        if (currentDownloadProcess != null && currentDownloadProcess.isAlive()) {
            currentDownloadProcess.destroyForcibly();
            currentDownloadProcess = null;
            currentDownloadUuid = null;
            return true;
        }
        return false;
    }
}
