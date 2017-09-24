package st.photonbur.Discord.Bot.lightbotv3.controller;

import com.sun.istack.internal.Nullable;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.ChannelAction;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ChannelController extends ListenerAdapter {
    private enum EventType {
        LEAVE, JOIN, MOVE
    }

    /**
     * Stores the default category specified per guild
     */
    private final HashMap<Guild, Category> categories;
    /**
     * Stores a timeout per voice channel.
     * Makes sure that, when a voice channel is created, it gets deleted when it is left unused.
     */
    private final HashMap<VoiceChannel, ScheduledExecutorService> timeoutCandidates;
    /**
     * Keeps track of channels which are linked to each other.
     * This "linkage" is referring to the displaying of join, leave and move actions in the text channel.
     */
    private final HashMap<VoiceChannel, TextChannel> linkedChannels;
    /**
     * Keeps track of pairs of channels which should not be removed when empty.
     */
    private final HashMap<VoiceChannel, TextChannel> permChannels;

    /**
     * Instance of the launcher for easy access to other classes
     */
    private final Launcher l;

    private static ChannelController instance;

    private ChannelController(Launcher l) {
        this.l = l;

        categories = new HashMap<>();
        linkedChannels = new HashMap<>();
        permChannels = new HashMap<>();
        timeoutCandidates = new HashMap<>();
    }

    public static ChannelController getInstance() {
        return getInstance(null);
    }

    public static ChannelController getInstance(Launcher l) {
        if (instance == null && !(l == null)) {
            instance = new ChannelController(l);
        }

        return instance;
    }

    /**
     * Applies the blacklist to a certain channel right before it is constructed.
     *
     * @param g      The guild the channel is in
     * @param action The action representing the channel to apply the blacklist to
     * @param perms  The permissions to apply
     */
    private void applyBlacklist(Guild g, ChannelAction action, Permission... perms) {
        // Figures out the blacklist present in this guild
        Set<? extends ISnowflake> blacklist = l.getBlacklistController().getForGuild(g);
        if (blacklist != null) {
            // If applicable, deny people their access of this category
            blacklist.forEach(item -> {
                if (item instanceof Role) {
                    action.addPermissionOverride((Role) item, 0L, Permission.getRaw(perms));
                } else if (item instanceof User) {
                    action.addPermissionOverride(g.getMember((User) item), 0L, Permission.getRaw(perms));
                }
            });
        }
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
     * Creates a temporary category to house a fresh set of temporary channels in.
     *
     * @param g        The guild to put the new category in.
     * @param name     The name to give the category.
     * @param callback The action to perform after creating this category.
     */
    public void createTempCategory(Guild g, String name, Consumer<Category> callback) {
        // Creates an intermediate action which only contains the name of the new category.
        ChannelAction cAction = g.getController().createCategory("Temp: " + name);

        applyBlacklist(g, cAction, Permission.MESSAGE_READ);

        // Finally construct the category object.
        // This construction is split so it only requires one request instead of multiple.
        cAction.setParent(categories.get(g))
                .reason("A new set of temporary channels was issued, needing an new category as no default was specified")
                .queue((c) -> {
                    if (callback != null) {
                        callback.accept(((Category) c));
                    }
                });
    }

    /**
     * Creates a temporary text channel.
     *
     * @param ev       The event which caused the new channel to be created.
     * @param name     The name to give the channel.
     * @param parent   The category to house the new channel in.
     * @param callback The action to perform after creating this text channel.
     */
    public void createTempTextChannel(GuildMessageReceivedEvent ev, String name, Category parent, Consumer<TextChannel> callback) {
        // Creates an intermediate action which only contains the name of the new text channel.
        ChannelAction tcAction = ev.getGuild().getController().createTextChannel(Utils.ircify("tdc-" + name))
                .addPermissionOverride(ev.getGuild().getPublicRole(),
                        Permission.getRaw(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY),
                        0
                ).addPermissionOverride(ev.getGuild().getSelfMember(),
                        Permission.getRaw(Permission.MESSAGE_HISTORY, Permission.MANAGE_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE),
                        0
                );

        applyBlacklist(ev.getGuild(), tcAction, Permission.MESSAGE_READ);

        // Finally construct the category object.
        // This construction is split so it only requires one request instead of multiple.
        tcAction.setParent(parent)
                .reason("A new temporary channel was issued by " + Utils.userAsString(ev.getAuthor()))
                .queue((tc) -> {
                    if (callback != null) {
                        callback.accept(((TextChannel) tc));
                    }
                });
    }

    /**
     * Creates a temporary voice channel.
     *
     * @param ev       The event which caused the new channel to be created.
     * @param name     The name to give the channel.
     * @param parent   The category to house the new channel in.
     * @param callback The action to perform after creating this voice channel.
     */
    public void createTempVoiceChannel(GuildMessageReceivedEvent ev, String name, Category parent, Consumer<VoiceChannel> callback) {
        // Creates an intermediate action which only contains the name of the new voice channel.
        ChannelAction vcAction = ev.getGuild().getController().createVoiceChannel("[T] " + name)
                .addPermissionOverride(ev.getGuild().getPublicRole(),
                        Permission.getRaw(Permission.VIEW_CHANNEL),
                        0);

        applyBlacklist(ev.getGuild(), vcAction, Permission.VIEW_CHANNEL);

        // Finally construct the category object.
        // This construction is split so it only requires one request instead of multiple.
        vcAction.setParent(parent)
                .reason("A new temporary channel was issued by " + Utils.userAsString(ev.getAuthor()))
                .queue((vc) -> {
                    if (callback != null) {
                        callback.accept(((VoiceChannel) vc));
                    }
                });
    }

    /**
     * Deletes a channel that was once linked.
     * If it was and not made permanent it was temporary and can therefore be deleted when obsolete.
     *
     * @param c The channel to remove
     * @return The success of the operation
     */
    private static boolean deleteLinkedChannel(Channel c) {
        if (c != null) {
            c.delete().reason("This temporary channel had no people left in it").queue();

            return true;
        } else {
            return false;
        }
    }

    /**
     * Deletes a set of linked channels and, if necessary, its parent as well.
     *
     * @param vc  The voice channel to remove. The linked channels are stored with {@link VoiceChannel} as key.
     * @param ses The timeout executor to shut down
     */
    private void deleteLinkedChannels(VoiceChannel vc, @Nullable ScheduledExecutorService ses) {
        // If any timeout executor is present, shut it down
        if (ses != null) {
            ses.shutdown();
        }

        // Try to remove all channels involved
        boolean cleaned = deleteLinkedChannel(vc) &&
                deleteLinkedChannel(linkedChannels.get(vc)) &&
                ((categories.containsKey(vc.getGuild()) && categories.get(vc.getGuild()) != null) || deleteLinkedChannel(vc.getParent()));

        // Log feedback
        if (cleaned) {
            Logger.log("Successfully cleaned up group " + vc.getName() + "!");
        } else {
            Logger.log("Something went wrong cleaning up group " + vc.getName() + "...");
        }
    }

    /**
     * Deletes a set of linked channels and, if necessary, its parent as well.
     *
     * @param vc  The voice channel to remove. The linked channels are stored with {@link VoiceChannel} as key.
     */
    private void deleteLinkedChannels(VoiceChannel vc) {
        deleteLinkedChannels(vc, null);
    }

    /**
     * @return The categories saved for every guild.
     */
    public HashMap<Guild, Category> getCategories() {
        return categories;
    }

    /**
     * @return The pairs of channels linked to each other.
     */
    public HashMap<VoiceChannel, TextChannel> getLinkedChannels() {
        return linkedChannels;
    }

    @SuppressWarnings("unchecked")
    WeakHashMap<VoiceChannel, TextChannel> getLinkedChannelsForGuild(Guild g) {
        return new WeakHashMap<>(linkedChannels.entrySet().stream()
                .filter(entry -> entry.getKey().getGuild().equals(g))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * @return The pairs of channels which should be kept, even when left empty.
     */
    public HashMap<VoiceChannel, TextChannel> getPermChannels() {
        return permChannels;
    }

    @SuppressWarnings("unchecked")
    WeakHashMap<VoiceChannel, TextChannel> getPermChannelsForGuild(Guild g) {
        return new WeakHashMap<>(permChannels.entrySet().stream()
                .filter(entry -> entry.getKey().getGuild().equals(g))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Informs the users about an event that happened.
     * This usually relates to voice channel activity.
     *
     * @param type   The type of event to inform about
     * @param member The member who triggered the event
     * @param vcs    The voice channel(s) in question
     */
    private void informUserAbout(EventType type, Member member, VoiceChannel... vcs) {
        switch (type) {
            case JOIN:
                if (isLinked(vcs[0])) {
                    linkedChannels.get(vcs[0]).sendMessage("**" + member.getEffectiveName() + "** joined the voice channel").queue();
                }
                break;
            case LEAVE:
                if (isLinked(vcs[0])) {
                    linkedChannels.get(vcs[0]).sendMessage("**" + member.getEffectiveName() + "** left the voice channel").queue();
                }
                break;
            case MOVE:
                if (isLinked(vcs[0])) {
                    linkedChannels.get(vcs[0]).sendMessage("**" + member.getEffectiveName() + "** moved in from **" + vcs[1].getName() + "**").queue();
                }
                if (isLinked(vcs[1])) {
                    linkedChannels.get(vcs[1]).sendMessage("**" + member.getEffectiveName() + "** moved out to **" + vcs[0].getName() + "**").queue();
                }
        }
    }

    /**
     * Checks whether a voice channel is marked as linked.
     *
     * @param vc The voice channel to check
     * @return True if the channel is marked as linked by the bot
     * @see #linkedChannels
     */
    public boolean isLinked(VoiceChannel vc) {
        return linkedChannels.containsKey(vc);
    }

    /**
     * Checks whether a voice channel is marked as permanent.
     *
     * @param tc The text channel to check
     * @return True if the channel is marked as linked by the bot
     * @see #linkedChannels
     */
    private boolean isLinked(TextChannel tc) {
        return linkedChannels.containsValue(tc);
    }

    /**
     * Checks whether a voice channel is marked as permanent.
     *
     * @param vc The voice channel to check
     * @return True if the channel is marked as permanent by the bot
     * @see #permChannels
     */
    public boolean isPermanent(VoiceChannel vc) {
        return permChannels.containsKey(vc);
    }

    /**
     * Checks whether a voice channel is marked as permanent.
     *
     * @param tc The voice channel to check
     * @return True if the channel is marked as permanent by the bot
     * @see #permChannels
     */
    private boolean isPermanent(TextChannel tc) {
        return permChannels.containsValue(tc);
    }

    /**
     * Fired when a member joins a voice channel.
     *
     * @param ev The event thrown after the joining has occurred
     */
    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent ev) {
        respondToJoin(ev.getChannelJoined(), ev.getMember());

        informUserAbout(EventType.JOIN, ev.getMember(), ev.getChannelJoined());
    }

    /**
     * Fired when a member moves between voice channels.
     *
     * @param ev The event thrown after the moving has occurred
     */
    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent ev) {
        respondToMove(ev.getChannelJoined(), ev.getChannelLeft(), ev.getMember());

        informUserAbout(EventType.MOVE, ev.getMember(), ev.getChannelJoined(), ev.getChannelLeft());
    }

    /**
     * Fired when a member leaves a voice channel.
     *
     * @param ev The event thrown after the leaving has occurred
     */
    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent ev) {
        respondToLeave(ev.getChannelLeft(), ev.getMember());

        informUserAbout(EventType.LEAVE, ev.getMember(), ev.getChannelLeft());
    }

    /**
     * Fired when a voice channel is deleted.
     *
     * @param ev The event thrown after a deletion has happened
     */
    @Override
    public void onVoiceChannelDelete(VoiceChannelDeleteEvent ev) {
        VoiceChannel vc = ev.getChannel();

        if (isPermanent(vc)) {
            Logger.log(String.format("Removing \"%s\" from list of permanent channels", vc.getName()));
            Logger.log(String.format("Removing \"#%s\" from list of permanent channels", permChannels.get(vc).getName()));
            permChannels.remove(vc);
        }

        if (isLinked(vc)) {
            Logger.log(String.format("Removing \"%s\" from list of linked channels", vc.getName()));
            Logger.log(String.format("Removing \"#%s\" from list of linked channels", linkedChannels.get(vc).getName()));
            linkedChannels.remove(vc);
        }
    }

    /**
     * Fired when a text channel is deleted.
     *
     * @param ev The event thrown after a deletion has happened
     */
    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent ev) {
        TextChannel tc = ev.getChannel();
        VoiceChannel vc = permChannels.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getKey(), tc))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        if (vc != null) {
            if (isPermanent(tc)) {
                Logger.log("Removing \"" + vc.getName() + "\" from list of permanent channels");
                Logger.log("Removing \"#" + tc.getName() + "\" from list of permanent channels");
                permChannels.remove(vc);
            }

            if (isLinked(tc)) {
                Logger.log("Removing \"" + vc.getName() + "\" from list of linked channels");
                Logger.log("Removing \"#" + tc.getName() + "\" from list of linked channels");
                linkedChannels.remove(vc);
            }
        }
    }

    /**
     * Fired when a member joins a voice channel.
     *
     * @param vc The voice channel which was joined
     * @param m  The member joining a voice channel
     */
    private void respondToJoin(VoiceChannel vc, Member m) {
        // If a shutdown was still scheduled, cancel it.
        // If a user joined the channel, it should be kept alive.
        if (timeoutCandidates.containsKey(vc)) {
            timeoutCandidates.get(vc).shutdown();
        }

        // If this is a linked channel, the member should have reading permissions within the text channel
        // should the voice channel be limited.
        if (isLinked(vc)) {
            if (Utils.hasLimit(vc)) {
                Utils.getPO(linkedChannels.get(vc), m,
                        po -> po.getManagerUpdatable()
                                .grant(Permission.MESSAGE_READ)
                                .update()
                                .reason("The channel was joined and is limited, requiring members to see the linked text channel")
                                .queue());
            }
        }
    }


    /**
     * Fired when a member joins a voice channel.
     *
     * @param vcj The voice channel which was joined
     * @param vcl The voice channel which was left
     * @param m  The member joining a voice channel
     */
    private void respondToMove(VoiceChannel vcj, VoiceChannel vcl, Member m) {
        // Moving corresponds to leaving and joining at the same time, hence do both actions
        respondToJoin(vcj, m);
        respondToLeave(vcl, m);
    }

    /**
     * Fired when a member leave a voice channel.
     *
     * @param vc The voice channel which was left
     * @param m  The member leaving a voice channel
     */
    private void respondToLeave(VoiceChannel vc, Member m) {
        // If a shutdown was still scheduled, cancel it.
        // If a user joined the channel, it should be kept alive.
        if (vc.getMembers().size() == 0 && isLinked(vc) && !isPermanent(vc)) {
            deleteLinkedChannels(vc);
        }

        // If this is a linked channel, the member should not have reading permissions within the text channel
        // should the voice channel be limited.
        if (isLinked(vc)) {
            if (Utils.hasLimit(vc)) {
                Utils.getPO(linkedChannels.get(vc), m,
                        po -> Utils.removePermissionsFrom(po,
                                "The channel was left and is limited, requiring non-members to not see the associated text channel",
                                Permission.MESSAGE_READ));
            }
        }
    }

    /**
     * Creates a new timeout for the specified channel.
     * When the timeout runs out, the channel will be deleted along with the dedicated channels.
     *
     * @param vc The voice channel to create the timeout with
     */
    public void setNewChannelTimeout(VoiceChannel vc) {
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        timeoutCandidates.put(vc, ses);

        ses.scheduleWithFixedDelay(() -> deleteLinkedChannels(vc, ses), 10, 10, TimeUnit.SECONDS);
    }
}
