package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableRole;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableUser;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectionEvent;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.Selector;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectorBuilder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class BlacklistCommand extends Command implements Selector<BannableEntity> {
    private static final Logger log = LoggerFactory.getLogger(BlacklistCommand.class);

    public BlacklistCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("blacklist", "bl"));
    }

    @Override
    void execute() {
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
                        performBlacklist(new BannableUser(candidates.get(0).getUser()));
                    } else {
                        LinkedHashMap<String, BannableEntity> candidateMap = new LinkedHashMap<>();
                        candidates.forEach(c -> candidateMap.put(Utils.userAsString(c.getUser()), new BannableUser(c.getUser())));

                        new SelectorBuilder<>(this)
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
                        performBlacklist(new BannableRole(candidates.get(0)));
                    } else {
                        LinkedHashMap<String, BannableEntity> candidateMap = new LinkedHashMap<>();
                        candidates.forEach(c -> candidateMap.put(c.getName(), new BannableEntity<Role>(c)));

                        new SelectorBuilder<>(this)
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
                    BannableEntity targetEntity = null;

                    // Test if the id was targeting a role or user. If not, throw an error, otherwise whitelist the target
                    if (ev.getGuild().getRoles().stream().anyMatch(role -> role.getId().equals(target))) {
                        targetEntity = new BannableRole(target);
                    }
                    if (ev.getGuild().getMembers().stream().anyMatch(member -> member.getUser().getId().equals(target))) {
                        targetEntity = new BannableUser(target);
                    }

                    if (targetEntity == null) {
                        handleError("The ID you supplied was neither a role or user in this server!");
                    }

                    String response = l.getBlacklistController().blacklist(ev.getGuild(), targetEntity);
                    l.getDiscordController().sendMessage(ev, response, DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                } else {
                    handleError("The entity you tried to blacklist is already blacklisted for this server!");
                }
            }
        } else {
            handleError("You didn't supply the ID of the entity to blacklist!\nPlease use `+blacklist <idToBlacklist>`.");
        }
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
    public void onSelection(SelectionEvent<BannableEntity> selEv) {
        if (selEv.selectionWasMade()) {
            BannableEntity target = selEv.getSelectedOption();

            // If the target isn't blacklisted yet, do so, and provide feedback
            if (!l.getBlacklistController().isBlacklisted(ev.getGuild(), target.getId())) {
                performBlacklist(target);
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

    private void performBlacklist(BannableEntity target) {
        String response = l.getBlacklistController().whitelist(ev.getGuild(), target);

        LoggerUtils.logAndDelete(log, response);
        l.getDiscordController().sendMessage(ev,
                String.format("Successfully blacklisted %s **%s**!", target.getClassName().toLowerCase(),
                        target.isOfClass(User.class) ? Utils.userAsString((User) (target.get())) :
                        target.isOfClass(Role.class) ? ((Role) target.get()).getName() : ""),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }
}
