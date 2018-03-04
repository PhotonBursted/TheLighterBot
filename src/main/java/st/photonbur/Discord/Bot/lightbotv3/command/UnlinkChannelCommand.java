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

public class UnlinkChannelCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(UnlinkChannelCommand.class);

    public UnlinkChannelCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("unlink", "ul"));
    }

    @Override
    void execute() {
        if (ev.getMember().getVoiceState().inVoiceChannel()) {
            VoiceChannel vc = ev.getMember().getVoiceState().getChannel();

            if (l.getChannelController().isLinked(vc)) {
                TextChannel tc = l.getChannelController().getLinkedChannels().getForVoiceChannel(vc);

                l.getChannelController().getLinkedChannels().remove(vc);
                if (l.getChannelController().isPermanent(vc)) {
                    l.getChannelController().getPermChannels().remove(vc);
                }

                LoggerUtils.logAndDelete(log, String.format("A link has been removed:\n" +
                        " - VC: %s (%s)\n" +
                        " - TC: %s (%s)",
                        vc.getName(), vc.getId(),
                        tc.getName(), tc.getId()));
                l.getDiscordController().sendMessage(ev,
                        String.format("Successfully unlinked **%s** from **%s**!", vc.getName(), tc.getAsMention()),
                        DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
            } else {
                handleError(MessageContent.CHANNEL_NOT_LINKED);
            }
        } else {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }
    }

    @Override
    String getDescription() {
        return "Unlinks a voice and text channel.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] { Permission.MANAGE_CHANNEL };
    }

    @Override
    String getUsage() {
        return "{}unlink\n" +
                "    Unlinks the voice channel you are in to the text channel the command was issued from.\n" +
                "    This will remove the messages appearing when joining and leaving the voice channel.";
    }
}
