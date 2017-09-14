package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

/**
 * Used as a template for commands.
 * These commands can be activated by specific inputs out of messages supplied by Discord.
 */
abstract class Command {
    /**
     * A place to store the input of a user.
     * This input will be condensed out of a message and supplied by a {@link CommandParser CommandParser} if it is targeted at this command.
     */
    ArrayDeque<String> input;

    /**
     * Checks whether a member is actually allowed execution of this command.
     *
     * @param m The member to check the permissions of
     * @return Whether or not the member is allowed to execute this command
     */
    public boolean canBeExecutedBy(Member m) {
        return m.hasPermission(getPermissionsRequired());
    }

    /**
     * Method to be implemented with the behaviour and actions the command should have.
     */
    abstract void execute();

    /**
     * Method to be implemented with aliases of the command.
     * @return The aliases this command possesses
     */
    abstract Set<String> getAliases();

    /**
     * Method to be implemented with the description describing this command.
     * @return The description explaining the intended function
     */
    abstract String getDescription();

    /**
     * Method to be implemented with the permissions a user should possess in order to be allowed to execute this command.
     * @return The list of permissions needed to perform the command
     */
    abstract List<Permission> getPermissionsRequired();

    /**
     * Method to be implemented with an explanation of the usage of this command.
     * @return The explanation of how to use the command
     */
    abstract String getUsage();

    /**
     * Checks whether the first part of the stored input matches the query.
     *
     * @param query The string to match
     * @return Whether or not the input was targeted at this command
     */
    boolean messageIsCommand(String query) {
        return messageIsCommand(query, input);
    }

    /**
     * Checks whether the first part of an input is one of the command's aliases.
     *
     * @param input The input to match from
     * @return Whether or not the input was targeted at this command
     */
    boolean messageIsCommand(ArrayDeque<String> input) {
        return messageIsCommand("", input);
    }

    /**
     * Checks whether the first part of an input matches a query.
     *
     * @param query The string to match
     * @param input The input to match from
     * @return Whether or not the input was targeted at this command
     */
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

    /**
     * Sets the input the command can execute with
     *
     * @param args The arguments the user specified in their message
     * @return This instance (for chaining purposes)
     */
    Command setInput(ArrayDeque<String> args) {
        this.input = args;
        return this;
    }
}
