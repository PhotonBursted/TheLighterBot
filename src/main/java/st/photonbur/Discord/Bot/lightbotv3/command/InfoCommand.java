package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;

import java.awt.*;

@AvailableCommand
public class InfoCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(InfoCommand.class);

    public InfoCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("info", "i"));
    }

    @Override
    protected void execute() {
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
                "Version: `" + Launcher.VERSION + "`\n" +
                "Library: `JDA v" + JDAInfo.VERSION + "`\n" +
                "Connected guilds: `" + l.getBot().getGuildCache().size() + "`", false);

        LoggerUtils.logAndDelete(log, "Showed bot info");
        l.getDiscordController().sendMessage(ev, Color.WHITE, eb,
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }

    @Override
    protected String getDescription() {
        return "Returns information about the bot.";
    }

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[0];
    }

    @Override
    protected String getUsage() {
        return "{}info\n" +
                "    Returns information about the bot.";
    }
}
