package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.ChannelController;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TemporaryChannelSizeCommand extends Command {
    @Override
    public void execute() throws RateLimitedException {
        VoiceChannel vc = ev.getMember().getVoiceState().getChannel();
        TextChannel tc = ChannelController.getLinkedChannels().get(vc);

        if (vc != null) {
            if (vc != ev.getGuild().getAfkChannel()) {
                if (!ChannelController.getPermChannels().containsKey(vc) ||
                        (ChannelController.getPermChannels().containsKey(vc) && ev.getMember().hasPermission(Permission.MANAGE_CHANNEL))) {
                    String limit = input.poll();
                    if (limit.equals("remove")) {
                        limit = "0";
                    }

                    if (Utils.isInteger(limit) && Integer.parseInt(limit) >= 0 && Integer.parseInt(limit) <= 99) {
                        int intLimit = Integer.parseInt(limit);

                        vc.getManagerUpdatable().getUserLimitField()
                                .setValue(intLimit)
                                .update()
                                .reason("A command was issued from a temporary channel")
                                .queue();

                        if (tc != null) {
                            Logger.logAndDelete(String.format("Changed user limit of channel \"%s\" to %s.",
                                    vc.getName(), intLimit));
                            DiscordController.sendMessage(ev, String.format("**%s** changed the user limit %sto **%s**.",
                                    ev.getAuthor().getName(),
                                    tc.equals(ev.getChannel()) ? ("of **" + vc.getName() + "** ") : "",
                                    intLimit));

                            if (intLimit == 0) {
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
                            } else {
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
    public Set<String> getAliases() {
        return new HashSet<>(Arrays.asList("tempchan -s", "tempchan -size"));
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Set<Permission> getPermissionsRequired() {
        return new HashSet<>();
    }

    @Override
    public String getUsage() {
        return null;
    }
}
