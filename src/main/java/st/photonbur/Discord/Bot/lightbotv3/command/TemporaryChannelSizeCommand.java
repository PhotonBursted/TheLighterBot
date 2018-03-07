package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
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
        vc.getManagerUpdatable().getUserLimitField()
                .setValue(intLimit)
                .update()
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
                // Remove all permissions that were blocking other users from seeing the channel
                Utils.getPO(tc, tc.getGuild().getPublicRole(), po ->
                        Utils.removePermissionsFrom(po,
                                "The channel had its limit removed by a command from a temporary channel",
                                Permission.MESSAGE_READ));
                Utils.getPO(tc, tc.getGuild().getSelfMember(), po ->
                        Utils.removePermissionsFrom(po,
                                "The channel had its limit removed by a command from a temporary channel",
                                Permission.MESSAGE_READ));

                for (Member m : vc.getMembers()) {
                    Utils.getPO(tc, m, po ->
                            Utils.removePermissionsFrom(po,
                                    "The channel had its limit removed by a command from a temporary channel",
                                    Permission.MESSAGE_READ));
                }
            } else {
                // Revoke access for non-members of the voice channel should the channel be limited
                Utils.getPO(tc, ev.getGuild().getPublicRole(), po -> po
                        .getManagerUpdatable()
                        .deny(Permission.MESSAGE_READ)
                        .update()
                        .reason("The channel was limited by a command from a temporary channel.")
                        .queue());
                Utils.getPO(tc, ev.getGuild().getSelfMember(), po -> po
                        .getManagerUpdatable()
                        .grant(Permission.MESSAGE_READ)
                        .update()
                        .reason("The channel was limited by a command from a temporary channel.")
                        .queue());

                for (Member m : vc.getMembers()) {
                    Utils.getPO(tc, m, po -> po
                            .getManagerUpdatable()
                            .grant(Permission.MESSAGE_READ)
                            .update()
                            .reason("The channel was limited requiring the members to have read permissions")
                            .queue());
                }
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
