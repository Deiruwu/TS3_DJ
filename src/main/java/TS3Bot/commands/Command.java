package TS3Bot.commands;

import TS3Bot.TeamSpeakBot;
import TS3Bot.interfaces.Replyable;

public abstract class Command implements Replyable {
    protected final TeamSpeakBot bot;

    private static final String C_ACTION  = "Orange";    // Acciones, procesos, advertencias (Lo que te gustó)
    private static final String C_SUCCESS = "Lime";      // Éxito brillante
    private static final String C_ERROR   = "Red";       // Error crítico
    private static final String C_INFO    = "#BD93F9";   // Púrpura suave (Estilo Drácula/Gótico) para textos generales
    private static final String C_TITLE   = "DeepPink";  // Para títulos de listas o cabeceras
    private static final String C_ACCENT    = "Purple";

    public Command(TeamSpeakBot bot) {
        this.bot = bot;
    }

    @Override
    public TeamSpeakBot getBot() {
        return bot;
    }

    public abstract String getName();
    public abstract String[] getAliases();
    public abstract String getUsage();
    public abstract String getCategory();
    public abstract String getDescription();

    public abstract void execute(CommandContext ctx);

    public String getStrAliases() {
        if (getAliases().length == 0) return "";
        String aliases = String.join(", ", getAliases());

        return " (".concat(aliases).concat(")");
    }

    protected void replyUsage() {
        reply("Usa: ".concat(formatUsage()));
    }

    protected String formatUsage() {
        String cmdUsageStr = this.getUsage();

        final String C_CMD  = "#BD93F9";
        final String C_ARGS = "#777777";
        final String C_TEXT = "#333333";

        String[] parts = cmdUsageStr.split(" ", 2);
        String commandName = parts[0];
        String rest = (parts.length > 1) ? parts[1] : "";

        StringBuilder sb = new StringBuilder();

        // Nombre del comando
        sb.append("[b][color=").append(C_CMD).append("]")
                .append(commandName)
                .append("[/color][/b]");

        if (!rest.isEmpty()) {

            String aliases = "";
            String after = rest;

            if (rest.startsWith("(")) {
                int end = rest.indexOf(")");
                if (end != -1) {
                    aliases = rest.substring(0, end + 1);
                    after = rest.substring(end + 1).trim();
                }
            }

            if (!aliases.isEmpty()) {
                sb.append(" ").append(aliases);
            }

            if (!after.isEmpty()) {
                String styled = after.replaceAll(
                        "<(.*?)>",
                        "[color=" + C_ARGS + "]<$1>[/color]"
                );

                sb.append(" [color=").append(C_TEXT).append("]")
                        .append(styled)
                        .append("[/color]");
            }
        }

        return sb.toString();
    }
}