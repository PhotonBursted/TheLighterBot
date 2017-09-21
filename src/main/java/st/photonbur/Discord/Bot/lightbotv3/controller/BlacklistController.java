package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.entities.*;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains the blacklist. The blacklist contains users and roles who are not allowed to interact with the bot.
 */
public class BlacklistController {
    /**
     * Enum holding the types of actions which can be applied to the blacklist database.
     */
    private enum Action {
        BLACKLIST(), WHITELIST()
    }

    /**
     * Holds the blacklisted entities per guild.
     */
    private final HashMap<Guild, Set<ISnowflake>> blacklist;

    /**
     * The only BlacklistController instance permitted to exist.
     * Part of the Singleton design pattern.
     */
    private static BlacklistController instance;

    private BlacklistController() {
        blacklist = new HashMap<>();
    }

    /**
     * As part of the Singleton design pattern, no clones of this instance are permitted.
     *
     * @return nothing
     * @throws CloneNotSupportedException No clones of this instance are permitted
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Blacklists a {@link User user} or {@link Role role} for a certain guild.
     *
     * @param g      The guild to apply the blacklist to
     * @param entity The user or role to target
     * @param <T>    The target object has to implement {@link ISnowflake} and {@link IMentionable}
     * @return A response string to print into the logs
     */
    public <T extends ISnowflake & IMentionable> String blacklist(Guild g, T entity) {
        blacklist.putIfAbsent(g, new HashSet<>());
        blacklist.get(g).add(entity);

        return confirmAction(Action.BLACKLIST, g, entity);
    }

    /**
     * Generates a string to log as confirmation for the action done.
     *
     * @param action The type of action to have occured
     * @param g      The guild the action happened in
     * @param entity The target object which was affected by the action
     * @param <T>    The target object has to implement both {@link ISnowflake} and {@link IMentionable}
     * @return A response string to print into the logs
     */
    private static <T extends ISnowflake & IMentionable> String confirmAction(Action action, Guild g, T entity) {
        String entityType = entity.getClass().getSimpleName().toLowerCase().replace("impl", "");

        return String.format("%sed a %s in guild \"%s:\"\n" +
                        " - Guild ID: %s\n" +
                        " - %s ID: %s (%s)",
                action.name().substring(0, 1).toUpperCase() + action.name().substring(1).toLowerCase(),
                entityType, g.getName(), g.getId(),
                entityType.substring(0, 1).toUpperCase() + entityType.substring(1), entity.getId(),
                entity instanceof User ? Utils.userAsString((User) entity) : g.getRoleById(entity.getId()).getName());
    }

    Set<? extends ISnowflake> getForGuild(Guild g) {
        return blacklist.get(g);
    }

    /**
     * Controls getting a dynamic instance from this class.
     * Part of the Singleton design pattern.
     *
     * @return The only present instance of this class.
     */
    public static synchronized BlacklistController getInstance() {
        if (instance == null) {
            instance = new BlacklistController();
        }

        return instance;
    }

    /**
     * Checks if an entity is currently whitelisted within their guild.
     *
     * @param g        The {@link Guild guild} to perform the check on
     * @param targetID The String representation of the target's unique ID
     * @return True if the guild's blacklist contains an entity with a unique ID matching the target
     */
    public boolean isBlacklisted(Guild g, String targetID) {
        Set<ISnowflake> blacklistees = blacklist.get(g);

        return blacklistees != null &&
                blacklistees.size() != 0 &&
                blacklistees.stream().anyMatch(item -> item.getId().equals(targetID));
    }

    /**
     * Checks if a member is currently whitelisted within their guild.
     *
     * @param m The {@link Member member} to perform the check on
     * @return True if either the member or any of the member's roles are blacklisted
     */
    public boolean isBlacklisted(Member m) {
        // Retrieve the entities blacklisted for the member's guild
        Set<ISnowflake> blacklistees = blacklist.get(m.getGuild());

        // Return
        return blacklistees != null &&
                blacklistees.size() != 0 &&
                blacklistees.stream().anyMatch(item ->
                        m.getRoles().stream().anyMatch(role ->
                                item.getId().equals(role.getId())) ||
                                item.getId().equals(m.getUser().getId()));
    }

    public <T extends ISnowflake & IMentionable> boolean isBlacklisted(Guild g, T entity) {
        return isBlacklisted(g, entity.getId());
    }

    /**
     * Whitelists a {@link User user} or {@link Role role} for a certain guild.
     *
     * @param g      The guild to remove the blacklist to
     * @param entity The user or role to target
     * @param <T>    The target object has to implement {@link ISnowflake} and {@link IMentionable}
     * @return A response string to print into the logs
     */
    public <T extends ISnowflake & IMentionable> String whitelist(Guild g, T entity) {
        blacklist.get(g).remove(entity);

        return confirmAction(Action.WHITELIST, g, entity);
    }
}
