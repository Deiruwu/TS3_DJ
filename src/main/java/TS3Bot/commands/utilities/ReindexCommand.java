package TS3Bot.commands.utilities;

import TS3Bot.TeamSpeakBot;
import TS3Bot.audio.AnalysisScheduler;
import TS3Bot.audio.validation.AutoTrackValidator;
import TS3Bot.commands.AsyncCommand;
import TS3Bot.commands.CommandContext;
import TS3Bot.model.Track;

import java.io.File;
import java.util.List;

public class ReindexCommand extends AsyncCommand {
    private final AutoTrackValidator metadataValidator;

    public ReindexCommand(TeamSpeakBot bot) {
        super(bot);
        this.metadataValidator = new AutoTrackValidator();
    }

    @Override
    public String getName() {
        return "!reindex";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!fixdb"};
    }

    @Override
    public String getUsage() {
        return getName();
    }

    @Override
    public String getCategory() {
        return "Utilities";
    }

    @Override
    public String getDescription() {
        return "Revalida metadatos y encola analisis de audio";
    }

    @Override
    public void executeAsync(CommandContext ctx) {
        replyInfo("Iniciando reindexado masivo...");

        List<Track> allTracks = bot.getTrackDao().getAllTracks();

        if (allTracks.isEmpty()) {
            replyWarning("La base de datos esta vacia.");
            return;
        }

        int total = allTracks.size();
        int missingFile = 0;
        int repairedMetadata = 0;
        int queuedForAnalysis = 0;
        int perfectlyFine = 0;

        for (Track track : allTracks) {
            boolean wasModified = false;

            File file = track.getFile();
            if (file == null || !file.exists()) {
                missingFile++;
                continue;
            }

            try {
                if (metadataValidator.validateAndRepair(track)) {
                    bot.getTrackDao().saveTrack(track);
                    repairedMetadata++;
                    wasModified = true;
                }
            } catch (Exception e) {
                System.err.println("Error validando metadata: " + e.getMessage());
            }

            if (!track.isAnalyzed()) {
                AnalysisScheduler.submit(track);
                queuedForAnalysis++;
                wasModified = true;
            }

            if (!wasModified) {
                perfectlyFine++;
            }
        }

        replySuccess("Reindexado finalizado. Total: " + total);
        replyInfo("Perfectos: " + perfectlyFine);

        if (repairedMetadata > 0) {
            replyInfo("Metadatos Reparados: " + repairedMetadata);
        }

        if (queuedForAnalysis > 0) {
            replyInfo("Encolados para Analisis: " + queuedForAnalysis);
        }

        if (missingFile > 0) {
            replyWarning("Archivos Perdidos: " + missingFile);
        }
    }
}