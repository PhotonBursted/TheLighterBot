package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

abstract class Command {
    ArrayDeque<String> input;

    abstract void execute();

    abstract Set<String> getAliases();

    abstract String getDescription();

    abstract List<Permission> getPermissionsRequired();

    abstract String getUsage();

    boolean messageIsCommand() {
        return messageIsCommand("");
    }

    boolean messageIsCommand(String query) {
        return messageIsCommand(query, input);
    }

    boolean messageIsCommand(ArrayDeque<String> input) {
        return messageIsCommand("", input);
    }

    // Method checking if the string is part of a command, trimming up the message in the process
    private boolean messageIsCommand(String query, ArrayDeque<String> input) {
        boolean success;

        // Checks if the input complies with being a command or argument
        if (query.equals("")) {
            success = this.getAliases().stream().anyMatch(alias -> alias.equals(input.getFirst()));
        } else {
            success = input.getFirst().equals(DiscordController.getCommandPrefix() + query);
        }

        // If so, cut the first part of the input off
        if (success) {
            input.pop();
        }

        return success;
    }

    Command setInput(ArrayDeque<String> args) {
        this.input = args;
        return this;
    }
}
