package st.photonbur.Discord.Bot.lightbotv3.misc.menu;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class Menu extends ListenerAdapter {
    private final Control[] controls;

    private final ScheduledExecutorService messageTimeout;
    protected final Message message;
    protected final Launcher l;
    private ScheduledFuture<?> timeoutFuture;

    public Menu(Message message, Control... controls) {
        this.controls = controls;
        this.message = message;
        this.messageTimeout = new ScheduledThreadPoolExecutor(1);
        this.l = Launcher.getInstance();

        addControls();
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

    protected abstract void destroy();

    protected abstract void doActionWith(Control control);

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent ev) {
        if (ev.getUser().equals(l.getBot().getSelfUser())) return;

        ev.getReaction().removeReaction(ev.getUser()).queueAfter(50, TimeUnit.MILLISECONDS);

        Control selectedOption = Arrays.stream(Control.values())
                .filter(c -> c.getUnicode().equals(ev.getReaction().getEmote().getName()))
                .findFirst().orElse(null);

        if (selectedOption != null) {
            if (selectedOption.equals(Control.STOP)) {
                destroy();
            } else {
                doActionWith(selectedOption);
                refreshTimeout();
            }
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
}
