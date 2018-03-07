package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;

@AvailableCommand
public class LinkChannelCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(LinkChannelCommand.class);

    public LinkChannelCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("link", "l"));
    }

    @Override
    protected void execute() {
        if (!ev.getMember().getVoiceState().inVoiceChannel()) {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }

        VoiceChannel vc = ev.getMember().getVoiceState().getChannel();

        if (l.getChannelController().isLinked(vc)) {
            handleError(MessageContent.CHANNEL_ALREADY_LINKED);
        }

        TextChannel tc = ev.getChannel();
        l.getChannelController().getLinkedChannels().putStoring(tc, vc);
        l.getChannelController().getPermChannels().putStoring(tc, vc);

        LoggerUtils.logAndDelete(log, String.format("A new link has been established:\n" +
                        " - VC: %s (%s)\n" +
                        " - TC: %s (%s)",
                vc.getName(), vc.getId(),
                tc.getName(), tc.getId()));
        l.getDiscordController().sendMessage(tc,
                String.format("Successfully linked **%s** to **%s**!", vc.getName(), tc.getAsMention()),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }

    @Override
    protected String getDescription() {
        return "Links a voice and text channel together, thereby displaying a log of joins and leaves.";
    }

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[] { Permission.MANAGE_CHANNEL };
    }

    @Override
    protected String getUsage() {
        return "{}link\n" +
                "    Links the voice channel you are in to the text channel the command was issued from.\n" +
                "    This will make messages appear for joining and leaving the voice channel.";
    }
}
