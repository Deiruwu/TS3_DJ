package TS3Bot.audio;

import TS3Bot.db.MusicDAO;
import java.io.*;

public class YouTubeHelper {

    private static final String CACHE_DIR = "cache";
    private static final MusicDAO dao = new MusicDAO();

    public static class MusicEntry {
        public String uuid, titulo, ruta;
        public MusicEntry(String uuid, String titulo, String ruta) {
            this.uuid = uuid; this.titulo = titulo; this.ruta = ruta;
        }
    }

    public static MusicEntry processRequest(String query, boolean silent) throws Exception {
        File cacheFolder = new File(CACHE_DIR);
        if (!cacheFolder.exists()) cacheFolder.mkdir();

        String uuid = null;

        // VIA RÁPIDA: Si es un link, extraemos el ID manualmente sin llamar a yt-dlp
        if (query.contains("youtube.com/watch?v=")) {
            uuid = query.split("v=")[1].split("&")[0];
        } else if (query.contains("youtu.be/")) {
            uuid = query.split(".be/")[1].split("\\?")[0];
        }

        // Si ya tenemos el UUID, miramos la DB antes de hablar con YouTube
        if (uuid != null) {
            String rutaExistente = dao.getRutaPorUuid(uuid);
            if (rutaExistente != null && new File(rutaExistente).exists()) {
                // No imprimimos nada de "buscando", retornamos de inmediato
                return new MusicEntry(uuid, "Caché", rutaExistente);
            }
        }

        // VIA LENTA: Si no es link o no está en DB, ahí sí usamos yt-dlp
        if (!silent) System.out.println("[YT] Consultando información...");

        String[] info = getVideoInfo(query);
        uuid = info[0];
        String titulo = info[1];

        String rutaExistente = dao.getRutaPorUuid(uuid);
        if (rutaExistente != null && new File(rutaExistente).exists()) {
            return new MusicEntry(uuid, titulo, rutaExistente);
        }

        // Solo aquí avisamos que estamos trabajando duro
        // Este mensaje lo lanzaremos desde el Bot, no desde aquí.
        String rutaFinal = downloadSingleStep(uuid);
        dao.insertarCancion(uuid, titulo, "Desconocido", "Single", rutaFinal);

        return new MusicEntry(uuid, titulo, rutaFinal);
    }

    private static String[] getVideoInfo(String query) throws Exception {
        // yt-dlp detecta automáticamente si query es una URL o texto
        ProcessBuilder pb = new ProcessBuilder("yt-dlp",
                "--get-id", "--get-title",
                "--no-playlist", // Por ahora manejamos canciones individuales
                "--extractor-args", "youtube:player_client=android",
                (query.startsWith("http") ? query : "ytsearch1:" + query));

        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String title = r.readLine();
            String id = r.readLine();

            if (id == null || title == null) throw new Exception("No se encontró contenido.");
            return new String[]{id, title};
        }
    }

    private static String downloadSingleStep(String uuid) throws Exception {
        String finalFile = CACHE_DIR + "/" + uuid + ".wav";

        // El comando mágico de tu amigo integrado:
        // -af loudnorm aplica el filtro de normalización durante la creación del wav
        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "-x",
                "--audio-format", "wav",
                "--extractor-args", "youtube:player_client=android",
                "--postprocessor-args", "ffmpeg:-ar 48000 -ac 2 -af loudnorm=I=-16:TP=-1.5:LRA=11",
                "-o", finalFile,
                "https://www.youtube.com/watch?v=" + uuid
        );

        pb.redirectErrorStream(true);
        Process p = pb.start();

        // Opcional: imprimir el progreso en consola
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains("[download]") || line.contains("[ExtractAudio]")) {
                    System.out.println("\u001B[36m[yt-dlp]\u001B[0m " + line);
                }
            }
        }

        if (p.waitFor() != 0) throw new Exception("La descarga o normalización falló.");

        return finalFile;
    }
}