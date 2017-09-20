package st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Controls;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PaginatorImpl extends ListenerAdapter {

    private static final String ITEM_SEPARATOR = "\\nNEWITM\\n";
    private final Controls[] controls;

    private final ScheduledExecutorService messageTimeout;
    private ScheduledFuture<?> timeoutFuture;

    private final LinkedList<String> content;
    private int currPage;

    private final Message message;
    private final Launcher l;
    private final Paginator<?> parent;

    /**
     * Creates a new paginator targeting a {@link Message} and scrolling through the content.
     * This content will automatically be shrunk to fit within a message without surpassing the character limit.
     *
     * @param content The content to show in the paginator
     */
    public PaginatorImpl(LinkedList<String> content, Paginator<?> parent, Controls... controls) {
        this.parent = parent;

        this.content = parent.groupContent(content, ITEM_SEPARATOR);
        this.controls = controls;
        this.currPage = 0;
        this.l = Launcher.getInstance();
        this.message = l.getDiscordController().sendMessage(CommandParser.getLastEvent(), "Requesting help...");
        this.messageTimeout = new ScheduledThreadPoolExecutor(1);

        addControls();
        setPage(currPage);
    }

    private void addControl(int i) {
        if (i >= controls.length) {
            refreshTimeout();
            return;
        }

        message.addReaction(controls[i].getUnicode()).queue((success) -> addControl(i + 1));
    }

    private void addControls() {
        addControl(0);
    }

    private void destroy() {
        l.getBot().removeEventListener(this);
        message.delete().queue();
    }

    private void movePage(Controls controls) {
        if (controls == null) return;
        currPage += controls.getOffset();
        currPage = Math.floorMod(currPage, content.size());

        setPage(currPage);
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent ev) {
        if (ev.getUser().equals(l.getBot().getSelfUser())) return;

        ev.getReaction().removeReaction(ev.getUser()).queueAfter(50, TimeUnit.MILLISECONDS);

        Controls selectedOption = Arrays.stream(Controls.values())
                .filter(c -> c.getUnicode().equals(ev.getReaction().getEmote().getName()))
                .findFirst().orElse(null);
        if (selectedOption != null && selectedOption.equals(Controls.STOP)) {
            destroy();
        } else {
            movePage(selectedOption);
            refreshTimeout();
        }
    }

    private void refreshTimeout() {
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(true);
        }

        timeoutFuture = messageTimeout.schedule(() -> {
            destroy();
            messageTimeout.shutdown();
        }, DiscordController.AUTOMATIC_REMOVAL_INTERVAL, TimeUnit.SECONDS);
    }

    private void setPage(int page) {
        Object content = parent.constructMessage(this.content.get(page).split(ITEM_SEPARATOR), page + 1, this.content.size());

        if (content instanceof String) {
            message.editMessage(((String) content)).queue();
        } else if (content instanceof Message) {
            message.editMessage(((Message) content)).queue();
        } else if (content instanceof MessageEmbed) {
            message.editMessage(((MessageEmbed) content)).queue();
        }
    }
}
