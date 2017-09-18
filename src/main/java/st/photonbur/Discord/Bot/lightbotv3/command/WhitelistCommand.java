package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.*;

public class WhitelistCommand extends Command {
    public WhitelistCommand(Launcher l) {
        super(l);
    }

    @Override
    void execute() throws RateLimitedException {
        // Check if the input actually had enough arguments
        if (input.size() >= 2) {
            // Get the input after the arguments as one string representation
            String target = Utils.drainQueueToString(input);

            // Identify what the input was targeting
            if (target.startsWith("user:")) {
                // Retrieve a list of users which could be targeted by the search
                List<Member> candidates = ev.getGuild().getMembersByEffectiveName(String.join(":", Arrays.copyOfRange(target.split(":"), 1, target.split(":").length)), true);

                // See if there were any search results
                if (candidates.size() > 0) {
                    // Store the first result to be found
                    //TODO redo search system to allow better control of results (paginator?)
                    User userToWhitelist = candidates.get(0).getUser();

                    // If the target isn't whitelisted yet, do so, and provide feedback
                    if (l.getBlacklistController().isBlacklisted(ev.getGuild(), userToWhitelist)) {
                        l.getBlacklistController().whitelist(ev.getGuild(), userToWhitelist);
                        l.getDiscordController().sendMessage(ev,
                                String.format("Successfully whitelisted **%s** (ID %s)",
                                        Utils.userAsString(userToWhitelist), userToWhitelist.getId()),
                                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                    } else {
                        handleError(String.format("**%s** is already whitelisted for this server!",
                                Utils.userAsString(userToWhitelist)));
                    }
                } else {
                    handleError(String.format("No user was found in this server having name **%s!**",
                            String.join(":", Arrays.copyOfRange(target.split(":"), 1, target.split(":").length))));
                }
            } else if (target.startsWith("role:")) {
                // Retrieve a list of roles which could be targeted by the search
                List<Role> candidates = ev.getGuild().getRolesByName(String.join(":", Arrays.copyOfRange(target.split(":"), 1, target.split(":").length)), true);

                // See if there were any search results
                if (candidates.size() > 0) {
                    // Store the first result to be found
                    Role roleToWhitelist = candidates.get(0);

                    // If the target isn't whitelisted yet, do so, and provide feedback
                    if (l.getBlacklistController().isBlacklisted(ev.getGuild(), roleToWhitelist)) {
                        l.getBlacklistController().whitelist(ev.getGuild(), roleToWhitelist);
                        l.getDiscordController().sendMessage(ev,
                                String.format("Successfully whitelisted the **%s** role (ID %s)",
                                    roleToWhitelist.getName(), roleToWhitelist.getId()),
                                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                    } else {
                        handleError("The role you tried to whitelist is already whitelisted for this server!");
                    }
                } else {
                    handleError(String.format("The role you searched for (with name %s) couldn't be found!",
                            String.join(":", Arrays.copyOfRange(target.split(":"), 1, target.split(":").length))));
                }
            } else {
                // Detect if the id specified is already whitelisted
                if (l.getBlacklistController().isBlacklisted(ev.getGuild(), target)) {
                    // Test if the id was targeting a role or member. If not, throw an error, otherwise whitelist the target
                    if (ev.getGuild().getRoles().stream().anyMatch(role -> role.getId().equals(target))) {
                        Role targetRole = l.getBot().getRoleById(target);

                        l.getBlacklistController().whitelist(ev.getGuild(), targetRole);
                        l.getDiscordController().sendMessage(ev,
                                String.format("Successfully whitelisted the **%s** role (ID %s)",
                                        targetRole.getName(), target),
                                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                    } else if (ev.getGuild().getMembers().stream().anyMatch(member -> member.getUser().getId().equals(target))) {
                        User targetUser = l.getBot().getUserById(target);

                        l.getBlacklistController().whitelist(ev.getGuild(), targetUser);
                        l.getDiscordController().sendMessage(ev,
                                String.format("Successfully whitelisted **%s** (ID %s)",
                                        Utils.userAsString(targetUser), target),
                                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                    } else {
                        handleError("The ID you supplied was neither a role or user in this server!");
                    }
                } else {
                    handleError("The entity you tried to whitelist is already whitelisted for this server!");
                }
            }
        } else {
            handleError("You didn't supply the ID of the entity to whitelist!\nPlease use `+whitelist <idToWhitelist>`.");
        }
    }

    @Override
    Set<String> getAliases() {
        return new HashSet<>(Arrays.asList("wl", "whitelist"));
    }

    @Override
    String getDescription() {
        return null;
    }

    @Override
    Set<Permission> getPermissionsRequired() {
        return new HashSet<>(Collections.singletonList(Permission.MANAGE_CHANNEL));
    }

    @Override
    String getUsage() {
        return null;
    }
}
