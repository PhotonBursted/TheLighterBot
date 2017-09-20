package st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Control;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Menu;

import java.util.LinkedList;

public class PaginatorImpl extends Menu {
    private static final String ITEM_SEPARATOR = "\\nNEWITM\\n";

    private final LinkedList<String> content;
    private int currPage;

    private final Paginator<?> parent;

    /**
     * Creates a new paginator targeting a {@link Message} and scrolling through the content.
     * This content will automatically be shrunk to fit within a message without surpassing the character limit.
     *
     * @param content The content to show in the paginator
     */
    public PaginatorImpl(Paginator<?> parent, LinkedList<String> content, Message message, Control... controls) {
        super(message, controls);
        this.content = parent.groupContent(content, ITEM_SEPARATOR);
        this.currPage = 0;
        this.parent = parent;

        setPage(currPage);
    }

    protected void destroy() {
        l.getBot().removeEventListener(this);
        message.delete().queue();
    }

    private void movePage(Control control) {
        if (control == null) return;

        if (control.isRelative()) {
            currPage += control.getOffset();
        } else {
            currPage = control.getOffset();
        }

        currPage = Math.floorMod(currPage, content.size());

        setPage(currPage);
    }

    protected void doActionWith(Control source) {
        movePage(source);
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
