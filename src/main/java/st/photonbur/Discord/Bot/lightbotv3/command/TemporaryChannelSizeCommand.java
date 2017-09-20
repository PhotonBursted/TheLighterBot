package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TemporaryChannelSizeCommand extends Command {
    public TemporaryChannelSizeCommand(Launcher l) {
        super(l);
    }

    @Override
    public void execute() throws RateLimitedException {
        // Get the channels targeted by the issuer
        VoiceChannel vc = ev.getMember().getVoiceState().getChannel();
        TextChannel tc = l.getChannelController().getLinkedChannels().get(vc);

        // If the voice channel wasn't found the user wasn't in one to start with
        if (vc != null) {
            // The target should not be the default channel
            if (vc != ev.getGuild().getAfkChannel()) {
                // If the target voice channel is permanent, the user requires MANAGE_CHANNEL permissions
                if (!l.getChannelController().getPermChannels().containsKey(vc) ||
                        (l.getChannelController().getPermChannels().containsKey(vc) && ev.getMember().hasPermission(Permission.MANAGE_CHANNEL))) {
                    // If all of this is the case, getInstance the limit to be applied
                    String limit = input.poll();
                    if (limit.equals("remove")) {
                        limit = "0";
                    }

                    // If the input is an integer and within the limits, update the channel
                    if (Utils.isInteger(limit) && Integer.parseInt(limit) >= 0 && Integer.parseInt(limit) <= 99) {
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
                            Logger.logAndDelete(String.format("Changed user limit of channel \"%s\" to %s.",
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
                                Utils.removePermissionsFrom(
                                        Utils.getPO(tc, tc.getGuild().getPublicRole()),
                                        "The channel had its limit removed by a command from a temporary channel",
                                        Permission.MESSAGE_READ);
                                Utils.removePermissionsFrom(
                                        Utils.getPO(tc, tc.getGuild().getSelfMember()),
                                        "The channel had its limit removed by a command from a temporary channel",
                                        Permission.MESSAGE_READ);

                                for (Member m : vc.getMembers()) {
                                    Utils.removePermissionsFrom(
                                            Utils.getPO(tc, m),
                                            "The channel had its limit removed by a command from a temporary channel",
                                            Permission.MESSAGE_READ);
                                }
                            } else {
                                // Revoke access for non-members of the voice channel should the channel be limited
                                Utils.getPO(tc, ev.getGuild().getPublicRole())
                                        .getManagerUpdatable()
                                        .deny(Permission.MESSAGE_READ)
                                        .update()
                                        .reason("The channel was limited by a command from a temporary channel.")
                                        .queue();
                                Utils.getPO(tc, ev.getGuild().getSelfMember())
                                        .getManagerUpdatable()
                                        .grant(Permission.MESSAGE_READ)
                                        .update()
                                        .reason("The channel was limited by a command from a temporary channel.")
                                        .queue();

                                for (Member m : vc.getMembers()) {
                                    Utils.getPO(tc, m)
                                            .getManagerUpdatable()
                                            .grant(Permission.MESSAGE_READ)
                                            .update()
                                            .reason("The channel was limited requiring the members to have read permissions")
                                            .queue();
                                }
                            }
                        }
                    } else {
                        handleError(MessageContent.INVALID_INPUT, "Only integers between 0 (inclusive) and 99 (inclusive) are allowed!");
                    }
                } else {
                    handleError(MessageContent.PERMISSIONS_REQUIRED_PERMANENT_CHANNEL_SIZE_CHANGE);
                }
            } else {
                handleError(MessageContent.AFK_CHANNEL_ACTION_NOT_PERMITTED);
            }
        } else {
            handleError(MessageContent.NOT_IN_VOICE_CHANNEL);
        }
    }

    @Override
    public String[] getAliases() {
        return new String[] {"tc -s", "tc -size", "tempchan -s", "tempchan -size"};
    }

    @Override
    public String getDescription() {
        return "Adjusts the user limit of a channel without needing special permissions.";
    }

    @Override
    public Permission[] getPermissionsRequired() {
        return new Permission[] {};
    }

    @Override
    public String getUsage() {
        return "{}tempchan -size <channelSize>.\n" +
                "  <channelSize> specifies the new size of the voice channel.\n" +
                "  - <channelSize> has to have an integer value between 0 and 99.\n" +
                "    A value of 0 will remove the limit, as does the keyword \"remove\"";
    }
}