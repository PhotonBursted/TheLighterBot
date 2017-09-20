package st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator;

import java.util.LinkedList;

/**
 * This is to be implemented by any class using a Paginator as selection device.
 *
 * @param <T> The type of message to return. This should be limited to {@link String}, {@link net.dv8tion.jda.core.entities.Message} or {@link net.dv8tion.jda.core.entities.MessageEmbed}.
 */
public interface Paginator<T> {
    /**
     * Constructs a message to display inside of the Paginator.
     *
     * @param contents The contents of the page to display
     * @param currPage The current page number
     * @param nPages   The total amount of pages
     * @return The message to edit the paginator towards
     */
    T constructMessage(String[] contents, int currPage, int nPages);

    /**
     * Groups the contents up into pages.
     *
     * @param contents      The contents to divide into groups
     * @param itemSeparator The separator used to divide items inside of groups
     * @return The list of grouped up items
     */
    LinkedList<String> groupContent(LinkedList<String> contents, String itemSeparator);
}
