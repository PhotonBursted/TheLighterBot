package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;

import java.awt.*;

public class InfoCommand extends Command {
    @Override
    void execute() throws RateLimitedException {
        EmbedBuilder eb = new EmbedBuilder();
        eb.addField("General information",
                "Name: `TheLighterBot`\n" +
                "Description: `A temporary channel service inspired by TeamSpeak's temporary channels`\n" +
                "\n" +
                "Author: `PhotonBurst#6983`\n" +
                "Website: `https://github.com/PhotonBursted/TheLighterBot`\n" +
                "Invite: `http://photonbur.st/auth/discord/thelighterbot`\n" +
                "Discord server: `https://discord.gg/sU4pzV4`", false)
        .addField("Bot-specific information",
                "Version: `3.0.0`\n" +
                "Library: `JDA v" + JDAInfo.VERSION + "`\n" +
                "Connected guilds: `" + l.getBot().getGuildCache().size() + "`", false);

        Logger.logAndDelete("Showed bot info");
        l.getDiscordController().sendMessage(ev, Color.WHITE, eb);
    }

    @Override
    String[] getAliases() {
        return new String[] { "info" };
    }

    @Override
    String getDescription() {
        return "Returns information about the bot.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[0];
    }

    @Override
    String getUsage() {
        return "{}info\n" +
                "    Returns information about the bot.";
    }
}
