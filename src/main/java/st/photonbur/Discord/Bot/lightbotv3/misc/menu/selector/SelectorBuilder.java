package st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector;

import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Control;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.MenuBuilder;

import java.util.LinkedHashMap;

/**
 * Builder specifically for building Selector type menus.
 *
 * @param <T> The type of the instances which are to be selected from
 */
public class SelectorBuilder<T> extends MenuBuilder<Selector> {
    /**
     * The options the selector will have.
     *
     * The keys are strings used as a description of the options - these will be used in displaying the options in the menu.
     * The values are of the generics type specified and will be the actual type to be returned by the selector.
     */
    private LinkedHashMap<String, T> options;

    public SelectorBuilder(Selector parent) {
        super(parent);

        // Sets the controls the menu will use
        setControls(Control.UP, Control.DOWN, Control.ACCEPT, Control.STOP);
        // Sets a default message while loading the menu
        setPlaceholderMessage("Building selector...");
    }

    @Override
    public void build() {
        // First, send a message.
        // As a callback, use the newly generated message to hook the new menu onto
        Launcher.getInstance().getDiscordController().sendMessage(CommandParser.getLastEvent(), placeholderMessage,
                (message -> new SelectorImpl<>(controls, options, parent, message)));
    }

    /**
     * Sets the options this menu will have to be selected from.
     *
     * @param options The map of options to have available within the menu
     * @return This instance of the builder
     * @see #options
     */
    public SelectorBuilder setOptionMap(LinkedHashMap<String, T> options) {
        this.options = options;
        return this;
    }
}
