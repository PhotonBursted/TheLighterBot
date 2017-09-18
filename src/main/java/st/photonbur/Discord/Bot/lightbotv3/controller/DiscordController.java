package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

/**
 * This class is in charge of everything related to interaction with Discord.
 */
public class DiscordController {
    /**
     * The prefix to look for when receiving messages
     */
    private static String commandPrefix;
    /**
     * Instance of the launcher for easy reference to other classes
     */
    private final Launcher l;
    /**
     * The bot's token to connect with
     */
    private final String token;
    /**
     * The instance of the bot
     */
    private JDA bot;

    public static final long AUTOMATIC_REMOVAL_INTERVAL = 60L;
    private static DiscordController instance;

    /**
     * Constructs a new interface with Discord
     *
     * @param l             A reference to the main Launcher class
     * @param token         The token the bot uses to connect with Discord
     * @param commandPrefix The prefix to look for when receiving messages and in identifying whether something was a command or not
     */
    private DiscordController(Launcher l, String token, String commandPrefix) {
        this.token = token;
        DiscordController.commandPrefix = commandPrefix;
        this.l = l;

        // Start up and log in
        start();
    }

    public static synchronized DiscordController getInstance() {
        return getInstance(null, null, null);
    }

    public static synchronized DiscordController getInstance(Launcher l, String token, String commandPrefix) {
        if (instance == null && !(l == null || token == null || commandPrefix == null)) {
            instance = new DiscordController(l, token, commandPrefix);
        }

        return instance;
    }

    /**
     * @return The instance of the Discord interface
     */
    public JDA getBot() {
        return bot;
    }

    /**
     * @return The prefix to look for when receiving messages and in identifying whether something was a command or not
     */
    public String getCommandPrefix() {
        return commandPrefix;
    }

    /**
     * Sends a message to Discord in the form of an embed.
     * @param e              The event to respond to
     * @param color          The color to give to the embed
     * @param embedPrototype The rest of the embed in the form of a builder
     *
     * @see EmbedBuilder
     */
    public void sendMessage(GuildMessageReceivedEvent e, Color color, EmbedBuilder embedPrototype) {
        e.getMessage().getChannel().sendMessage(embedPrototype
                .setColor(color)
                .setFooter("Result of " + e.getMessage().getRawContent(), null)
                .setTimestamp(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam")).withZoneSameInstant(ZoneOffset.UTC))
                .build()
        ).complete();
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param e   The event to respond to
     * @param msg The message to display within the message
     *
     * @see MessageContent
     */
    public void sendMessage(GuildMessageReceivedEvent e, MessageContent msg) {
        sendMessage(e, msg, 0);
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param e                     The event to respond to
     * @param msg                   The message to display within the message
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     *
     * @see MessageContent
     */
    public void sendMessage(GuildMessageReceivedEvent e, MessageContent msg, long secondsBeforeDeletion) {
        sendMessage(e, msg.getMessage(), secondsBeforeDeletion);
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param e   The event to respond to
     * @param s   The string to display within the message
     */
    public Message sendMessage(GuildMessageReceivedEvent e, String s) {
        return sendMessage(e, s, 0);
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param e                     The event to respond to
     * @param s                     The string to display within the message
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     */
    public Message sendMessage(GuildMessageReceivedEvent e, String s, long secondsBeforeDeletion) {
        Message msg = e.getMessage().getChannel().sendMessage(s).complete();

        if (secondsBeforeDeletion > 0) {
            msg.delete().queueAfter(secondsBeforeDeletion, TimeUnit.SECONDS);
        }

        return msg;
    }

    /**
     * Starts up a connection with Discord, initializing an instance of the JDA library.
     * This also connects the CommandParser so that messages can be caught and interpreted for commands.
     *
     * @see CommandParser
     */
    private void start() {
        Logger.log("Logging in to Discord...");

        try {
            bot = new JDABuilder(AccountType.BOT)
                    .setToken(token)
                    .addEventListener(l.getCommandParser(), l.getChannelController(), l.getLogger())
                    .buildBlocking();

        } catch (LoginException | InterruptedException | RateLimitedException ex) {
            ex.printStackTrace();
        }
    }
}
