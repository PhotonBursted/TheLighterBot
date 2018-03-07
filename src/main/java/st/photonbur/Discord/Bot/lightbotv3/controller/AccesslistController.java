package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import st.photonbur.Discord.Bot.lightbotv3.entity.EntityConverter;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableRole;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableUser;
import st.photonbur.Discord.Bot.lightbotv3.entity.permissible.PermissibleEntity;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.StringUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;
import st.photonbur.Discord.Bot.lightbotv3.misc.map.accesslist.BlacklistMap;
import st.photonbur.Discord.Bot.lightbotv3.misc.map.accesslist.WhitelistMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Maintains the access list. The access list contains users and roles who are not or specifically allowed to interact with the bot.
 */
public class AccesslistController {
    private final Launcher l;

    /**
     * Enum holding the types of actions which can be applied to the blacklist database.
     */
    private enum Action {
        BLACKLIST, UNBLACKLIST, UNWHITELIST, WHITELIST
    }

    /**
     * Holds the blacklisted {@link st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity entities} per guild.
     */
    private final BlacklistMap blacklist;

    /**
     * Holds the whitelisted {@link st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity entities} per guild.
     */
    private final WhitelistMap whitelist;

    private final static String REASON = "The server's blacklist changed and required a permission update";

    /**
     * The only AccesslistController instance permitted to exist.
     * Part of the Singleton design pattern.
     */
    private static AccesslistController instance;

    private AccesslistController() {
        blacklist = new BlacklistMap();
        whitelist = new WhitelistMap();

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
    public static synchronized AccesslistController getInstance() {
        if (instance == null) {
            instance = new AccesslistController();
        }

        return instance;
    }

    /**
     * Blacklists a {@link BannableEntity bannable entity} for a certain guild.
     *
     * @param g      The guild to apply the blacklist to
     * @param entity The bannable entiity to target
     * @return A response string to print into the logs
     */
    public String blacklist(Guild g, BannableEntity entity) {
        unwhitelist(g, entity);
        blacklist.putStoring(g, entity);

        updateChannelPermOverrides(Action.BLACKLIST, g,
                entity.isOfClass(User.class) ?
                        EntityConverter.toPermissible((BannableUser) entity, g) :
                        EntityConverter.toPermissible((BannableRole) entity));

        return confirmAction(Action.BLACKLIST, g, entity);
    }

    /**
     * Generates a string to log as confirmation for the action done.
     *
     * @param action The type of action to have occured
     * @param g      The guild the action happened in
     * @return A response string to print into the logs
     */
    private static String confirmAction(Action action, Guild g, BannableEntity entity) {
        String entityType = entity.get().getClass().getSimpleName().toLowerCase().replace("impl", "");

        return String.format("%sed a %s in guild \"%s\":\n" +
                        " - Guild ID: %s\n" +
                        " - %s ID: %s (%s)",
                StringUtils.capitalize(action.name().toLowerCase()),
                entityType, g.getName(), g.getIdLong(),
                StringUtils.capitalize(entityType), entity.getIdLong(),
                entity.isOfClass(User.class) ? Utils.userAsString((User) entity.get()) : g.getRoleById(entity.getId()).getName());
    }

    public Set<BannableEntity> getBlacklistForGuild(Guild g) {
        return blacklist.get(g);
    }

    public Set<BannableEntity> getWhitelistForGuild(Guild g) {
        return whitelist.get(g);
    }

    /**
     * <p>
     * Checks if a member is currently blacklisted within their guild.
     * </p>
     * <p></p>
     * This method doesn't take the member's roles or whitelists into account; only the member's presence in the blacklist is teested.
     * </p>
     *
     * @param m The {@link net.dv8tion.jda.core.entities.Member member} to perform the check on
     * @return {@code true} if the member is blacklisted, {@code false} otherwise.
     * @see AccesslistController#isEffectivelyBlacklisted(Member)
     * @see AccesslistController#isBlacklisted(Role)
     */
    private boolean isBlacklisted(Member m) {
        return isContainedWithinAccessList(new BannableUser(m.getUser()), blacklist.get(m.getGuild()));
    }

    /**
     * <p>
     * Checks if a role is currently blacklisted within their guild.
     * </p>
     * <p></p>
     * This method doesn't take whitelists into account; only the role's presence in the blacklist is teested.
     * </p>
     *
     * @param r The {@link net.dv8tion.jda.core.entities.Role role} to perform the check on
     * @return {@code true} if the role is blacklisted, {@code false} otherwise.
     * @see AccesslistController#isEffectivelyBlacklisted(Member)
     * @see AccesslistController#isBlacklisted(Member)
     */
    public boolean isBlacklisted(Role r) {
        return isContainedWithinAccessList(new BannableRole(r), blacklist.get(r.getGuild()));
    }
    
    /**
     * Checks if an {@link st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity entity} is currently
     * contained within the passed list of {@link st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity banned entities}.
     *
     * @param entity The {@link st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity entity} to perform the check on
     * @return {@code true} if the entity is contained within, {@code false} otherwise.
     */
    private boolean isContainedWithinAccessList(BannableEntity entity,
                                                Set<BannableEntity> entities) {
        return entities != null &&
                entities.size() != 0 &&
                entities.stream().anyMatch(item ->
                        entity.getIdLong() == item.getIdLong());
    }

    /**
     * <p>
     * Checks if a member is currently <i>effectively</i> blacklisted within their guild.
     * </p>
     * <p>
     *     Being effectively blacklisted means one or more of the following apply:
     *     <ul>
     *         <li>The passed member itself is blacklisted explicitly.</li>
     *         <li>One of the passed member's roles is blacklisted without the member being whitelisted explicitly.</li>
     *     </ul>
     * </p>
     * <p></p>
     * This method, contrary to {@link #isBlacklisted(Member)}, takes into the member's roles or possible presence in the whitelist into account, as described above.
     * </p>
     *
     * @param m The member to check for being effectively blacklisted from their guild.
     * @return {@code true} if the member is effectively blacklisted, {@code false} otherwise.
     */
    public boolean isEffectivelyBlacklisted(Member m) {
        return isBlacklisted(m) || (m.getRoles().stream().anyMatch(this::isBlacklisted) && !isWhitelisted(m));
    }

    /**
     * <p>
     * Checks if a member is currently <i>effectively</i> whitelisted within their guild.
     * </p>
     * <p>
     *     Being effectively whitelisted means one or more of the following apply:
     *     <ul>
     *         <li>The passed member itself is whitelisted explicitly.</li>
     *         <li>One of the passed member's roles is whitelisted without the member being blacklisted explicitly</li>
     *     </ul>
     * </p>
     * <p></p>
     * This method, contrary to {@link #isWhitelisted(Member)}, takes into the member's roles or possible presence in the whitelist into account, as described above.
     * </p>
     *
     * @param m The member to check for being effectively blacklisted from their guild.
     * @return {@code true} if the member is effectively blacklisted, {@code false} otherwise.
     */
    public boolean isEffectivelyWhitelisted(Member m) {
        return isWhitelisted(m) || (m.getRoles().stream().anyMatch(this::isWhitelisted) && !isBlacklisted(m));
    }

    /**
     * <p>
     * Checks if a member is currently whitelisted within their guild.
     * </p>
     * <p></p>
     * This method doesn't take the member's roles or blacklists into account; only the member's presence in the whitelist is teested.
     * </p>
     *
     * @param m The {@link net.dv8tion.jda.core.entities.Member member} to perform the check on
     * @return {@code true} if the member is whitelisted, {@code false} otherwise.
     * @see AccesslistController#isEffectivelyWhitelisted(Member)
     * @see AccesslistController#isWhitelisted(Role)
     */
    private boolean isWhitelisted(Member m) {
        return isContainedWithinAccessList(new BannableUser(m.getUser()), whitelist.get(m.getGuild()));
    }

    /**
     * <p>
     * Checks if a role is currently whitelisted within their guild.
     * </p>
     * <p></p>
     * This method doesn't take blacklists into account; only the role's presence in the whitelist is teested.
     * </p>
     *
     * @param r The {@link net.dv8tion.jda.core.entities.Role role} to perform the check on
     * @return {@code true} if the member is whitelisted, {@code false} otherwise.
     * @see AccesslistController#isEffectivelyWhitelisted(Member)
     * @see AccesslistController#isWhitelisted(Member)
     */
    public boolean isWhitelisted(Role r) {
        return isContainedWithinAccessList(new BannableRole(r), whitelist.get(r.getGuild()));
    }

    private void updateChannelPermOverrides(Action action, Guild g, BannableEntity entity) {
        updateChannelPermOverrides(action, g,entity.isOfClass(User.class) ?
                EntityConverter.toPermissible((BannableUser) entity, g) :
                EntityConverter.toPermissible((BannableRole) entity));
    }

    private void updateChannelPermOverrides(Action action, Guild guild, PermissibleEntity entity) {
        l.getChannelController().getLinkedChannels().entrySet().stream()
                .filter(entry -> entry.getKey().getGuild().equals(guild))
                .forEach(entry -> {
                    if (action == Action.BLACKLIST) {
                        Utils.getPO(entry.getKey(), entity, po -> {
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
                        PermissionOverride poT = null, poC = null;
                        List<PermissionOverride> poVs = new ArrayList<>();

                        if (entity.isOfClass(Role.class)) {
                            poT = entry.getKey().getPermissionOverride((Role) entity);

                            for (VoiceChannel vc : entry.getValue()) {
                                poVs.add(vc.getPermissionOverride((Role) entity));
                            }
                        } else if (entity.isOfClass(User.class)) {
                            poT = entry.getKey().getPermissionOverride(guild.getMember((User) entity));

                            for (VoiceChannel vc : entry.getValue()) {
                                poVs.add(vc.getPermissionOverride(guild.getMember((User) entity)));
                            }
                        }

                        if (poT != null && poT.getDenied().contains(Permission.MESSAGE_READ)) {
                            Utils.removePermissionsFrom(poT, REASON, Permission.MESSAGE_READ);
                        }

                        for (PermissionOverride poV : poVs) {
                            if (poV != null && poV.getDenied().contains(Permission.VIEW_CHANNEL)) {
                                Utils.removePermissionsFrom(poV, REASON, Permission.VIEW_CHANNEL);
                            }
                        }

                        if (entry.getKey().getParent() != null && !l.getChannelController().getCategories().containsKey(guild)) {
                            if (entity.isOfClass(Role.class)) {
                                poC = entry.getKey().getParent().getPermissionOverride((Role) entity);
                            } else if (entity.isOfClass(User.class)) {
                                poC = entry.getKey().getParent().getPermissionOverride(guild.getMember((User) entity));
                            }

                            if (poC != null && poC.getDenied().contains(Permission.VIEW_CHANNEL)) {
                                Utils.removePermissionsFrom(poC, REASON, Permission.VIEW_CHANNEL);
                            }
                        }
                    }
                });
    }

    public void reset() {
        blacklist.clear();
        whitelist.clear();
    }

    public String unblacklist(Guild g, BannableEntity entity) {
        blacklist.removeStoring(g, entity);

        updateChannelPermOverrides(Action.WHITELIST, g, entity);

        return confirmAction(Action.UNBLACKLIST, g, entity);
    }

    public String unwhitelist(Guild g, BannableEntity entity) {
        whitelist.removeStoring(g, entity);

        updateChannelPermOverrides(Action.WHITELIST, g, entity);

        return confirmAction(Action.UNWHITELIST, g, entity);
    }

    /**
     * Whitelists a {@link BannableEntity bannable entity} for a certain guild.
     *
     * @param g      The guild to remove the blacklist to
     * @param entity The entity to target
     * @return A response string to print into the logs
     */
    public String whitelist(Guild g, BannableEntity entity) {
        unblacklist(g, entity);
        whitelist.putStoring(g, entity);

        updateChannelPermOverrides(Action.WHITELIST, g, entity);

        return confirmAction(Action.WHITELIST, g, entity);
    }
}
