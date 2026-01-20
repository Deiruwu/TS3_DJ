package TS3Bot.interfaces;

import TS3Bot.TeamSpeakBot;
import java.util.List;

public interface Replyable {

    TeamSpeakBot getBot();

    String C_ACTION  = "Orange";
    String C_SUCCESS = "Lime";
    String C_ERROR   = "Red";
    String C_INFO    = "#BD93F9";   // Púrpura suave
    String C_TITLE   = "DeepPink";
    String C_ACCENT  = "Purple";


    /**
     * Envío base. Llama al metodo reply() nativo del bot.
     */
    default void reply(String message) {
        if (getBot() != null) {
            getBot().reply(message);
        }
    }

    default void replyAction(String message) {
        replyAction("", message);
    }

    default void replyAction(String prefix, String message) {
        String pre = prefix.isEmpty() ? "" : "[b]" + prefix + "[/b] ";
        reply("[color=" + C_ACTION + "]" + pre + message + "[/color]");
    }

    default void replySuccess(String message) {
        reply("[color=" + C_SUCCESS + "]" + message + "[/color]");
    }

    default void replyError(String message) {
        reply("[color=" + C_ERROR + "][b]ERROR: [/b][/color]" + message);
    }

    default void replyInfo(String message) {
        reply("[color=" + C_INFO + "]" + message + "[/color]");
    }

    // 1. (Por defecto: primeros 15)
    default void replyList(List<String> list) {
        replyList(list, 15, false);
    }

    // 2. (Por defecto: primeros 15 y "Mostrar Recientes")
    default void replyList(List<String> list, boolean showLatest) {
        replyList(list, 15, showLatest);
    }

    // 3. manual (Por defecto: desde el principio)
    default void replyList(List<String> list, int limit) {
        replyList(list, limit, false);
    }

    // 4. (Con opción de "Mostrar Recientes")
    default void replyList(List<String> list, int limit, boolean showLatest) {
        if (list == null || list.isEmpty()) return;

        int size = list.size();
        int printCount = Math.min(size, limit);

        if (showLatest) {
            int start = size - printCount;

            if (start > 0) {
                reply("[i][color=gray]... ocultando los primeros " + start + " elementos.[/color][/i]");
            }

            for (int i = start; i < size; i++) {
                reply("[b][color=" + C_ACCENT + "]\t#" + (i + 1) + ".  [/color][/b]" + list.get(i));
            }

        } else {

            for (int i = 0; i < printCount; i++) {
                reply("[b][color=" + C_ACCENT + "]\t#" + (i + 1) + ".  [/color][/b]" + list.get(i));
            }

            // Avisamos si faltan
            int remaining = size - printCount;
            if (remaining > 0) {
                reply("[i][color=gray]... y " + remaining + " elementos más.[/color][/i]");
            }
        }
    }

    default void replyImportant(String message) {
        reply("[color=" + C_TITLE + "][b]Action:[/b][/color] " + message);
    }
}