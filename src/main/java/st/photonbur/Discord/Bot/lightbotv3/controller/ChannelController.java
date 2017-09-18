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
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.ChannelAction;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ChannelController extends ListenerAdapter {
    private enum EventType {
        LEAVE("left"), JOIN("joined"), MOVE("");

        private final String verb;

        EventType(String verb) {
            this.verb = verb;
        }

        public String getVerb() {
            return this.verb;
        }
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
     * Creates a temporary category to house a fresh set of temporary channels in.
     *
     * @param g    The guild to put the new category in.
     * @param name The name to give the category.
     * @return The category created by this method.
     */
    public Category createTempCategory(Guild g, String name) {
        // Creates an intermediate action which only contains the name of the new category.
        ChannelAction cAction = g.getController().createCategory("Temp: " + name);

        applyBlacklist(g, cAction, Permission.MESSAGE_READ);

        // Finally construct the category object.
        // This construction is split so it only requires one request instead of multiple.
        Category c = (Category) cAction
                .setParent(categories.get(g))
                .reason("A new set of temporary channels was issued, needing an new category as no default was specified")
                .complete();

        Logger.log("Added temp category:\n" +
                " - id: " + c.getId() + "\n" +
                " - name: \"" + c.getName() + "\""
        );

        return c;
    }

    /**
     * Creates a temporary text channel.
     *
     * @param ev     The event which caused the new channel to be created.
     * @param name   The name to give the channel.
     * @param parent The category to house the new channel in.
     * @return The category created by this method.
     */
    public TextChannel createTempTextChannel(GuildMessageReceivedEvent ev, String name, Category parent) throws RateLimitedException {
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
        TextChannel tc = (TextChannel) tcAction
                .setParent(parent)
                .reason("A new temporary channel was issued by " + Utils.userAsString(ev.getAuthor()))
                .complete();

        Logger.log("Added temp text channel:\n" +
                " - id: " + tc.getId() + "\n" +
                " - name: \"" + tc.getName() + "\""
        );

        return tc;
    }

    /**
     * Creates a temporary voice channel.
     *
     * @param ev     The event which caused the new channel to be created.
     * @param name   The name to give the channel.
     * @param parent The category to house the new channel in.
     * @return The category created by this method.
     */
    public VoiceChannel createTempVoiceChannel(GuildMessageReceivedEvent ev, String name, Category parent) throws RateLimitedException {
        // Creates an intermediate action which only contains the name of the new voice channel.
        ChannelAction vcAction = ev.getGuild().getController().createVoiceChannel("[T] " + name)
                .addPermissionOverride(ev.getGuild().getPublicRole(),
                        Permission.getRaw(Permission.MESSAGE_READ, Permission.VOICE_CONNECT),
                        0);

        applyBlacklist(ev.getGuild(), vcAction, Permission.MESSAGE_READ, Permission.VOICE_CONNECT);

        // Finally construct the category object.
        // This construction is split so it only requires one request instead of multiple.
        VoiceChannel vc = (VoiceChannel) vcAction
                .setParent(parent)
                .reason("A new temporary channel was issued by " + Utils.userAsString(ev.getAuthor()))
                .complete();

        Logger.log("Added temp voice channel:\n" +
                "  id: " + vc.getId() + "\n" +
                "  name: \"" + vc.getName() + "\""
        );

        return vc;
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

            Logger.log("Deleted temporary channel:\n" +
                    " - id: " + c.getId() + "\n" +
                    " - name: \"" + c.getName() + "\"\n" +
                    " - type: " + c.getClass().getSimpleName().replace("Impl", "")
            );

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
     * Fetches all channel pairs which should be linked together.
     * This both validates the channel pairs stored in the channel file and gathers pairs of still-active temporary channels.
     * @return The pairing of all linked channels
     */
    private Map<VoiceChannel, TextChannel> fetchChannelPairs() {
        Logger.log("Fetching channel pairs...");

        Map<VoiceChannel, TextChannel> ids = new HashMap<>();

        // Validate stored channels
        permChannels.forEach((vc, tc) -> {
            if (vc != null && tc != null) {
                if (tc.canTalk() && vc.getGuild().equals(tc.getGuild())) {
                    ids.put(vc, tc);
                }
            }
        });

        // Find already active temporary channels
        l.getBot().getVoiceChannels().stream().filter(vc -> vc.getName().startsWith("[T] ")).forEach(vc -> {
            List<TextChannel> tcl = vc.getGuild().getTextChannelsByName(Utils.ircify("tdc-" + vc.getName().substring(4)), true);

            if (tcl.size() != 0) {
                ids.put(vc, tcl.get(0));
            }
        });

        // Log the found pairs
        Logger.log("Found channel pairs: " + ids.size() + "\n" + Utils.getFetchedChannelMapAsString(ids));

        return ids;
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

    /**
     * @return The pairs of channels which should be kept, even when left empty.
     */
    public HashMap<VoiceChannel, TextChannel> getPermChannels() {
        return permChannels;
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
                if (linkedChannels.containsKey(vcs[0])) {
                    linkedChannels.get(vcs[0]).sendMessage("**" + member.getEffectiveName() + "** joined the voice channel").queue();
                }
                break;
            case LEAVE:
                if (linkedChannels.containsKey(vcs[0])) {
                    linkedChannels.get(vcs[0]).sendMessage("**" + member.getEffectiveName() + "** left the voice channel").queue();
                }
                break;
            case MOVE:
                if (linkedChannels.containsKey(vcs[0])) {
                    linkedChannels.get(vcs[0]).sendMessage("**" + member.getEffectiveName() + "** moved in from **" + vcs[1].getName() + "**").queue();
                }
                if (linkedChannels.containsKey(vcs[1])) {
                    linkedChannels.get(vcs[1]).sendMessage("**" + member.getEffectiveName() + "** moved out to **" + vcs[0].getName() + "**").queue();
                }
        }
    }

    /**
     * Logs an event to the logs.
     *
     * @param type   The type of event to log
     * @param member The member who triggered the event
     * @param vcs    The voice channel(s) in question
     */
    private void logEvent(@Nullable EventType type, Member member, VoiceChannel... vcs) {
        String message = "";

        if (type != null) {
            int amountOfMembers = vcs[0].getMembers().size();

            message += "\n" +
                    member.getEffectiveName() + " " + type.getVerb() + " channel \"" + vcs[0].getName() + "\" in \"" + vcs[0].getGuild().getName() + "\".\n" +
                    "There " + (amountOfMembers == 1 ? "is" : "are") + " now " + amountOfMembers + " member" + (amountOfMembers == 1 ? "" : "s") + " in the channel.\n";
        } else {
            int[] amountOfMembers = new int[vcs.length];
            for (int i = 0; i < vcs.length; i++) {
                amountOfMembers[i] = vcs[i].getMembers().size();
            }

            message += "\n" +
                    member.getEffectiveName() + " moved from \"" + vcs[0].getName() + "\" to \"" + vcs[1].getName() + "\" in \"" + vcs[0].getGuild().getName() + "\".\n" +
                    "There " + (amountOfMembers[0] == 1 ? "is" : "are") + " now " + amountOfMembers[0] + " member" + (amountOfMembers[0] == 1 ? "" : "s") + " in the left channel\n" +
                    "  and " + amountOfMembers[1] + " member" + (amountOfMembers[1] == 1 ? "" : "s") + " in the joined channel.\n";
        }

        Logger.log(message);
    }

    /**
     * Fired when a member joins a voice channel.
     *
     * @param ev The event thrown after the joining has occurred
     */
    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent ev) {
        logEvent(EventType.JOIN, ev.getMember(), ev.getChannelJoined());
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
        logEvent(null, ev.getMember(), ev.getChannelLeft(), ev.getChannelJoined());
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
        logEvent(EventType.LEAVE, ev.getMember(), ev.getChannelLeft());
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

        if (permChannels.containsKey(vc)) {
            Logger.log(String.format("Removing \"%s\" from list of permanent channels", vc.getName()));
            Logger.log(String.format("\nRemoving \"#%s\" from list of permanent channels\n", permChannels.get(vc).getName()));
            permChannels.remove(vc);
        }

        if (linkedChannels.containsKey(vc)) {
            Logger.log(String.format("\nRemoving \"%s\" from list of linked channels\n", vc.getName()));
            Logger.log(String.format("\nRemoving \"#%s\" from list of linked channels\n", linkedChannels.get(vc).getName()));
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
            if (permChannels.containsValue(tc)) {
                Logger.log("\nRemoving \"" + vc.getName() + "\" from list of permanent channels\n");
                Logger.log("\nRemoving \"#" + tc.getName() + "\" from list of permanent channels\n");
                permChannels.remove(vc);
            }

            if (linkedChannels.containsValue(tc)) {
                Logger.log("\nRemoving \"" + vc.getName() + "\" from list of linked channels\n");
                Logger.log("\nRemoving \"#" + tc.getName() + "\" from list of linked channels\n");
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
        if (linkedChannels.containsKey(vc)) {
            if (Utils.hasLimit(vc)) {
                try {
                    Utils.getPO(linkedChannels.get(vc), m).getManagerUpdatable().grant(Permission.MESSAGE_READ).update().reason("The channel was joined and is limited, requiring members to see the linked text channel").queue();
                } catch (RateLimitedException ex) {
                    ex.printStackTrace();
                }
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
        if (vc.getMembers().size() == 0 && linkedChannels.containsKey(vc) && !permChannels.containsKey(vc)) {
            deleteLinkedChannels(vc);
        }

        // If this is a linked channel, the member should not have reading permissions within the text channel
        // should the voice channel be limited.
        if (linkedChannels.containsKey(vc)) {
            if (Utils.hasLimit(vc)) {
                try {
                    Utils.removePermissionsFrom(Utils.getPO(linkedChannels.get(vc), m), "The channel was left and is limited, requiring non-members to not see the associated text channel", Permission.MESSAGE_READ);
                } catch (RateLimitedException ex) {
                    ex.printStackTrace();
                }
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
