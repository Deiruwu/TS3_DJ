package TS3Bot.commands;

import java.util.*;

public class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();
    private final List<Command> commandList = new ArrayList<>(); // Para mantener orden/lista única

    public void register(Command cmd) {
        commands.put(cmd.getName().toLowerCase(), cmd);
        for (String alias : cmd.getAliases()) {
            commands.put(alias.toLowerCase(), cmd);
        }
        commandList.add(cmd);
    }

    public void executeCommand(String label, CommandContext ctx) {
        Command cmd = commands.get(label.toLowerCase());
        if (cmd != null) {
            cmd.execute(ctx);
        }
    }

    public List<String> generateHelp() {
        List<String> messages = new ArrayList<>();

        final String C_HEADER = "#FF8C00";     // Tu Naranja (Dark Orange)
        final String C_CMD    = "#BD93F9";    // // Púrpura suave para los comandos
        final String C_ARGS   = "#0066cc";    // Azul oscuro profesional para argumentos
        final String C_TEXT   = "#333333";    // Gris muy oscuro (casi negro) para el resto

        // Header más limpio, sin cajas pesadas
        messages.add("[b][color=" + C_CMD + "]=== LISTA DE COMANDOS ===[/color][/b]");

        Map<String, List<Command>> categorized = new LinkedHashMap<>();
        for (Command cmd : commandList) {
            categorized.computeIfAbsent(cmd.getCategory(), k -> new ArrayList<>()).add(cmd);
        }

        for (Map.Entry<String, List<Command>> entry : categorized.entrySet()) {
            // Título de categoría limpio
            // Separador más ligero usando color gris
            messages.add("[color=#999999]───────────────────────────────────────────────[/color]");
            messages.add("[b][color=" + C_HEADER + "]● " + entry.getKey().toUpperCase() + "[/color][/b]");

            for (Command cmd : entry.getValue()) {
                String rawUsage = cmd.getUsage(); // Ej: "!play (!p) <url>"

                String[] parts = rawUsage.split(" ", 2);
                String commandName = parts[0]; // "!play"
                String argsAndAliases = (parts.length > 1) ? parts[1] : ""; // "(!p) <url>"

                // Reconstruimos el string con seguridad
                StringBuilder sb = new StringBuilder();

                // 1. Bullet point gris
                sb.append("[color=#999999]»[/color] ");

                // 2. Comando en Naranja y Negrita (Tu branding)
                sb.append("[b][color=" + C_CMD + "]").append(commandName).append("[/color][/b] ");

                // 3. El resto del texto procesado suavemente
                if (!argsAndAliases.isEmpty()) {
                    // Coloreamos sutilmente los argumentos <...>
                    String styledArgs = argsAndAliases
                            .replaceAll("<(.*?)>", "[color=" + C_ARGS + "]<$1>[/color]")
                            .replaceAll("\\[(.*?)\\]", "[color=#777777][$1][/color]"); // Opcionales en gris

                    sb.append("[color=" + C_TEXT + "]").append(styledArgs).append("[/color]");
                }

                messages.add(sb.toString());
            }
        }

        return messages;
    }

    public int getCommandCount() {
        return commandList.size();
    }
}