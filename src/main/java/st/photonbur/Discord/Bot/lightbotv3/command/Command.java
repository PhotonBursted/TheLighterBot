package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollection;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.StringUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Used as a template for commands.
 * These commands can be activated by specific inputs out of messages supplied by Discord.
 */
@SuppressWarnings("WeakerAccess")
public abstract class Command {
    private static Logger log = LoggerFactory.getLogger(Command.class);

    /**
     * Stores the {@link GuildMessageReceivedEvent event} which caused the command to trigger.
     */
    protected GuildMessageReceivedEvent ev;
    /**
     * A place to store the input of a user.
     * This input will be condensed out of a message and supplied by a {@link CommandParser CommandParser} if it is targeted at this command.
     */
    protected LinkedBlockingQueue<String> input;

    protected final Launcher l;
    private CommandAliasCollection aliases;
    private static long opId;

    static {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("config.properties"));

            opId = Long.parseLong(props.getProperty("opID"));
            log.info("OP is " + opId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Command(CommandAliasCollectionBuilder aliasCollectionBuilder) {
        this.l = Launcher.getInstance();

        applyAliases(aliasCollectionBuilder);
    }

    private void applyAliases(CommandAliasCollectionBuilder builder) {
        this.aliases = builder.build();
    }

    /**
     * Checks whether a member is actually allowed execution of this command.
     *
     * @param m The member to check the permissions of
     * @return Whether or not the member is allowed to execute this command
     */
    private boolean canBeExecutedBy(Member m) {
        log.info(String.format("%s tried to execute command.%n - OP = %s%n - Permissible = %s",
                Utils.userAsString(m.getUser()),
                m.getUser().getIdLong() == opId ? "y" : "n",
                m.hasPermission(ev.getChannel(), getPermissionsRequired()) ? "y" : "n"));
        return m.getUser().getIdLong() == opId || m.hasPermission(ev.getChannel(), getPermissionsRequired());
    }

    /**
     * Method to be implemented with the behaviour and actions the command should have.
     */
    protected abstract void execute();

    /**
     * Called by the {@link CommandParser} to execute the command.
     * This method will also carry out last preparations before actually executing the command.
     */
    void executeCmd() {
        this.ev = CommandParser.getLastEvent();

        if (l.getAccesslistController().isEffectivelyBlacklisted(ev.getMember())) {
            handleError(MessageContent.BLACKLISTED);
            return;
        }

        if (!canBeExecutedBy(ev.getMember())) {
            handleError(MessageContent.PERMISSIONS_REQUIRED_GENERAL,
                    String.join(", ", Arrays.stream(getPermissionsRequired())
                            .map(Enum::name)
                            .collect(Collectors.toList()))
                            .replaceFirst("(?s)(.*), ", "$1 and "));
            return;
        }

        execute();
    }

    final CommandAliasCollection getAliasCollection() {
        return aliases;
    }

    /**
     * Method to be implemented with the description describing this command.
     * @return The description explaining the intended function
     */
    protected abstract String getDescription();

    /**
     * Method to be implemented with the permissions a user should possess in order to be allowed to execute this command.
     * @return The list of permissions needed to perform the command
     */
    protected abstract Permission[] getPermissionsRequired();

    /**
     * Method to be implemented with an explanation of the usage of this command.
     * @return The explanation of how to use the command
     */
    protected abstract String getUsage();

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
    protected void handleError(String msg) {
        // Send a message indicating the error
        l.getDiscordController().sendMessage(ev, msg, 10);
        // Delete both messages after 10 seconds
        ev.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
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
    private boolean messageIsCommand(String query, LinkedBlockingQueue<String> input) {
        boolean success;

        // Checks if the input complies with being a command or argument
        if (query.equals("")) {
            // Generate the full input from the input.
            // Don't use the original input as this operation clears all other input from the queue as well.
            String inputStr = Utils.drainQueueToString(new LinkedList<>(input)).substring(l.getDiscordController().getCommandPrefix().length());

            // Try to find an alias that complies with the start of the input sequence
            success = this.getAliasCollection().stream().anyMatch(alias -> StringUtils.startsWithIgnoreCase(inputStr, alias + " "));
        } else {
            success = input.peek().equalsIgnoreCase(l.getDiscordController().getCommandPrefix() + query);

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
