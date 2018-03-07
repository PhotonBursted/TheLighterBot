package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;

@AvailableCommand
public class UnpermanentChannelCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(UnpermanentChannelCommand.class);

    public UnpermanentChannelCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("unperm", "up", "temp"));
    }

    @Override
    protected void execute() {
        if (!ev.getMember().getVoiceState().inVoiceChannel()) {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }

        VoiceChannel vc = ev.getMember().getVoiceState().getChannel();

        if (!l.getChannelController().isPermanent(vc)) {
            handleError(MessageContent.CHANNEL_NOT_PERMANENT);
        }

        l.getChannelController().getPermChannels().removeByValueStoring(vc);

        LoggerUtils.logAndDelete(log, String.format("%s has been made temporary.", vc.getName()));
        l.getDiscordController().sendMessage(ev,
                String.format("Successfully made **%s** temporary!%s", vc.getName(),
                        l.getChannelController().isLinked(vc) ? "\n__Be aware that leaving the channel empty will now delete the channel!__" : ""),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
        l.getFileController().applyPermDeletion(ev.getChannel(), vc);
    }

    @Override
    protected String getDescription() {
        return "Makes a voice channel temporary.\nThis means the channel will be deleted when empty and linked.";
    }

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[] { Permission.MANAGE_CHANNEL };
    }

    @Override
    protected String getUsage() {
        return "{}unperm\n" +
                "    Makes the voice channel you are in temporary.\n" +
                "    If the voice channel is linked to a text channel, this means it will be deleted when empty.";
    }
}
