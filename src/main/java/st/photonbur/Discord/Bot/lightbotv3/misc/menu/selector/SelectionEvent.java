package st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Events of this type are created whenever a selection menu has had a decision made by the user.
 *
 * @param <T> The type of result to expect out of the menu
 */
public class SelectionEvent<T> {
    /**
     * The index of the option the user picked.
     */
    private final int selectedIndex;
    /**
     * The list of options available to the user.
     */
    private final LinkedList<T> options;

    SelectionEvent(int selectedIndex, LinkedHashMap<String, T> options) {
        this.selectedIndex = selectedIndex;
        this.options = options == null ? null : new LinkedList<>(options.values());
    }

    /**
     * Figures out what option the user actually chose for.
     *
     * @return The option chosen by the user
     */
    public T getSelectedOption() {
        return selectedIndex == -1 ? null : options.get(selectedIndex);
    }

    /**
     * @return True if the selection wasn't cancelled
     */
    public boolean selectionWasMade() {
        return selectedIndex >= 0 && options != null;
    }
}
