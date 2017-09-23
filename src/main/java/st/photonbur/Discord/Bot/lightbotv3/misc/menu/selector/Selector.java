package st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector;

/**
 * This interface makes sure there is a proper interaction between caller of the selector menu and the menu itself.
 */
public interface Selector {
    /**
     * Fired whenever a decision has been made.
     * This could be either cancelling or not.
     *
     * @param selectionEvent The event fired when a decision has been made
     */
    void onSelection(SelectionEvent<?> selectionEvent);
}
