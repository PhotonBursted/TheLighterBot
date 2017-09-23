package st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector;

import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Control;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.MenuBuilder;

import java.util.LinkedHashMap;

public class SelectorBuilder<T> extends MenuBuilder<Selector> {
    private LinkedHashMap<String, T> options;

    public SelectorBuilder(Selector parent) {
        super(parent);

        setControls(Control.UP, Control.DOWN, Control.ACCEPT, Control.STOP);
        setPlaceholderMessage("Building selector...");
    }

    @Override
    public void build() {
        Launcher.getInstance().getDiscordController().sendMessage(CommandParser.getLastEvent(), placeholderMessage,
                (message -> new SelectorImpl<>(controls, options, parent, message)));
    }

    public SelectorBuilder setOptionMap(LinkedHashMap<String, T> options) {
        this.options = options;
        return this;
    }
}
