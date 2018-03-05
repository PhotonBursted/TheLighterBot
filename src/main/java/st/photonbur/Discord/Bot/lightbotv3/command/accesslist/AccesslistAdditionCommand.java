package st.photonbur.Discord.Bot.lightbotv3.command.accesslist;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import st.photonbur.Discord.Bot.lightbotv3.command.Command;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableRole;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableUser;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.StringUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectionEvent;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.Selector;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectorBuilder;

import java.util.LinkedHashMap;
import java.util.List;

abstract class AccesslistAdditionCommand extends Command implements Selector<BannableEntity> {
    private final String actionName;
    private final Logger log;

    AccesslistAdditionCommand(CommandAliasCollectionBuilder aliasBuilder, String actionName, Logger log) {
        super(aliasBuilder);

        this.actionName = actionName;
        this.log = log;
    }

    @Override
    protected void execute() {
        // Check if the input actually had enough arguments
        if (input.size() < 1) {
            handleError(String.format("You didn't supply the ID of the entity to %1$s!\nPlease use `+%1$s <idTo%2$s>`.",
                    actionName, StringUtils.capitalize(actionName)));
            return;
        }

        // Get the input after the arguments as one string representation
        String target = Utils.drainQueueToString(input);
        String[] targetParts = target.split(":", 2);

        String targetType = targetParts[0];
        String targetName = targetParts.length > 1 ? targetParts[1] : null;

        // Identify what the input was targeting
        switch (targetType) {
            case "user": {
                // Retrieve a list of users which could be targeted by the search
                List<Member> candidates = ev.getGuild().getMembersByEffectiveName(targetName, true);

                // See if there were any search results
                if (candidates.size() <= 0) {
                    handleError(String.format("No user was found in this server having name **%s!**", targetName));
                    return;
                }

                // If there was only one user found, perform the blacklist.
                // Otherwise, generate a selector and let the user decide what the target was
                if (candidates.size() == 1) {
                    performAccessListModification(new BannableUser(candidates.get(0).getUser()));
                } else {
                    LinkedHashMap<String, BannableEntity> candidateMap = new LinkedHashMap<>();
                    candidates.forEach(c -> candidateMap.put(Utils.userAsString(c.getUser()), new BannableUser(c.getUser())));

                    new SelectorBuilder<>(this)
                            .setOptionMap(candidateMap)
                            .build();
                }
                break;
            }
            case "role": {
                // Retrieve a list of roles which could be targeted by the search
                List<Role> candidates = ev.getGuild().getRolesByName(targetName, true);

                // See if there were any search results
                if (candidates.size() <= 0) {
                    handleError(String.format("The role you searched for (with name **%s**) couldn't be found!", targetName));
                    return;
                }

                // If there was only one user found, perform the blacklist.
                // Otherwise, generate a selector and let the user decide what the target was
                if (candidates.size() == 1) {
                    performAccessListModification(new BannableRole(candidates.get(0)));
                } else {
                    LinkedHashMap<String, BannableEntity> candidateMap = new LinkedHashMap<>();
                    candidates.forEach(c -> candidateMap.put(c.getName(), new BannableRole(c)));

                    new SelectorBuilder<>(this)
                            .setOptionMap(candidateMap)
                            .build();
                }
                break;
            }
            default:
                BannableEntity targetEntity = null;

                // Test if the id was targeting a role or user. If not, throw an error, otherwise whitelist the target
                if (ev.getGuild().getRoles().stream().anyMatch(role -> role.getId().equals(target))) {
                    targetEntity = new BannableRole(target);
                }
                if (ev.getGuild().getMembers().stream().anyMatch(member -> member.getUser().getId().equals(target))) {
                    targetEntity = new BannableUser(target);
                }

                if (targetEntity == null) {
                    handleError(String.format("The ID you supplied (**`%s`**) was neither a role or user in this server!",
                            target));
                    return;
                }

                // Detect if the id specified is already blacklisted
                if (performActionCheck(ev.getGuild(), targetEntity)) {
                    handleError(String.format("The entity you tried to %1$s is already %1$sed for this server!",
                            actionName));
                    return;
                }

                performAccessListModification(targetEntity);
                break;
        }
    }

    @Override
    protected String getDescription() {
        return StringUtils.capitalize(actionName) + "s a role or user.";
    }

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[] {Permission.MANAGE_CHANNEL};
    }

    @Override
    protected String getUsage() {
        return "{}" + actionName + " <searchTerm>\n" +
                "    <searchTerm> can be any of:\n" +
                "       - <search> - searches for a role/user ID\n" +
                "       - user:<search> - searches for a user with the name of <searchTerm>\n" +
                "       - role:<search> - searches for a role with the name of <searchTerm>\n\n" +
                "__Note that the `@everyone` role has the same ID as the guild it is part of!__";
    }

    @Override
    public void onSelection(SelectionEvent<BannableEntity> selEv) {
        if (!selEv.selectionWasMade()) {
            handleError(String.format("The %s action was cancelled.", actionName));
            return;
        }

        BannableEntity target = selEv.getSelectedOption();

        if (!performActionCheck(ev.getGuild(), target)) {
            if (target.isOfClass(User.class)) {
                handleError(String.format("User **%s** is already %sed for this server!",
                        Utils.userAsString((User) target.get()), actionName));
                return;
            }

            if (target.isOfClass(Role.class)) {
                handleError(String.format("Role **%s** is already %sed for this server!",
                        ((Role) target.get()).getName(), actionName));
                return;
            }

            handleError(String.format("This entity is already %sed for this server!",
                    actionName));
        } else {
            // If the target isn't blacklisted yet, do so, and provide feedback
            performAccessListModification(target);
        }
    }

    private void performAccessListModification(BannableEntity target) {
        String response = performAction(ev.getGuild(), target);

        LoggerUtils.logAndDelete(log, response);
        l.getDiscordController().sendMessage(ev,
                String.format("Successfully %sed %s **%s**!",
                        actionName,
                        target.get().getClass().getSimpleName().toLowerCase(),
                        target.isOfClass(User.class) ? Utils.userAsString((User) (target.get())) :
                        target.isOfClass(Role.class) ? ((Role) target.get()).getName() : ""),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }

    abstract String performAction(Guild guild, BannableEntity target);

    abstract boolean performActionCheck(Guild guild, BannableEntity target);
}
