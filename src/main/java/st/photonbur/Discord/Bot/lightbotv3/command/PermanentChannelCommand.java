package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;

public class PermanentChannelCommand extends Command {
    @Override
    void execute() throws RateLimitedException {
        if (ev.getMember().getVoiceState().inVoiceChannel()) {
            VoiceChannel vc = ev.getMember().getVoiceState().getChannel();

            if (!l.getChannelController().isPermanent(vc)) {
                l.getChannelController().getPermChannels().put(vc, ev.getChannel());

                Logger.logAndDelete(String.format("%s has been made permanent.", vc.getName()));
                l.getDiscordController().sendMessage(ev,
                        String.format("Successfully made **%s** permanent!", vc.getName()),
                        DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
            } else {
                handleError(MessageContent.CHANNEL_ALREADY_PERMANENT);
            }
        } else {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }
    }

    @Override
    String[] getAliases() {
        return new String[] { "perm", "p" };
    }

    @Override
    String getDescription() {
        return "Makes a voice channel permanent.\nThis means the channel will not be deleted, even when it is empty and linked.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] { Permission.MANAGE_CHANNEL };
    }

    @Override
    String getUsage() {
        return "{}perm\n" +
                "    Makes the voice channel the issuer is in permanent.";
    }
}
