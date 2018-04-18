package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.ChannelManager;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.HashMap;
import java.util.Set;

public class ChannelPermissionController {
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

        manager.reason("Applying access lists").queue();
    }

    /**
     * <p>Changes a channel group from public to private.</p>
     * <p>This encompasses shielding the seeing of these channels again, except for the people in the voice channel.</p>
     * @param tc The text channel to make private
     * @param vc The voice channel to make private
     */
    public void changeToPrivateFromPublic(TextChannel tc, VoiceChannel vc) {
        if (incorrectLastState(tc, AccessState.PUBLIC)) return;

        ChannelManager manager = tc.getManager();

        // Revoke access for non-members of the voice channel should the channel be limited
        manager = manager.putPermissionOverride(tc.getGuild().getPublicRole(), 0L, Permission.getRaw(Permission.MESSAGE_READ));

        for (Member m : vc.getMembers()) {
            manager = manager.putPermissionOverride(m, Permission.getRaw(Permission.MESSAGE_READ), 0L);
        }

        manager.reason("The channel was limited by a command from a temporary channel.").queue();

        currentStates.put(tc, AccessState.PRIVATE);
    }

    public void changeToPublicFromPrivate(TextChannel tc, VoiceChannel vc) {
        if (incorrectLastState(tc, AccessState.PRIVATE)) return;

        ChannelManager manager = tc.getManager();

        // Remove all permissions that were blocking other users from seeing the channel
        if (l.getAccesslistController().isWhitelisted(tc.getGuild().getPublicRole())) {
            manager = manager.putPermissionOverride(tc.getGuild().getPublicRole(), Permission.getRaw(Permission.MESSAGE_READ), 0L);
        }

        for (Member m : vc.getMembers()) {
            manager = manager.putPermissionOverride(m, Permission.getRaw(Permission.MESSAGE_READ), 0L);
        }

        manager.reason("The channel had its limit removed by a command from a temporary channel").queue();

        currentStates.put(tc, AccessState.PUBLIC);
    }

    public void changeToPublicFromNew(TextChannel tc, Permission... perms) {
        if (incorrectLastState(tc, null)) return;

        applyAccessList(tc, perms);

        currentStates.put(tc, AccessState.PUBLIC);
    }

    public void changeToPublicFromNew(VoiceChannel vc, Permission... perms) {
        applyAccessList(vc, perms);
    }

    private boolean incorrectLastState(TextChannel tc, AccessState state) {
        return currentStates.get(tc) != state;
    }

    public enum AccessState {
        PRIVATE, PUBLIC
    }
}
