package st.photonbur.Discord.Bot.lightbotv3.controller;

import com.sun.istack.internal.Nullable;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ChannelController extends ListenerAdapter {
    private static final HashMap<Guild, Category> categories;
    private static final HashMap<VoiceChannel, ScheduledExecutorService> timeoutCandidates;
    private static final HashMap<VoiceChannel, TextChannel> linkedChannels, permChannels;

    static {
        categories = new HashMap<>();
        linkedChannels = new HashMap<>();
        permChannels = new HashMap<>();
        timeoutCandidates = new HashMap<>();
    }

    private final Launcher l;

    public ChannelController(Launcher l) {
        this.l = l;
    }

    public static HashMap<Guild, Category> getCategories() {
        return categories;
    }

    public static HashMap<VoiceChannel, TextChannel> getLinkedChannels() {
        return linkedChannels;
    }

    public static HashMap<VoiceChannel, TextChannel> getPermChannels() {
        return permChannels;
    }

    public static Category createTempCategory(Guild g, String channelName) {
        ChannelAction cAction = g.getController().createCategory("Temp: " + channelName);

        Set<? extends ISnowflake> blacklist = BlacklistController.getForGuild(g);
        if (blacklist != null) {
            blacklist.forEach(item -> {
                if (item instanceof Role) {
                    cAction.addPermissionOverride((Role) item, 0L, Permission.getRaw(Permission.MESSAGE_READ));
                } else if (item instanceof User) {
                    cAction.addPermissionOverride(g.getMember((User) item), 0L, Permission.getRaw(Permission.MESSAGE_READ));
                }
            });
        }

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

    public static TextChannel createTempTextChannel(GuildMessageReceivedEvent ev, String name, Category parent) throws RateLimitedException {
        ChannelAction tcAction = ev.getGuild().getController().createTextChannel(Utils.ircify("tdc-" + name))
                .addPermissionOverride(ev.getGuild().getPublicRole(),
                        Permission.getRaw(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY),
                        0
                ).addPermissionOverride(ev.getGuild().getSelfMember(),
                        Permission.getRaw(Permission.MESSAGE_HISTORY, Permission.MANAGE_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE),
                        0
                );

        // SHOULD BE REMOVED ONCE CATEGORIES ARE PUBLIC!!!
        Set<? extends ISnowflake> blacklist = BlacklistController.getForGuild(ev.getGuild());
        if (blacklist != null) {
            blacklist.forEach(item -> {
                if (item instanceof Role) {
                    tcAction.addPermissionOverride((Role) item, 0L, Permission.getRaw(Permission.MESSAGE_READ));
                } else if (item instanceof User) {
                    tcAction.addPermissionOverride(ev.getGuild().getMember((User) item), 0L, Permission.getRaw(Permission.MESSAGE_READ));
                }
            });
        }
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

    // Create a temporary voice channel and notify the user of it.
    public static VoiceChannel createTempVoiceChannel(GuildMessageReceivedEvent ev, String name, Category parent) throws RateLimitedException {
        ChannelAction vcAction = ev.getGuild().getController().createVoiceChannel("[T] " + name)
                .addPermissionOverride(ev.getGuild().getPublicRole(),
                        Permission.getRaw(Permission.MESSAGE_READ, Permission.VOICE_CONNECT),
                        0);

        // SHOULD BE REMOVED ONCE CATEGORIES ARE PUBLIC!!!
        Set<? extends ISnowflake> blacklist = BlacklistController.getForGuild(ev.getGuild());
        if (blacklist != null) {
            blacklist.forEach(item -> {
                if (item instanceof Role) {
                    vcAction.addPermissionOverride((Role) item, 0L, Permission.getRaw(Permission.VOICE_CONNECT, Permission.MESSAGE_READ));
                } else if (item instanceof User) {
                    vcAction.addPermissionOverride(ev.getGuild().getMember((User) item), 0L, Permission.getRaw(Permission.VOICE_CONNECT, Permission.MESSAGE_READ));
                }
            });
        }

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

    private static void deleteLinkedChannels(VoiceChannel vc, @Nullable ScheduledExecutorService ses) {
        if (ses != null) {
            ses.shutdown();
        }

        boolean cleaned = deleteLinkedChannel(vc) &&
                deleteLinkedChannel(linkedChannels.get(vc)) &&
                ((categories.containsKey(vc.getGuild()) && categories.get(vc.getGuild()) != null) || deleteLinkedChannel(vc.getParent()));

        if (cleaned) {
            Logger.log("Successfully cleaned up group " + vc.getName() + "!");
        } else {
            Logger.log("Something went wrong cleaning up group " + vc.getName() + "...");
        }
    }

    public static void setNewChannelTimeout(VoiceChannel vc) {
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        timeoutCandidates.put(vc, ses);

        ses.scheduleWithFixedDelay(() -> deleteLinkedChannels(vc, ses), 10, 10, TimeUnit.SECONDS);
    }

    private void deleteLinkedChannels(VoiceChannel vc) {
        deleteLinkedChannels(vc, null);
    }

    private Map<VoiceChannel, TextChannel> fetchChannels() {
        Logger.log("Fetching permanent channel pairs...");

        Map<VoiceChannel, TextChannel> ids = new HashMap<>();

        permChannels.forEach((vc, tc) -> {
            if (vc != null && tc != null) {
                if (tc.canTalk() && vc.getGuild().equals(tc.getGuild())) {
                    ids.put(vc, tc);
                }
            }
        });

        l.getBot().getVoiceChannels().stream().filter(vc -> vc.getName().startsWith("[T] ")).forEach(vc -> {
            List<TextChannel> tcl = vc.getGuild().getTextChannelsByName(Utils.ircify("tdc-" + vc.getName().substring(4)), true);

            if (tcl.size() != 0) {
                ids.put(vc, tcl.get(0));
            }
        });

        Logger.log("Found permanent channel pairs: " + ids.size() + "\n" + Utils.getFetchedChannelMapAsString(ids));

        return ids;
    }

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

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent ev) {
        logEvent(EventType.JOIN, ev.getMember(), ev.getChannelJoined());
        respondToJoin(ev.getChannelJoined(), ev.getMember());

        informUserAbout(EventType.JOIN, ev.getMember(), ev.getChannelJoined());
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent ev) {
        logEvent(null, ev.getMember(), ev.getChannelLeft(), ev.getChannelJoined());
        respondToMove(ev.getChannelJoined(), ev.getChannelLeft(), ev.getMember());

        informUserAbout(EventType.MOVE, ev.getMember(), ev.getChannelJoined(), ev.getChannelLeft());
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent ev) {
        logEvent(EventType.LEAVE, ev.getMember(), ev.getChannelLeft());
        respondToLeave(ev.getChannelLeft(), ev.getMember());

        informUserAbout(EventType.LEAVE, ev.getMember(), ev.getChannelLeft());
    }

    private void respondToJoin(VoiceChannel vc, Member m) {
        if (timeoutCandidates.containsKey(vc)) {
            timeoutCandidates.get(vc).shutdown();
        }

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

    private void respondToMove(VoiceChannel vcj, VoiceChannel vcl, Member m) {
        respondToJoin(vcj, m);
        respondToLeave(vcl, m);
    }

    private void respondToLeave(VoiceChannel vc, Member m) {
        if (vc.getMembers().size() == 0 && linkedChannels.containsKey(vc) && !permChannels.containsKey(vc)) {
            deleteLinkedChannels(vc);
        }

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
}
