package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This class is in charge of everything related to interaction with Discord.
 */
@SuppressWarnings("SameParameterValue")
public class DiscordController {
    public static final BiConsumer<Throwable, Message> MESSAGE_ACTION_FAIL = (throwable, message) -> {
        if (!throwable.getMessage().toUpperCase().contains("UNKNOWN MESSAGE")) {
            throwable.printStackTrace();

            message.getChannel().sendMessage("Something went wrong!\n  - " + throwable.getMessage() + "\nContent: " + message.getContentRaw()).queue();
        }
    };

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

    /**
     * As part of the Singleton design pattern, no clones of this instance are permitted.
     *
     * @return nothing
     * @throws CloneNotSupportedException No clones of this instance are permitted
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
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
     * Handles a default message sending situation.
     * This method is supposed to be used whenever a message is succesfully sent to Discord.
     *
     * @param callback              The action to take when the message has been sent
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     * @param sentMsg               The sent message object with which to resolve the callback.
     */
    private void handleCallbackAndDeletion(Consumer<Message> callback, long secondsBeforeDeletion, Message sentMsg) {
        if (callback != null) {
            callback.accept(sentMsg);
        }

        if (secondsBeforeDeletion > 0) {
            sentMsg.delete().queueAfter(secondsBeforeDeletion, TimeUnit.SECONDS, null, error -> DiscordController.MESSAGE_ACTION_FAIL.accept(error, sentMsg));
        }
    }

    /**
     * Sends a message to Discord in the form of an embed.
     * @param e              The event to respond to
     * @param color          The color to give to the embed
     * @param embedPrototype The contents of the embed in the form of a builder
     *
     * @see EmbedBuilder
     */
    public void sendMessage(GuildMessageReceivedEvent e, Color color, EmbedBuilder embedPrototype, long secondsBeforeDeletion) {
        sendMessage(e, embedPrototype
                .setColor(color)
                .setFooter("Result of " + e.getMessage().getContentRaw(), null)
                .setTimestamp(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam")).withZoneSameInstant(ZoneOffset.UTC))
                .build(), secondsBeforeDeletion);
    }

    /**
     * Sends a message to Discord in the form of an embed.
     * @param e                     The event to respond to
     * @param msg                   The embed to send
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     */
    private void sendMessage(GuildMessageReceivedEvent e, MessageEmbed msg, long secondsBeforeDeletion) {
        sendMessage(e, msg, secondsBeforeDeletion, null);
    }

    /**
     * Sends a message to Discord in the form of an embed.
     * @param e                     The event to respond to
     * @param msg                   The embed to send
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     * @param callback              The action to take when the message has been sent
     */
    private void sendMessage(GuildMessageReceivedEvent e, MessageEmbed msg, long secondsBeforeDeletion, Consumer<Message> callback) {
        sendMessage(e.getChannel(), msg, secondsBeforeDeletion, callback);
    }

    /**
     * Sends a message to Discord in the form of an embed.
     * @param c                     The channel to send the message into
     * @param msg                   The embed to send
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     */
    public void sendMessage(MessageChannel c, MessageEmbed msg, long secondsBeforeDeletion) {
        sendMessage(c, msg, secondsBeforeDeletion, null);
    }

    /**
     * Sends a message to Discord in the form of an embed.
     * @param c                     The channel to send the message into
     * @param msg                   The embed to send
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     * @param callback              The action to take when the message has been sent
     */
    private void sendMessage(MessageChannel c, MessageEmbed msg, long secondsBeforeDeletion, Consumer<Message> callback) {
        c.sendMessage(msg).queue(sentMsg -> handleCallbackAndDeletion(callback, secondsBeforeDeletion, sentMsg));
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param e                     The event to respond to
     * @param s                     The string to display within the message
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     */
    public void sendMessage(GuildMessageReceivedEvent e, String s, long secondsBeforeDeletion) {
        sendMessage(e, s, secondsBeforeDeletion, null);
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param e        The event to respond to
     * @param s        The string to display within the message
     * @param callback The action to take when the message has been sent
     */
    public void sendMessage(GuildMessageReceivedEvent e, String s, Consumer<Message> callback) {
        sendMessage(e, s, 0, callback);
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param e                     The event to respond to
     * @param s                     The string to display within the message
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     * @param callback              The action to take when the message has been sent
     */
    private void sendMessage(GuildMessageReceivedEvent e, String s, long secondsBeforeDeletion, Consumer<Message> callback) {
        sendMessage(e.getChannel(), s, secondsBeforeDeletion, callback);
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param c                     The channel to send the message into
     * @param s                     The string to display within the message
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     */
    public void sendMessage(MessageChannel c, String s, long secondsBeforeDeletion) {
        sendMessage(c, s, secondsBeforeDeletion, null);
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param c                     The channel to send the message into
     * @param s                     The string to display within the message
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     * @param callback              The action to take when the message has been sent
     */
    private void sendMessage(MessageChannel c, String s, long secondsBeforeDeletion, Consumer<Message> callback) {
        c.sendMessage(s).queue(sentMsg -> handleCallbackAndDeletion(callback, secondsBeforeDeletion, sentMsg));
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param c                     The channel to send the message into
     * @param msg                   The message to send
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     */
    public void sendMessage(MessageChannel c, Message msg, long secondsBeforeDeletion) {
        sendMessage(c, msg, secondsBeforeDeletion, null);
    }

    /**
     * Sends a message to Discord in the form of a normal text message.
     * @param c                     The channel to send the message into
     * @param msg                   The message to send
     * @param secondsBeforeDeletion The amount of seconds before the message should be deleted automatically
     * @param callback              The action to take when the message has been sent
     */
    private void sendMessage(MessageChannel c, Message msg, long secondsBeforeDeletion, Consumer<Message> callback) {
        c.sendMessage(msg).queue(sentMsg -> handleCallbackAndDeletion(callback, secondsBeforeDeletion, sentMsg));
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

        } catch (LoginException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
