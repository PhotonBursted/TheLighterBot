package st.photonbur.Discord.Bot.lightbotv3.controller;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;
import st.photonbur.Discord.Bot.lightbotv3.misc.map.CategoryMap;
import st.photonbur.Discord.Bot.lightbotv3.misc.map.channel.LinkedChannelMap;
import st.photonbur.Discord.Bot.lightbotv3.misc.map.channel.PermanentChannelMap;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static net.dv8tion.jda.core.Permission.MESSAGE_READ;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ChannelController extends ListenerAdapter {
    private enum EventType {
        LEAVE, JOIN, MOVE
    }

    private static final Logger log = LoggerFactory.getLogger(ChannelController.class);

    /**
     * Stores the default category specified per guild
     */
    private final CategoryMap categories;
    /**
     * Stores a timeout per voice channel.
     * Makes sure that, when a voice channel is created, it gets deleted when it is left unused.
     */
    private final HashMap<VoiceChannel, ScheduledExecutorService> timeoutCandidates;
    /**
     * Keeps track of channels which are linked to each other.
     * This "linkage" is referring to the displaying of join, leave and move actions in the text channel.
     */
    private final LinkedChannelMap linkedChannels;
    /**
     * Keeps track of pairs of channels which should not be removed when empty.
     */
    private final PermanentChannelMap permChannels;

    private static ChannelController instance;

    private ChannelController() {
        categories = new CategoryMap();
        linkedChannels = new LinkedChannelMap();
        permChannels = new PermanentChannelMap();
        timeoutCandidates = new HashMap<>();
    }

    public static synchronized ChannelController getInstance() {
        if (instance == null) {
            instance = new ChannelController();
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
     * Creates a temporary category to house a fresh set of temporary channels in.
     *
     * @param g        The guild to put the new category in.
     * @param name     The name to give the category.
     * @param callback The action to perform after creating this category.
     */
    public void createTempCategory(Guild g, String name, Consumer<Category> callback) {
        // Creates an intermediate action which only contains the name of the new category.
        ChannelAction cAction = g.getController().createCategory("Temp: " + name);

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
                .addPermissionOverride(ev.getGuild().getSelfMember(),
                        Permission.getRaw(Permission.MESSAGE_HISTORY, Permission.MANAGE_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE),
                        0
                );

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
                .addPermissionOverride(ev.getGuild().getSelfMember(),
                        Permission.getRaw(Permission.MANAGE_CHANNEL, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
                        0);

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
    private boolean deleteLinkedChannel(Channel c) {
        if (c instanceof TextChannel && linkedChannels.get(c).size() > 1) {
            return true;
        } else if (c != null) {
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
        boolean cleaned = ((categories.containsKey(vc.getGuild()) && categories.get(vc.getGuild()) != null) ||
                deleteLinkedChannel(vc.getParent()) &&
                deleteLinkedChannel(vc) && deleteLinkedChannel(linkedChannels.getForVoiceChannel(vc)));

        // Log feedback
        if (cleaned) {
            log.info("Successfully cleaned up group " + vc.getName() + "!");
        } else {
            log.warn("Something went wrong cleaning up group " + vc.getName() + "...");
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
    public CategoryMap getCategories() {
        return categories;
    }

    /**
     * @return The pairs of channels linked to each other.
     */
    public LinkedChannelMap getLinkedChannels() {
        return linkedChannels;
    }

    /**
     * @return The pairs of channels which should be kept, even when left empty.
     */
    public PermanentChannelMap getPermChannels() {
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
        TextChannel[] tcs = new TextChannel[] {
                vcs.length > 0 ? linkedChannels.getForVoiceChannel(vcs[0]) : null,
                vcs.length > 1 ? linkedChannels.getForVoiceChannel(vcs[1]) : null
        };

        switch (type) {
            case JOIN:
                if (isLinked(vcs[0])) {
                    tcs[0].sendMessage("**" + member.getEffectiveName() + "** joined " +
                            (isPermanent(vcs[0]) || linkedChannels.get(tcs[0]).size() > 1 ? String.format("**%s**", vcs[0].getName()) : "the voice channel")
                    ).queue();
                }
                break;
            case LEAVE:
                if (isLinked(vcs[0])) {
                    tcs[0].sendMessage("**" + member.getEffectiveName() + "** left "
                            + (isPermanent(vcs[0]) || linkedChannels.get(tcs[0]).size() > 1 ? String.format("**%s**", vcs[0].getName()) : "the voice channel")
                    ).queue();
                }
                break;
            case MOVE:
                if (Objects.equals(tcs[0], tcs[1])) {
                    if (isLinked(vcs[0])) {
                        tcs[0].sendMessage("**" + member.getEffectiveName() + "** moved from **" + vcs[0].getName() + "** to **" + vcs[1].getName() + "**").queue();
                    }
                } else {
                    if (isLinked(vcs[0])) {
                        tcs[0].sendMessage(String.format("**%s** moved in%s from **%s**",
                                member.getEffectiveName(),
                                linkedChannels.get(tcs[0]).size() > 1 ? "to " + vcs[0].getName() : "",
                                vcs[1].getName())).queue();
                    }
                    if (isLinked(vcs[1])) {
                        tcs[1].sendMessage(String.format("**%s** moved out%s to **%s**",
                                member.getEffectiveName(),
                                linkedChannels.get(tcs[1]).size() > 1 ? " from " + vcs[1].getName() : "",
                                vcs[0].getName())).queue();
                    }
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
        return linkedChannels.values().stream().anyMatch(set -> set.stream().anyMatch(tvc -> tvc.equals(vc)));
    }

    /**
     * Checks whether a voice channel is marked as permanent.
     *
     * @param tc The text channel to check
     * @return True if the channel is marked as linked by the bot
     * @see #linkedChannels
     */
    private boolean isLinked(TextChannel tc) {
        return linkedChannels.containsKey(tc);
    }

    /**
     * Checks whether a voice channel is marked as permanent.
     *
     * @param vc The voice channel to check
     * @return True if the channel is marked as permanent by the bot
     * @see #permChannels
     */
    public boolean isPermanent(VoiceChannel vc) {
        return permChannels.values().stream().anyMatch(set -> set.stream().anyMatch(tvc -> tvc.equals(vc)));
    }

    /**
     * Checks whether a voice channel is marked as permanent.
     *
     * @param tc The voice channel to check
     * @return True if the channel is marked as permanent by the bot
     * @see #permChannels
     */
    private boolean isPermanent(TextChannel tc) {
        return permChannels.containsKey(tc);
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
            permChannels.removeStoring(permChannels.getForVoiceChannel(vc), vc);
        }

        if (isLinked(vc)) {
            linkedChannels.removeByValueStoring(vc);
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
        Set<VoiceChannel> vcs = permChannels.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getKey(), tc))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);

        if (isPermanent(tc) && vcs != null) {
            for (VoiceChannel vc : vcs) {
                permChannels.removeByValueStoring(vc);
            }
        }

        if (isLinked(tc) && vcs != null) {
            for (VoiceChannel vc : vcs) {
                linkedChannels.removeByValueStoring(vc);
            }
        }
    }

    public void reset() {
        categories.clear();
        linkedChannels.clear();
        permChannels.clear();
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
                Utils.getPO(linkedChannels.getForVoiceChannel(vc), m,
                        po -> po.getManager()
                                .grant(MESSAGE_READ)
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
                Utils.getPO(linkedChannels.getForVoiceChannel(vc), m,
                        po -> Utils.removePermissionsFrom(po,
                                "The channel was left and is limited, requiring non-members to not see the associated text channel",
                                MESSAGE_READ));
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
