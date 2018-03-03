package st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector;

import net.dv8tion.jda.core.entities.Message;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Control;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Menu;

import java.util.LinkedHashMap;

/**
 * Implementation of the selector type menu
 *
 * @param <T> The type of the options to select from
 */
public class SelectorImpl<T> extends Menu {
    /**
     * The index the user currently has selected.
     */
    private int selectedIndex = 0;
    /**
     * The parent of the menu. This is the instance which is responsible for actually doing something with anything happening within the menu.
     */
    private final Selector<T> parent;
    /**
     * The options available within this menu.
     */
    private final LinkedHashMap<String, T> options;

    SelectorImpl(Control[] controls, LinkedHashMap<String, T> options, Selector<T> parent, Message message) {
        super(message, controls);
        this.options = options;
        this.parent = parent;

        render();
    }

    @Override
    protected void destroy() {
        destroy(true);
    }

    private void destroy(boolean cancelled) {
        super.destroy();

        if (cancelled) {
            parent.onSelection(new SelectionEvent<>(-1, null));
        }
    }

    @Override
    protected void doActionWith(Control control) {
        switch (control) {
            case UP:
            case DOWN:
                moveSelector(control);
                break;
            case ACCEPT:
                parent.onSelection(new SelectionEvent<>(selectedIndex, options));
                destroy(false);
                break;
            case STOP:
                destroy(true);
                break;
        }
    }

    private void moveSelector(Control control) {
        if (control == null) return;

        selectedIndex += control.getOffset();
        selectedIndex = Math.min(Math.max(0, selectedIndex), options.size() - 1);

        render();
    }

    private void render() {
        int option = 1;

        StringBuilder sb = new StringBuilder("Make a selection out of the following options:\n\n");
        for (String optionDesc : options.keySet()) {
            sb.append(String.format(" " + (option == selectedIndex + 1 ? Control.SELECTED.getUnicode() : Control.NOT_SELECTED.getUnicode()) + " %d. %s\n", option++, optionDesc));
        }
        sb.append("\n\nUse the controls below to select an option.");

        message.editMessage(sb.toString()).queue(null, error -> DiscordController.MESSAGE_ACTION_FAIL.accept(error, message));
    }
}
