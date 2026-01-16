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

    default void replyList(List<String> list) {
        if (list == null || list.isEmpty()) return;
        for (int i = 0; i < list.size(); i++) {
            reply("[b][color=" + C_ACCENT + "]\t#" + (i + 1) + ".  [/color][/b]" + list.get(i));
        }
    }

    default void replyImportant(String message) {
        reply("[color=" + C_TITLE + "][b]Action:[/b][/color] " + message);
    }
}