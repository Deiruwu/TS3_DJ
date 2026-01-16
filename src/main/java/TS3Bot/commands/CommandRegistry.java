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


        // Header más limpio, sin cajas pesadas
        messages.add("[b][color=" + C_CMD + "]=== LISTA DE COMANDOS ===[/color][/b]");

        Map<String, List<Command>> categorized = new LinkedHashMap<>();
        for (Command cmd : commandList) {
            categorized.computeIfAbsent(cmd.getCategory(), k -> new ArrayList<>()).add(cmd);
        }

        for (Map.Entry<String, List<Command>> entry : categorized.entrySet()) {
            // Título de categoría limpio
            messages.add("[color=#999999]───────────────────────────────────────────────[/color]");
            messages.add("[b][color=" + C_HEADER + "]● " + entry.getKey().toUpperCase() + "[/color][/b]");

            for (Command cmd : entry.getValue()) {
                // Reconstruimos el string con seguridad
                StringBuilder sb = new StringBuilder();

                // 1. Bullet point gris
                sb.append("[color=#999999]»[/color] ");

                // 2. Resolvemos el formato del comando con su metodo interno
                sb.append(cmd.formatUsage());

                // 3. Agregamos el String Builder a la lista
                messages.add(String.valueOf(sb));
            }
        }

        return messages;
    }

    public int getCommandCount() {
        return commandList.size();
    }
}