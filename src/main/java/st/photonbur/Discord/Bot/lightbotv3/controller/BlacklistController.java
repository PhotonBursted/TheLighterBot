package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains the blacklist. The blacklist contains users and roles who are not allowed to interact with the bot.
 */
public class BlacklistController {
    private final Launcher l;

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

    private final static String REASON = "The server's blacklist changed and required a permission update";
    
    /**
     * The only BlacklistController instance permitted to exist.
     * Part of the Singleton design pattern.
     */
    private static BlacklistController instance;

    private BlacklistController() {
        blacklist = new HashMap<>();
        l = Launcher.getInstance();
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

        updateChannelPermOverrides(Action.BLACKLIST, g, entity);

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

    private <T extends ISnowflake> void updateChannelPermOverrides(Action action, Guild guild, T entity) {
        l.getChannelController().getLinkedChannels().entrySet().stream()
                .filter(entry -> entry.getKey().getGuild().equals(guild))
                .forEach(entry -> {
                    if (action == Action.BLACKLIST) {
                        Utils.getPO(entry.getValue(), entity, po -> {
                            if (!po.getDenied().contains(Permission.MESSAGE_READ)) {
                                po.getManagerUpdatable()
                                        .deny(Permission.MESSAGE_READ)
                                        .update()
                                        .reason(REASON).queue();
                            }
                        });

                        Utils.getPO(entry.getKey(), entity, po -> {
                            if (!po.getDenied().contains(Permission.VIEW_CHANNEL)) {
                                po.getManagerUpdatable()
                                        .deny(Permission.VIEW_CHANNEL)
                                        .update()
                                        .reason(REASON).queue();
                            }
                        });

                        if (entry.getKey().getParent() != null && !l.getChannelController().getCategories().containsKey(guild)) {
                            Utils.getPO(entry.getKey().getParent(), entity, po -> {
                                if (!po.getDenied().contains(Permission.VIEW_CHANNEL)) {
                                    po.getManagerUpdatable()
                                            .deny(Permission.VIEW_CHANNEL)
                                            .update()
                                            .reason(REASON).queue();
                                }
                            });
                        }
                    } else {
                        PermissionOverride poT = null, poV = null, poC = null;

                        if (entity instanceof Role) {
                            poT = entry.getValue().getPermissionOverride((Role) entity);
                            poV = entry.getKey().getPermissionOverride((Role) entity);
                        } else if (entity instanceof User) {
                            poT = entry.getValue().getPermissionOverride(guild.getMember((User) entity));
                            poV = entry.getKey().getPermissionOverride(guild.getMember((User) entity));
                        }

                        if (poT != null && poT.getDenied().contains(Permission.MESSAGE_READ)) {
                            Utils.removePermissionsFrom(poT, REASON, Permission.MESSAGE_READ);
                        }

                        if (poV != null && poV.getDenied().contains(Permission.VIEW_CHANNEL)) {
                            Utils.removePermissionsFrom(poV, REASON, Permission.VIEW_CHANNEL);
                        }

                        if (entry.getKey().getParent() != null && !l.getChannelController().getCategories().containsKey(guild)) {
                            if (entity instanceof Role) {
                                poC = entry.getKey().getParent().getPermissionOverride((Role) entity);
                            } else if (entity instanceof User) {
                                poC = entry.getKey().getParent().getPermissionOverride(guild.getMember((User) entity));
                            }

                            if (poC != null && poC.getDenied().contains(Permission.VIEW_CHANNEL)) {
                                Utils.removePermissionsFrom(poC, REASON, Permission.VIEW_CHANNEL);
                            }
                        }
                    }
                });
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

        updateChannelPermOverrides(Action.WHITELIST, g, entity);

        return confirmAction(Action.WHITELIST, g, entity);
    }
}
