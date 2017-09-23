package st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator;

import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Control;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.MenuBuilder;

import java.util.LinkedList;

public class PaginatorBuilder<T> extends MenuBuilder<Paginator<T>> {
    private LinkedList<String> content;

    public PaginatorBuilder(Paginator<T> parent) {
        super(parent);

        setControls(Control.PREV, Control.STOP, Control.NEXT);
        setPlaceholderMessage("Building paginator...");
    }

    @Override
    public void build() {
        Launcher.getInstance().getDiscordController().sendMessage(CommandParser.getLastEvent(), placeholderMessage,
                (message -> new PaginatorImpl(controls, content, parent, message)));
    }

    public PaginatorBuilder<T> setContent(LinkedList<String> content) {
        this.content = content;
        return this;
    }
}
