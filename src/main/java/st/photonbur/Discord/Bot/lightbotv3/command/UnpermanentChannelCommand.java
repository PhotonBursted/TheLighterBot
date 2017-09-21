package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;

public class UnpermanentChannelCommand extends Command {
    @Override
    void execute() throws RateLimitedException {
        if (ev.getMember().getVoiceState().inVoiceChannel()) {
            VoiceChannel vc = ev.getMember().getVoiceState().getChannel();

            if (l.getChannelController().isPermanent(vc)) {
                l.getChannelController().getPermChannels().remove(vc, ev.getChannel());

                Logger.logAndDelete(String.format("%s has been made temporary.", vc.getName()));
                l.getDiscordController().sendMessage(ev,
                        String.format("Successfully made **%s** temporary!%s", vc.getName(),
                                l.getChannelController().isLinked(vc) ? "\n__Be aware that leaving the channel empty will now delete the channel!__" : ""),
                        DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                l.getFileController().saveGuild(ev.getGuild());
            } else {
                handleError(MessageContent.CHANNEL_NOT_PERMANENT);
            }
        } else {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }
    }

    @Override
    String[] getAliases() {
        return new String[] { "unperm", "up" };
    }

    @Override
    String getDescription() {
        return "Makes a voice channel temporary.\nThis means the channel will be deleted when empty and linked.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] { Permission.MANAGE_CHANNEL };
    }

    @Override
    String getUsage() {
        return "{}unperm\n" +
                "    Makes the voice channel you are in temporary.\n" +
                "    If the voice channel is linked to a text channel, this means it will be deleted when empty.";
    }
}
