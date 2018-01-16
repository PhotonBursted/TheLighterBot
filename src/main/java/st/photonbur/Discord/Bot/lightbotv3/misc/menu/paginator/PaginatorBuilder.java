package st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Control;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.MenuBuilder;

import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Builder specifically for building Paginator type menus.
 *
 * @param <T> The type of the message to be displayed by the menu.
 */
public class PaginatorBuilder<T> extends MenuBuilder<Paginator<T>> {
    /**
     * List containing the content of all pages to be listed by this paginator.
     */
    private LinkedList<String> content;

    public PaginatorBuilder(Paginator<T> parent) {
        super(parent);

        if (CommandParser.getLastEvent().getGuild().getSelfMember().hasPermission(CommandParser.getLastEvent().getChannel(), Permission.MESSAGE_MANAGE)) {
            // Sets the controls this menu needs
            setControls(Control.PREV, Control.STOP, Control.NEXT);
            // Sets a default message for when this menu is loading
            setPlaceholderMessage("Building paginator...");
        } else {
            setControls();
            setPlaceholderMessage(PLACEHOLDER_DISABLED_STRING);
        }
    }

    @Override
    public void buildImpl() {
        Consumer<Message> createAction = message -> new PaginatorImpl(controls, content, parent, message);

        if (placeholderMessage != null) {
            // First sends a message, then uses that to hook the menu onto
            DiscordController.getInstance().sendMessage(CommandParser.getLastEvent(), placeholderMessage, createAction);
        } else {
            createAction.accept(CommandParser.getLastEvent().getMessage());
        }
    }

    /**
     * Sets the list of pages to display.
     * @param content The list of Strings containing the content within the pages.
     * @return This instance of the builder
     */
    public PaginatorBuilder<T> setContent(LinkedList<String> content) {
        this.content = content;
        return this;
    }
}
