package TS3Bot.interfaces;

import TS3Bot.TeamSpeakBot;
import java.util.List;

public interface Replyable {

    TeamSpeakBot getBot();

    // === TU IDENTIDAD VISUAL ===
    String C_PRIMARY   = "#BD93F9";  // Púrpura - Números (#1, #2), markers, inicios
    String C_HEADER    = "#FF8C00";  // Naranja - Títulos, encabezados, nombres de playlists

    // === PALETA VIBRANTE ===
    String C_SUCCESS   = "#00C896";  // Verde esmeralda vibrant - Éxitos
    String C_ERROR     = "#FF3B3B";  // Rojo vibrante - Errores
    String C_WARNING   = "#FFA500";  // Orange - Advertencias
    String C_MUTED     = "#7C7C7C";  // Gray medio - Textos secundarios

    // === CONTEXTOS ESPECÍFICOS ===
    String C_MUSIC     = "#4169E1";  // RoyalBlue - Música añadida
    String C_QUEUE     = "#FFB347";  // Naranja pastel - Cola/siguiente
    String C_PLAYLIST  = "#FF6B9D";  // Rosa coral - Playlists
    String C_HIGHLIGHT = "#FF4500";  // OrangeRed - Énfasis extra fuerte
    String C_CONFIRM   = "#FFC107";  // Amber - Confirmaciones

    default void reply(String message) {
        if (getBot() != null) {
            getBot().reply(message);
        }
    }

    default void replyPoke(int idUser, String msg) {
        getBot().replyPoke(idUser, msg);
    }

    // === MENSAJES DE ESTADO ===

    default void replySuccess(String message) {
        reply("[color=" + C_SUCCESS + "]" + message + "[/color]");
    }

    default void replyError(String message) {
        reply("[color=" + C_ERROR + "]" + message + "[/color]");
    }

    default void replyWarning(String message) {
        reply("[color=" + C_WARNING + "]" + message + "[/color]");
    }

    default void replyInfo(String message) {
        reply("[color=" + C_PRIMARY + "]" + message + "[/color]");
    }

    default void replyAction(String message) {
        reply("[color=" + C_HEADER + "]" + message + "[/color]");
    }

    default void replyAction(String prefix, String message) {
        String pre = prefix.isEmpty() ? "" : "[b]" + prefix + "[/b] ";
        reply("[color=" + C_HEADER + "]" + pre + message + "[/color]");
    }

    default void replyDowload(String track) {
        reply("[color=" + C_HEADER + "][b]Track fuera de la base de datos.[/b] Descargando: [/color]" + track + "...");
    }

    // === MENSAJES DE MÚSICA ===

    default void replyMusicAdded(String track) {
        reply("[color=" + C_MUSIC + "][b]Añadido:[/b][/color] [i]" + track + "[/i]");
    }

    default void replyMusicNext(String track) {
        reply("[color=" + C_MUSIC + "][b]Siguiente:[/b][/color] [i]" + track + "[/i]");
    }

    default void replyNowPlaying(String track) {
        reply("[color=" + C_MUSIC + "][b]Estás escuchando:[/b][/color] [i]" + track + "[/i]");
    }

    default void replyPlayingListener(String track) {
        reply("[color=" + C_MUSIC + "][b]A continuación:[/b][/color] [i]" + track + "[/i]");
    }

    // === MENSAJES DE PLAYLIST ===

    default void replyPlaylistAction(String action, String playlistName) {
        reply("[color=" + C_PLAYLIST + "][b]" + action + "[/b][/color] [color=" + C_HEADER + "][i]" + playlistName + "[/i][/color]");
    }

    // === CONFIRMACIONES ===

    default void replyConfirmation(String action, String target, String extra, int timeout) {
        String targetInfo = (extra != null && !extra.isEmpty()) ? target + " (" + extra + ")" : target;

        // Formato limpio:
        // Acción -> OrangeRed (Highlight)
        // Objeto -> Negrita (Blanco/Default)
        // 'y' y Tiempo -> Amber (Confirm)
        String msg = String.format(
                "¿Estás seguro/a de [color=%s][b]%s[/b][/color] [b]%s[/b]? Escribe [color=%s][b]y[/b][/color] para confirmar. (Timeout: [color=%s][b]%ds[/b][/color])",
                C_HIGHLIGHT,           // Color Acción
                action.toUpperCase(),  // Texto Acción
                targetInfo,            // Texto Objeto
                C_CONFIRM,             // Color 'y'
                C_CONFIRM,             // Color Timeout
                timeout                // Valor Timeout
        );

        reply(msg);
    }

    default void replyCancelled() {
        reply("[color=" + C_MUTED + "]Cancelado[/color]");
    }

    // === LISTAS ===

    default void replyList(List<String> list) {
        replyList(list, 15, false);
    }

    default void replyList(List<String> list, boolean showLatest) {
        replyList(list, 15, showLatest);
    }

    default void replyList(List<String> list, int limit) {
        replyList(list, limit, false);
    }

    default void replyList(List<String> list, int limit, boolean showLatest) {
        if (list == null || list.isEmpty()) return;

        int size = list.size();
        int printCount = Math.min(size, limit);

        if (showLatest) {
            int start = size - printCount;
            if (start > 0) {
                reply("[color=" + C_MUTED + "]\t··· " + start + " anteriores ocultos[/color]");
            }
            for (int i = start; i < size; i++) {
                reply("[color=" + C_PRIMARY + "]\t[b]#" + (i + 1) + ".[/b][/color] " + list.get(i));
            }
        } else {
            for (int i = 0; i < printCount; i++) {
                reply("[color=" + C_PRIMARY + "]\t[b]#" + (i + 1) + ".[/b][/color] " + list.get(i));
            }
            int remaining = size - printCount;
            if (remaining > 0) {
                reply("[color=" + C_MUTED + "]\t··· " + remaining + " más[/color]");
            }
        }
    }

    default void replyListHeader(String title) {
        reply("[color=" + C_HEADER + "][b]═══ " + title + " ═══[/b][/color]");
    }
}