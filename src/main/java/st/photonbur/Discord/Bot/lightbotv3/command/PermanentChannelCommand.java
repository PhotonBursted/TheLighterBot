package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;

public class PermanentChannelCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(InfoCommand.class);

    public PermanentChannelCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("perm", "p"));
    }

    @Override
    void execute() {
        if (ev.getMember().getVoiceState().inVoiceChannel()) {
            VoiceChannel vc = ev.getMember().getVoiceState().getChannel();

            if (!l.getChannelController().isPermanent(vc)) {
                l.getChannelController().getPermChannels().put(ev.getChannel(), vc);

                LoggerUtils.logAndDelete(log, String.format("%s has been made permanent.", vc.getName()));
                l.getDiscordController().sendMessage(ev.getChannel(),
                        String.format("Successfully made **%s** permanent!", vc.getName()),
                        DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                l.getFileController().applyPermAddition(ev.getChannel(), vc);
            } else {
                handleError(MessageContent.CHANNEL_ALREADY_PERMANENT);
            }
        } else {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }
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
