package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;

public class UnlinkChannelCommand extends Command {
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

                Logger.logAndDelete(String.format("A link has been removed:\n" +
                        " - VC: %s (%s)\n" +
                        " - TC: %s (%s)",
                        vc.getName(), vc.getId(),
                        tc.getName(), tc.getId()));
                l.getDiscordController().sendMessage(ev,
                        String.format("Successfully unlinked **%s** from **%s**!", vc.getName(), tc.getAsMention()),
                        DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                l.getFileController().saveGuild(ev.getGuild());
            } else {
                handleError(MessageContent.CHANNEL_NOT_LINKED);
            }
        } else {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }
    }

    @Override
    String[] getAliases() {
        return new String[] { "unlink", "ul" };
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
