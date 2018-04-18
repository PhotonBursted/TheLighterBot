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
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

@AvailableCommand
public class TemporaryChannelTopicCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(TemporaryChannelTopicCommand.class);

    public TemporaryChannelTopicCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("tempchan", "tc")
                .addAliasPart("-t", "-topic", "-d", "-desc", "description"));
    }

    @Override
    protected void execute() {
        // Get the channels targeted by the issuer
        VoiceChannel vc = ev.getMember().getVoiceState().getChannel();
        TextChannel tc = l.getChannelController().getLinkedChannels().getForVoiceChannel(vc);

        // If the voice channel wasn't found the user wasn't in one to start with
        if (vc == null) {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
            return;
        }

        // The target should not be the default channel
        if (vc == ev.getGuild().getAfkChannel()) {
            handleError(MessageContent.AFK_CHANNEL_ACTION_NOT_PERMITTED);
            return;
        }

        // If the target voice channel is permanent, the user requires MANAGE_CHANNEL permissions
        if (l.getChannelController().isPermanent(vc) && (!l.getChannelController().isPermanent(vc) || !ev.getMember().hasPermission(Permission.MANAGE_CHANNEL))) {
            handleError(MessageContent.PERMISSIONS_REQUIRED, Permission.MANAGE_CHANNEL.name(), "change the topic of permanent voice channels");
            return;
        }

        String description = Utils.drainQueueToString(input);

        if (tc != null) {
            // Send feedback to the logs and issuer
            LoggerUtils.logAndDelete(log, String.format("Changed description of channel \"%s\" to \"%s\".",
                    tc.getName(), description));
            l.getDiscordController().sendMessage(ev,
                    String.format("**%s** changed the topic%s.",
                            ev.getAuthor().getName(),
                            !tc.equals(ev.getChannel()) ? ("of **" + tc.getAsMention() + "**") : ""),
                    DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
            tc.getManager().setTopic(description).reason(Utils.userAsString(ev.getAuthor()) + " requested a change of topic for this channel.").queue();
        }
    }

    @Override
    protected String getDescription() {
        return "Adjusts the description of a channel without needing special permissions.";
    }

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[] {};
    }

    @Override
    protected String getUsage() {
        return "{}tempchan -t <newDescription>\n" +
                "    <newDescription> specifies the new topic to apply to the linked channel.";
    }
}
