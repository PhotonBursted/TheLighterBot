package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectionEvent;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.Selector;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectorImpl;

import java.util.*;

public class WhitelistCommand extends Command implements Selector {
    @Override
    void execute() throws RateLimitedException {
        // Check if the input actually had enough arguments
        if (input.size() >= 1) {
            // Get the input after the arguments as one string representation
            String target = Utils.drainQueueToString(input);

            // Identify what the input was targeting
            if (target.startsWith("user:")) {
                // Retrieve a list of users which could be targeted by the search
                List<Member> candidates = ev.getGuild().getMembersByEffectiveName(String.join(":", Arrays.copyOfRange(target.split(":"), 1, target.split(":").length)), true);

                // See if there were any search results
                if (candidates.size() > 0) {
                    // If there was only one user found, perform the whitelist.
                    // Otherwise, generate a selector and let the user decide what the target was
                    if (candidates.size() == 1) {
                        performWhitelist(candidates.get(0).getUser());
                    } else {
                        LinkedHashMap<String, User> candidateMap = new LinkedHashMap<>();
                        candidates.forEach(c -> candidateMap.put(Utils.userAsString(c.getUser()), c.getUser()));

                        new SelectorImpl<>(this, l.getDiscordController().sendMessage(ev, "Building selector..."), candidateMap);
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
                    // If there was only one role found, perform the whitelist.
                    // Otherwise, generate a selector and let the user decide what the target was
                    if (candidates.size() == 1) {
                        performWhitelist(candidates.get(0));
                    } else {
                        LinkedHashMap<String, Role> candidateMap = new LinkedHashMap<>();
                        candidates.forEach(c -> candidateMap.put(c.getName(), c));

                        new SelectorImpl<>(this, l.getDiscordController().sendMessage(ev, "Building selector..."), candidateMap);
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

                        String response = l.getBlacklistController().whitelist(ev.getGuild(), targetRole);
                        l.getDiscordController().sendMessage(ev, response,
                                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                    } else if (ev.getGuild().getMembers().stream().anyMatch(member -> member.getUser().getId().equals(target))) {
                        User targetUser = l.getBot().getUserById(target);

                        String response = l.getBlacklistController().whitelist(ev.getGuild(), targetUser);
                        l.getDiscordController().sendMessage(ev, response,
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
    String[] getAliases() {
        return new String[] {"whitelist", "wl"};
    }

    @Override
    String getDescription() {
        return "Removes a role or user from the blacklist, reenabling them to interact with the bot.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] {Permission.MANAGE_CHANNEL};
    }

    @Override
    String getUsage() {
        return "{}whitelist <searchTerm>.\n" +
                "    <searchTerm> can be any of:\n" +
                "       - <search> - searches for a role/user ID\n" +
                "       - user:<search> - searches for a user with the name of <search>\n" +
                "       - role:<search> - searches for a role with the name of <search>";
    }

    @Override
    public void onSelection(SelectionEvent<?> selEv) {
        if (selEv.selectionWasMade()) {
            Object target = selEv.getSelectedOption();

            // If the target isn't whitelisted yet, do so, and provide feedback
            if (l.getBlacklistController().isBlacklisted(ev.getGuild(), ((ISnowflake) target).getId())) {
                if (target instanceof User) {
                    performWhitelist(((User) target));
                } else if (target instanceof Role) {
                    performWhitelist(((Role) target));
                }

                l.getFileController().saveGuild(ev.getGuild());
            } else {
                if (target instanceof User) {
                    handleError(String.format("User **%s** is already whitelisted for this server!",
                            Utils.userAsString(((User) target))));
                } else if (target instanceof Role) {
                    handleError(String.format("Role **%s** is already whitelisted for this server!",
                            ((Role) target).getName()));
                } else {
                    handleError("This entity is already blacklisted for this server!");
                }
            }
        } else {
            handleError("The blacklist action was cancelled.");
        }
    }

    private void performWhitelist(User user) {
        String response = l.getBlacklistController().whitelist(ev.getGuild(), user);

        Logger.logAndDelete(response);
        l.getDiscordController().sendMessage(ev,
                String.format("Successfully whitelisted user **%s**!", Utils.userAsString(user)),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }

    private void performWhitelist(Role role) {
        String response = l.getBlacklistController().whitelist(ev.getGuild(), role);

        Logger.logAndDelete(response);
        l.getDiscordController().sendMessage(ev,
                String.format("Successfully whitelisted role **%s**!", role.getName()),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }
}
