package TS3Bot.audio;

import TS3Bot.model.Track;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalysisScheduler {
    private static final ExecutorService analysisQueue = Executors.newSingleThreadExecutor();

    /**
     * Mete el track en la cola. No bloquea el hilo principal.
     * El bot sigue funcionando mientras esto trabaja en el fondo.
     */
    public static void submit(Track track) {
        if (track.getBpm() > 0 && track.getCamelotKey() != null) {
            return;
        }

        analysisQueue.submit(() -> {
            try {
                Thread.sleep(1000);

                System.out.println("[AnalysisQueue] Iniciando análisis para: " + track.getTitle());

                MetadataClient.analyze(track);

            } catch (Exception e) {
                System.err.println("[AnalysisQueue] Error analizando " + track.getTitle() + ": " + e.getMessage());
            }
        });
    }

    public static void shutdown() {
        analysisQueue.shutdown();
    }
}