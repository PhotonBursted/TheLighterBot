package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Used as a template for commands.
 * These commands can be activated by specific inputs out of messages supplied by Discord.
 */
abstract class Command {
    /**
     * Stores the {@link GuildMessageReceivedEvent event} which caused the command to trigger.
     */
    GuildMessageReceivedEvent ev;
    /**
     * A place to store the input of a user.
     * This input will be condensed out of a message and supplied by a {@link CommandParser CommandParser} if it is targeted at this command.
     */
    LinkedBlockingQueue<String> input;

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
    abstract void execute() throws RateLimitedException;

    /**
     * Called by the {@link CommandParser} to execute the command.
     * This method will also carry out last preparations before actually executing the command.
     */
    void executeCmd() {
        try {
            this.ev = CommandParser.getLastEvent();

            execute();
        } catch (RateLimitedException ex) {
            ex.printStackTrace();
        }
    }

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
    abstract Set<Permission> getPermissionsRequired();

    /**
     * Method to be implemented with an explanation of the usage of this command.
     * @return The explanation of how to use the command
     */
    abstract String getUsage();

    /**
     * Handles any errors that might occur during the handling of the input for a command.
     *
     * @param msg The message to display
     */
    void handleError(MessageContent msg) {
        handleError(msg.getMessage());
    }

    /**
     * Handles any errors that might occur during the handling of the input for a command.
     *
     * @param msg The message to display
     */
    void handleError(MessageContent msg, String... s) {
        handleError(MessageContent.format(msg, s));
    }

    /**
     * Handles any errors that might occur during the handling of the input for a command.
     *
     * @param msg The message to display
     */
    void handleError(String msg) {
        // Send a message indicating the error
        DiscordController.sendMessage(CommandParser.getLastEvent(), msg, 10);
        // Delete both messages after 10 seconds
        CommandParser.getLastEvent().getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
    }

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
    boolean messageIsCommand(LinkedBlockingQueue<String> input) {
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
    private boolean messageIsCommand(String query, LinkedBlockingQueue<String> input) {
        boolean success;

        // Checks if the input complies with being a command or argument
        if (query.equals("")) {
            // Generate the full input from the input.
            // Don't use the original input as this operation clears all other input from the queue as well.
            String inputStr = Utils.drainQueueToString(new LinkedBlockingQueue<>(input));
            // Try to find an alias that complies with the start of the input sequence
            success = this.getAliases().stream().anyMatch(alias -> inputStr.toLowerCase().startsWith(DiscordController.getCommandPrefix() + alias.toLowerCase()));

            // If so, cut the first part of the input off
            if (success) {
                for (int i = 0; i < this.getAliases().iterator().next().split("\\s+").length; i++) {
                    input.poll();
                }
            }
        } else {
            success = input.peek().equalsIgnoreCase(DiscordController.getCommandPrefix() + query);

            // If so, cut the first part of the input off
            if (success) {
                input.poll();
            }
        }

        return success;
    }

    /**
     * Sets the input the command can execute with
     *
     * @param args The arguments the user specified in their message
     * @return This instance (for chaining purposes)
     */
    Command prepareWithInput(LinkedBlockingQueue<String> args) {
        this.input = args;
        return this;
    }
}
