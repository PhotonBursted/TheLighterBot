package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;

public class LinkChannelCommand extends Command {
    @Override
    void execute() {
        if (ev.getMember().getVoiceState().inVoiceChannel()) {
            VoiceChannel vc = ev.getMember().getVoiceState().getChannel();

            if (!l.getChannelController().isLinked(vc)) {
                TextChannel tc = ev.getChannel();
                l.getChannelController().getLinkedChannels().put(vc, tc);
                l.getChannelController().getPermChannels().put(vc, tc);

                Logger.logAndDelete(String.format("A new link has been established:\n" +
                        " - VC: %s (%s)\n" +
                        " - TC: %s (%s)",
                        vc.getName(), vc.getId(),
                        tc.getName(), tc.getId()));
                l.getDiscordController().sendMessage(tc,
                        String.format("Successfully linked **%s** to **%s**!", vc.getName(), tc.getAsMention()),
                        DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                l.getFileController().saveGuild(ev.getGuild());
            } else {
                handleError(MessageContent.CHANNEL_ALREADY_LINKED);
            }
        } else {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }
    }

    @Override
    String[] getAliases() {
        return new String[] { "link", "l" };
    }

    @Override
    String getDescription() {
        return "Links a voice and text channel together, thereby displaying a log of joins and leaves.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] { Permission.MANAGE_CHANNEL };
    }

    @Override
    String getUsage() {
        return "{}link\n" +
                "    Links the voice channel you are in to the text channel the command was issued from.\n" +
                "    This will make messages appear for joining and leaving the voice channel.";
    }
}
