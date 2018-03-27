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
public class TemporaryChannelSizeCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(TemporaryChannelSizeCommand.class);

    public TemporaryChannelSizeCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("tempchan", "tc")
                .addAliasPart("-s", "-size"));
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
            handleError(MessageContent.PERMISSIONS_REQUIRED, Permission.MANAGE_CHANNEL.name(), "change the size of permanent voice channels");
            return;
        }

        // If all of this is the case, getInstance the limit to be applied
        String limit = input.poll();
        if (limit.equals("remove")) {
            limit = "0";
        }

        // If the input is an integer and within the limits, update the channel
        if (!Utils.isInteger(limit) || Integer.parseInt(limit) < 0 || Integer.parseInt(limit) > 99) {
            handleError(MessageContent.INVALID_INPUT, "Only integers between 0 (inclusive) and 99 (inclusive) are allowed!");
            return;
        }

        int intLimit = Integer.parseInt(limit);

        // Get the user limit and set the new value
        vc.getManager()
                .setUserLimit(intLimit)
                .reason("A command was issued from a temporary channel")
                .queue();

        // If a channel is linked, update its permissions
        if (tc != null) {
            // Send feedback to the logs and issuer
            LoggerUtils.logAndDelete(log, String.format("Changed user limit of channel \"%s\" to %s.",
                    vc.getName(), intLimit));
            l.getDiscordController().sendMessage(ev,
                    String.format("**%s** changed the user limit %sto **%s**.",
                            ev.getAuthor().getName(),
                            tc.equals(ev.getChannel()) ? ("of **" + vc.getName() + "** ") : "",
                            intLimit),
                    DiscordController.AUTOMATIC_REMOVAL_INTERVAL);

            // If the limit is 0, this means the limit was removed
            if (intLimit == 0) {
                l.getChannelPermissionController().changeToPublicFromPrivate(tc, vc);
            } else {
                l.getChannelPermissionController().changeToPrivateFromPublic(tc, vc);
            }
        }
    }

    @Override
    protected String getDescription() {
        return "Adjusts the user limit of a channel without needing special permissions.";
    }

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[] {};
    }

    @Override
    protected String getUsage() {
        return "{}tempchan -s <channelSize>\n" +
                "    <channelSize> specifies the new size of the voice channel.\n" +
                "    - <channelSize> has to have an integer value between 0 and 99.\n" +
                "        A value of 0 will remove the limit, as does the keyword \"remove\"";
    }
}
