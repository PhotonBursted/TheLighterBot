package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectionEvent;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.Selector;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectorBuilder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class BlacklistCommand extends Command implements Selector {
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
                    // If there was only one user found, perform the blacklist.
                    // Otherwise, generate a selector and let the user decide what the target was
                    if (candidates.size() == 1) {
                        performBlacklist(candidates.get(0).getUser());
                    } else {
                        LinkedHashMap<String, User> candidateMap = new LinkedHashMap<>();
                        candidates.forEach(c -> candidateMap.put(Utils.userAsString(c.getUser()), c.getUser()));

                        new SelectorBuilder<User>(this)
                                .setOptionMap(candidateMap)
                                .build();
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
                    // If there was only one user found, perform the blacklist.
                    // Otherwise, generate a selector and let the user decide what the target was
                    if (candidates.size() == 1) {
                        performBlacklist(candidates.get(0));
                    } else {
                        LinkedHashMap<String, Role> candidateMap = new LinkedHashMap<>();
                        candidates.forEach(c -> candidateMap.put(c.getName(), c));

                        new SelectorBuilder<Role>(this)
                                .setOptionMap(candidateMap)
                                .build();
                    }
                } else {
                    handleError(String.format("The role you searched for (with name %s) couldn't be found!",
                            String.join(":", Arrays.copyOfRange(target.split(":"), 1, target.split(":").length))));
                }
            } else {
                // Detect if the id specified is already blacklisted
                if (!l.getBlacklistController().isBlacklisted(ev.getGuild(), target)) {
                    // Test if the id was targeting a role or member. If not, throw an error, otherwise blacklist the target
                    if (ev.getGuild().getRoles().stream().anyMatch(role -> role.getId().equals(target))) {
                        Role targetRole = l.getBot().getRoleById(target);

                        String response = l.getBlacklistController().blacklist(ev.getGuild(), targetRole);
                        l.getDiscordController().sendMessage(ev, response,
                                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                    } else if (ev.getGuild().getMembers().stream().anyMatch(member -> member.getUser().getId().equals(target))) {
                        User targetUser = l.getBot().getUserById(target);

                        String response = l.getBlacklistController().blacklist(ev.getGuild(), targetUser);
                        l.getDiscordController().sendMessage(ev, response,
                                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                    } else {
                        handleError("The ID you supplied was neither a role or user in this server!");
                    }
                } else {
                    handleError("The entity you tried to blacklist is already blacklisted for this server!");
                }
            }
        } else {
            handleError("You didn't supply the ID of the entity to blacklist!\nPlease use `+blacklist <idToBlacklist>`.");
        }
    }

    @Override
    String[] getAliases() {
        return new String[] {"blacklist", "bl"};
    }

    @Override
    String getDescription() {
        return "Blacklists a role or user blocking them from any interaction with the bot.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] {Permission.MANAGE_CHANNEL};
    }

    @Override
    String getUsage() {
        return "{}blacklist <searchTerm>\n" +
                "    <searchTerm> can be any of:\n" +
                "       - <search> - searches for a role/user ID\n" +
                "       - user:<search> - searches for a user with the name of <search>\n" +
                "       - role:<search> - searches for a role with the name of <search>";
    }

    @Override
    public void onSelection(SelectionEvent<?> selEv) {
        if (selEv.selectionWasMade()) {
            Object target = selEv.getSelectedOption();

            // If the target isn't blacklisted yet, do so, and provide feedback
            if (!l.getBlacklistController().isBlacklisted(ev.getGuild(), ((ISnowflake) target).getId())) {
                if (target instanceof User) {
                    performBlacklist(((User) target));
                } else if (target instanceof Role) {
                    performBlacklist(((Role) target));
                }

                l.getFileController().saveGuild(ev.getGuild());
            } else {
                if (target instanceof User) {
                    handleError(String.format("User **%s** is already blacklisted for this server!",
                            Utils.userAsString(((User) target))));
                } else if (target instanceof Role) {
                    handleError(String.format("Role **%s** is already blacklisted for this server!",
                            ((Role) target).getName()));
                } else {
                    handleError("This entity is already blacklisted for this server!");
                }
            }
        } else {
            handleError("The blacklist action was cancelled.");
        }
    }

    private void performBlacklist(User user) {
        String response = l.getBlacklistController().blacklist(ev.getGuild(), user);

        Logger.logAndDelete(response);
        l.getDiscordController().sendMessage(ev,
                String.format("Successfully blacklisted user **%s**!", Utils.userAsString(user)),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }

    private void performBlacklist(Role role) {
        String response = l.getBlacklistController().blacklist(ev.getGuild(), role);

        Logger.logAndDelete(response);
        l.getDiscordController().sendMessage(ev,
                String.format("Successfully blacklisted role **%s**!", role.getName()),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }
}
