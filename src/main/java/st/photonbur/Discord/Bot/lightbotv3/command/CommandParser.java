package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.Message;

import java.util.concurrent.TimeUnit;

public class CommandParser extends ListenerAdapter {
    private static GuildMessageReceivedEvent lastEvent;

    public static GuildMessageReceivedEvent getLastEvent() {
        return lastEvent;
    }

    private void handleError(GuildMessageReceivedEvent ev, Message msg) {
        handleError(ev, msg.getMessage());
    }

    private void handleError(GuildMessageReceivedEvent ev, String msg) {
        DiscordController.sendMessage(ev, msg, 10);
        ev.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent ev) {
        lastEvent = ev;
    }
}
