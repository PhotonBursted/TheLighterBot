package st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Control;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Menu;

import java.util.LinkedList;

public class PaginatorImpl extends Menu {
    private static final String ITEM_SEPARATOR = "\nNEWITM\n";

    private final LinkedList<String> content;
    private int currPage;

    private final Paginator<?> parent;

    /**
     * Creates a new paginator targeting a {@link Message} and scrolling through the content.
     * The content will have to be supplied and displayed by the parent.
     *
     * @param parent   The instance of the object requesting to show this menu
     * @param content  The content to show in the paginator
     * @param message  The message to hook the menu into
     */
    PaginatorImpl(Control[] controls, LinkedList<String> content, Paginator<?> parent, Message message) {
        super(message, controls);
        this.content = parent.groupContent(content, ITEM_SEPARATOR);
        this.currPage = 0;
        this.parent = parent;

        if (message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
            renderPage(currPage);
        } else {
            DiscordController.getInstance().sendMessage(message.getChannel(),
                    "Since no " + Permission.MESSAGE_MANAGE.name() +
                    " permissions have been granted, the menu is displayed flattened," +
                    " and will delete in " + DiscordController.AUTOMATIC_REMOVAL_INTERVAL + " seconds.",
                    DiscordController.AUTOMATIC_REMOVAL_INTERVAL);

            for (int page = 0; page < this.content.size(); page++) {
                renderPage(page, false);
            }
        }
    }

    @Override
    protected void doActionWith(Control source) {
        movePage(source);
    }

    /**
     * Moves to a page when a certain control was clicked.
     *
     * @param control The control clicked
     */
    private void movePage(Control control) {
        if (control == null) return;

        // If the control has its offset specified as being relative, add the offset. Otherwise, set it
        if (control.isRelative()) {
            currPage += control.getOffset();
        } else {
            currPage = control.getOffset();
        }

        // Make sure the current page is within the range of the amount of pages
        currPage = Math.floorMod(currPage, content.size());

        renderPage(currPage);
    }

    /**
     * Renders the content from a specific page
     *
     * @param page The page to render
     */
    private void renderPage(int page) {
        renderPage(page, true);
    }

    private void renderPage(int page, boolean doEdit) {
        // Get the content supplied by the parent caller
        Object content = parent.constructMessage(this.content.get(page).split(ITEM_SEPARATOR), page + 1, this.content.size());

        if (doEdit) {
            // Try to edit the message if it is of the right type
            if (content instanceof String) {
                message.editMessage(((String) content)).queue(null, error -> DiscordController.MESSAGE_ACTION_FAIL.accept(error, message));
            } else if (content instanceof Message) {
                message.editMessage(((Message) content)).queue(null, error -> DiscordController.MESSAGE_ACTION_FAIL.accept(error, message));
            } else if (content instanceof MessageEmbed) {
                message.editMessage(((MessageEmbed) content)).queue(null, error -> DiscordController.MESSAGE_ACTION_FAIL.accept(error, message));
            }
        } else {
            if (content instanceof String) {
                DiscordController.getInstance().sendMessage(message.getChannel(), (String) content, DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
            } else if (content instanceof Message) {
                DiscordController.getInstance().sendMessage(message.getChannel(), (Message) content, DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
            } else if (content instanceof MessageEmbed) {
                DiscordController.getInstance().sendMessage(message.getChannel(), (MessageEmbed) content, DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
            }
        }
    }
}
