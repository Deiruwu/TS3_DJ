package TS3Bot.commands;

import TS3Bot.TeamSpeakBot;

import java.util.List;

public class HelpCommand extends Command{
    private final CommandRegistry commandRegistry;

    public HelpCommand(TeamSpeakBot bot) {
        super(bot);
        this.commandRegistry = new CommandRegistry();
    }

    @Override
    public String getName() {
        return "!help";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"!h"};
    }

    @Override
    public String getUsage() {
        return getName().concat(getStrAliases());
    }

    @Override
    public String getCategory() {
        return "informacion";
    }

    @Override
    public String getDescription() {
        return "Comando para mostrar todos los comandos disponibles y sus alias";
    }

    @Override
    public void execute(CommandContext ctx) {
        List<String> comandos = bot.getCommandRegistry().generateHelp();


        for (String cmd : comandos) {
            reply(cmd + "\n");
        }
    }
}
