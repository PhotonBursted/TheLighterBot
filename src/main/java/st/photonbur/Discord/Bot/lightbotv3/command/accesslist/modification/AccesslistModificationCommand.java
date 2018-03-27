package st.photonbur.Discord.Bot.lightbotv3.command.accesslist.modification;

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

public abstract class AccesslistModificationCommand extends Command implements Selector<BannableEntity> {
    protected final String targetAccesslist;
    private final boolean isAdditionAction;
    protected final Logger log;
    private final String primaryCommandName;

    protected AccesslistModificationCommand(CommandAliasCollectionBuilder aliasBuilder,
                                            String primaryCommandName,
                                            String targetAccesslist,
                                            boolean isAdditionAction,
                                            Logger log) {
        super(aliasBuilder);

        this.targetAccesslist = targetAccesslist;
        this.primaryCommandName = primaryCommandName;
        this.isAdditionAction = isAdditionAction;
        this.log = log;
    }

    @Override
    protected void execute() {
        // Check if the input actually had enough arguments
        if (input.size() < 1) {
            handleError(String.format("You didn't supply the ID of the entity to %s!\nPlease use `+%s <idTo%s>`.",
                    getActionDescription("%s", "remove from the %s"),
                    primaryCommandName,
                    StringUtils.capitalize(targetAccesslist)));
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
                    handleError(String.format("No user was found in this server having name **%s**!", targetName));
                    return;
                }

                // If there was only one user found, perform the blacklist.
                // Otherwise, generate a selector and let the user decide what the target was
                if (candidates.size() == 1) {
                    performAccesslistModification(new BannableUser(candidates.get(0).getUser()));
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
                    handleError(String.format("No role was found in this server having name **%s**!", targetName));
                    return;
                }

                // If there was only one user found, perform the blacklist.
                // Otherwise, generate a selector and let the user decide what the target was
                if (candidates.size() == 1) {
                    performAccesslistModification(new BannableRole(candidates.get(0)));
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
                    handleError(String.format("The ID you supplied (`%s`) was neither a role or user in this server!",
                            target));
                    return;
                }

                // Detect if the id specified is already blacklisted
                if (performActionCheck(ev.getGuild(), targetEntity)) {
                    handleError(String.format("The entity you tried to %s is already %sed for this server!",
                            getActionDescription("%s", "remove from the %s"),
                            targetAccesslist));
                    return;
                }

                performAccesslistModification(targetEntity);
                break;
        }
    }

    private String getActionDescription(String additionAction, String removalAction) {
        return String.format((isAdditionAction ? additionAction : removalAction), targetAccesslist);
    }

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[] {Permission.MANAGE_CHANNEL};
    }

    @Override
    protected String getUsage() {
        return "{}" + primaryCommandName + " <searchTerm>\n" +
                "    <searchTerm> can be any of:\n" +
                "       - <search> - searches for a role/user ID\n" +
                "       - user:<search> - searches for a user with the name of <searchTerm>\n" +
                "       - role:<search> - searches for a role with the name of <searchTerm>\n\n" +
                "__Note that the `@everyone` role has the same ID as the guild it is part of!__";
    }

    @Override
    public void onSelection(SelectionEvent<BannableEntity> selEv) {
        if (!selEv.selectionWasMade()) {
            handleError(String.format("The %s action was cancelled.",
                    getActionDescription("%s", "%s removal")));
            return;
        }

        // If the target isn't blacklisted yet, do so, and provide feedback
        performAccesslistModification(selEv.getSelectedOption());
    }

    private void performAccesslistModification(BannableEntity target) {
        if (performActionCheck(ev.getGuild(), target)) {
            if (target.isOfClass(User.class)) {
                User targetUser = (User) target.get();
                if (targetUser.equals(l.getBot().getSelfUser())) {
                    handleError("Access list operations cannot be executed on the bot itself!");
                    return;
                }

                handleError(String.format("User **%s** is already %s for this server!",
                        Utils.userAsString((User) target.get()),
                        getActionDescription("%sed", "not present in the %s")));
                return;
            }

            if (target.isOfClass(Role.class)) {
                handleError(String.format("Role **%s** is already %s for this server!",
                        ((Role) target.get()).getName(),
                        getActionDescription("%sed", "not present in the %s")));
                return;
            }

            handleError(String.format("This entity is already %s for this server!",
                    getActionDescription("%sed", "not present in the %s")));
            return;
        }

        String response = performAction(ev.getGuild(), target);

        LoggerUtils.logAndDelete(log, response);
        l.getDiscordController().sendMessage(ev,
                String.format("%s **%s** was successfully %s!",
                        StringUtils.capitalize(target.get().getClass().getSimpleName().toLowerCase().replace("impl", "")),
                        target.isOfClass(User.class) ? Utils.userAsString((User) target.get()) :
                        target.isOfClass(Role.class) ? String.format("`%s`", ((Role) target.get()).getName()) : "",
                        getActionDescription("%sed", "removed from the %s")),
                DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
    }

    protected abstract String performAction(Guild guild, BannableEntity target);

    protected abstract boolean performActionCheck(Guild guild, BannableEntity target);
}
