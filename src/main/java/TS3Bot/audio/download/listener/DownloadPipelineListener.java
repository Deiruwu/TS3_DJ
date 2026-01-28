package TS3Bot.audio.download.listener;

import TS3Bot.TeamSpeakBot;
import TS3Bot.audio.AnalysisScheduler;
import TS3Bot.audio.MetadataClient;
import TS3Bot.interfaces.Replyable;
import TS3Bot.model.Track;

import java.io.File;

/**
 * Listener encargado de reaccionar a los eventos de descarga
 * y encadenar el flujo descarga -> análisis.
 */
public class DownloadPipelineListener implements MetadataClient.DownloadListener, Replyable {
    private final TeamSpeakBot bot;
    public DownloadPipelineListener(TeamSpeakBot bot) {
        this.bot = bot;
    }

    @Override
    public TeamSpeakBot getBot() {
        return bot;
    }

    @Override
    public void onStart(Track track) {
        System.out.println("[Download] Iniciando descarga: " + track.getTitle());
        replyDowload(track.toString());
    }

    @Override
    public void onProgress(Track track, double percent) {

    }

    @Override
    public void onComplete(Track track, File file) {
        System.out.println("[Download] Descarga finalizada: " + track.getTitle());
        AnalysisScheduler.submit(track);
    }

    @Override
    public void onError(Track track, Exception e) {
        System.err.println(
                "[Download] Error descargando " + track.getTitle() + ": " + e.getMessage()
        );
    }


}
