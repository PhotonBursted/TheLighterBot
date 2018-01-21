package st.photonbur.Discord.Bot.lightbotv3.main;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Custom Logger implementation for use throughout the project
 */
public class LoggerUtils extends ListenerAdapter {
    /**
     * Retains the ID of the last message deleted by the logger.
     * It is used to verify if a message marked for deletion should really be deleted or not.
     *
     * @see Message#getId()
     */
    private static String lastDeletedMessageId = "";

    /**
     * Besides logging, this method will also grab Discord's received message events as parsed by the {@link st.photonbur.Discord.Bot.lightbotv3.command.CommandParser CommandParser}.
     * If viable, it will delete the message and put the author and source of the sent message in the log.
     *
     * @param msg The string to log
     * @see GuildMessageReceivedEvent
     * @see CommandParser#getLastEvent
     */
    public static void logAndDelete(Logger log, String msg) {
        logAndDelete(log, msg, null);
    }

    /**
     * Besides logging, this method will also grab Discord's received message events as parsed by the {@link st.photonbur.Discord.Bot.lightbotv3.command.CommandParser CommandParser}.
     * If viable, it will delete the message and put the author and source of the sent message in the log.
     *
     * @param msg              The string to log
     * @param successOperation The action to execute if the message deletes appropriately
     * @see GuildMessageReceivedEvent
     * @see CommandParser#getLastEvent
     */
    public static void logAndDelete(Logger log, String msg, Consumer<Void> successOperation) {
        // Get the last event parsed by the command parser
        GuildMessageReceivedEvent ev = CommandParser.getLastEvent();

        // If the fetched event hasn't yet been deleted by the LoggerUtils, print a detailed message. Otherwise, just log it
        if (!lastDeletedMessageId.equals(ev.getMessageId())) {
            // Log a detailed message including author and source
            log.info(String.format("%s\n" +
                            " - Author: %s#%s (%s)\n" +
                            " - Source: %s",
                    msg,
                    ev.getAuthor().getName(), ev.getAuthor().getDiscriminator(), ev.getAuthor().getId(),
                    ev.getChannel().getName()));

            // Delete the message if permissions allow it
            if (ev.getGuild().getSelfMember().hasPermission(ev.getChannel(), Permission.MESSAGE_MANAGE)) {
                ev.getMessage().delete().queue(successOperation);
            } else {
                successOperation.accept(null);
            }

            // Mark the event as last handled
            lastDeletedMessageId = ev.getMessageId();
        } else {
            log.info(msg);
        }
    }
}
