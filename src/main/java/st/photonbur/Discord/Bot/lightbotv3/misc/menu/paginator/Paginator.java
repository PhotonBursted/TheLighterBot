package st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator;

import java.util.LinkedList;

public interface Paginator<T> {
    T constructMessage(String[] contents, int currPage, int nPages);

    LinkedList<String> groupContent(LinkedList<String> contents, String itemSeparator);
}
