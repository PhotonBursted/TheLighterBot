package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class CommandParser {
    private static GuildMessageReceivedEvent lastEvent;

    public static GuildMessageReceivedEvent getLastEvent() {
        return lastEvent;
    }
}
