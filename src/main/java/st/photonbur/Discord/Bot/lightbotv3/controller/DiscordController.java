package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public class DiscordController {
    private final Launcher l;
    private final String token;
    private static String commandPrefix;
    private JDA bot;

    public DiscordController(Launcher l, String token, String commandPrefix) {
        this.token = token;
        DiscordController.commandPrefix = commandPrefix;
        this.l = l;

        start();
    }

    public JDA getBot() {
        return bot;
    }

    public static String getCommandPrefix() {
        return commandPrefix;
    }

    public static void sendMessage(GuildMessageReceivedEvent e, Color color, EmbedBuilder embedPrototype) {
        e.getMessage().getChannel().sendMessage(embedPrototype
                .setColor(color)
                .setFooter("Result of " + e.getMessage().getRawContent(), null)
                .setTimestamp(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam")).withZoneSameInstant(ZoneOffset.UTC))
                .build()
        ).complete();
    }

    public static void sendMessage(GuildMessageReceivedEvent e, st.photonbur.Discord.Bot.lightbotv3.entity.Message msg) {
        sendMessage(e, msg, 0);
    }

    public static void sendMessage(GuildMessageReceivedEvent e, st.photonbur.Discord.Bot.lightbotv3.entity.Message msg, long secondsBeforeDeletion) {
        sendMessage(e, msg.getMessage(), secondsBeforeDeletion);
    }

    public static Message sendMessage(GuildMessageReceivedEvent e, String s) {
        return sendMessage(e, s, 0);
    }

    public static Message sendMessage(GuildMessageReceivedEvent e, String s, long secondsBeforeDeletion) {
        Message msg = e.getMessage().getChannel().sendMessage(s).complete();

        if (secondsBeforeDeletion > 0) {
            msg.delete().queueAfter(secondsBeforeDeletion, TimeUnit.SECONDS);
        }

        return msg;
    }

    private void start() {
        try {
            bot = new JDABuilder(AccountType.BOT)
                    .setToken(token)
                    .addEventListener(new CommandParser())
                    .buildBlocking();
        } catch (LoginException | InterruptedException | RateLimitedException ex) {
            ex.printStackTrace();
        }
    }
}
