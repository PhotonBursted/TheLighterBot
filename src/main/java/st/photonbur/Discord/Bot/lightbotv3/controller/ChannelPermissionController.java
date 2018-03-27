package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.ChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.HashMap;
import java.util.Set;

public class ChannelPermissionController {
    private static final Logger log = LoggerFactory.getLogger(ChannelPermissionController.class);
    private static ChannelPermissionController instance;
    /**
     * Instance of the launcher for easy access to other classes
     */
    private final Launcher l = Launcher.getInstance();
    private HashMap<TextChannel, AccessState> currentStates;

    private ChannelPermissionController() {
        currentStates = new HashMap<>();
    }

    public static synchronized ChannelPermissionController getInstance() {
        if (instance == null) {
            instance = new ChannelPermissionController();
        }

        return instance;
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
     * Applies the properties of an accesslist to a certain channel right before it is constructed.
     *
     * @param c     The channel to apply permissions to
     * @param perms The permissions to apply
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void applyAccessList(Channel c, Permission... perms) {
        Guild g = c.getGuild();

        // Figures out the blacklist and whitelist present in this guild
        Set<BannableEntity> blacklist = l.getAccesslistController().getBlacklistForGuild(g);
        Set<BannableEntity> whitelist = l.getAccesslistController().getWhitelistForGuild(g);

        ChannelManager manager = c.getManager();

        if (blacklist != null) {
            for (BannableEntity item : blacklist) {
                if (item.isOfClass(Role.class)) {
                    manager = manager.putPermissionOverride((Role) item.get(), 0L, Permission.getRaw(perms));
                } else if (item.isOfClass(User.class)) {
                    manager = manager.putPermissionOverride(g.getMember((User) item.get()), 0L, Permission.getRaw(perms));
                }
            }
        }

        if (whitelist != null) {
            for (BannableEntity item : whitelist) {
                if (item.isOfClass(Role.class)) {
                    manager = manager.putPermissionOverride((Role) item.get(), Permission.getRaw(perms), 0L);
                } else if (item.isOfClass(User.class)) {
                    manager = manager.putPermissionOverride(g.getMember((User) item.get()), Permission.getRaw(perms), 0L);
                }
            }
        }

        manager.reason("Applying initial access lists").queue();
    }

    public void changeToPrivateFromPublic(TextChannel tc, VoiceChannel vc) {
        checkCorrectLastState(tc, AccessState.PUBLIC);

        ChannelManager manager = tc.getManager();

        // Revoke access for non-members of the voice channel should the channel be limited
        manager = manager
                .putPermissionOverride(tc.getGuild().getPublicRole(), 0L, Permission.getRaw(Permission.MESSAGE_READ))
                .putPermissionOverride(tc.getGuild().getSelfMember(), Permission.getRaw(Permission.MESSAGE_READ), 0L);

        for (Member m : vc.getMembers())
            manager = manager.putPermissionOverride(m, Permission.getRaw(Permission.MESSAGE_READ), 0L);

        manager.reason("The channel was limited by a command from a temporary channel.").queue();

        currentStates.put(tc, AccessState.PRIVATE);
    }

    public void changeToPublicFromPrivate(TextChannel tc, VoiceChannel vc) {
        checkCorrectLastState(tc, AccessState.PRIVATE);

        ChannelManager manager = tc.getManager();

        // Remove all permissions that were blocking other users from seeing the channel
        if (!l.getAccesslistController().isBlacklisted(tc.getGuild().getPublicRole())) {
            manager = manager.putPermissionOverride(tc.getGuild().getPublicRole(), Permission.getRaw(Permission.MESSAGE_READ), 0L);
        }

        manager = manager.putPermissionOverride(tc.getGuild().getSelfMember(), 0L, Permission.getRaw(Permission.MESSAGE_READ));

        for (Member m : vc.getMembers()) {
            manager = manager.putPermissionOverride(m, 0L, Permission.getRaw(Permission.MESSAGE_READ));
        }

        manager.reason("The channel had its limit removed by a command from a temporary channel").queue();

        currentStates.put(tc, AccessState.PUBLIC);
    }

    public void changeToPublicFromNew(TextChannel tc, Permission... perms) {
        checkCorrectLastState(tc, null);

        applyAccessList(tc, perms);

        currentStates.put(tc, AccessState.PUBLIC);
    }

    public void changeToPublicFromNew(VoiceChannel vc, Permission... perms) {
        applyAccessList(vc, perms);
    }

    private void checkCorrectLastState(TextChannel tc, AccessState state) {
        if (currentStates.get(tc) != state) {
            log.warn("Wrong method call was used! Permissions may be wrong for channel.");
        }
    }

    public enum AccessState {
        PRIVATE, PUBLIC
    }
}
