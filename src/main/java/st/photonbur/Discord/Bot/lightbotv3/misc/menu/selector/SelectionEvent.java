package st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class SelectionEvent<T> {
    private final int selectedIndex;
    private final LinkedList<T> options;

    SelectionEvent(int selectedIndex, LinkedHashMap<String, T> options) {
        this.selectedIndex = selectedIndex;
        this.options = options == null ? null : new LinkedList<>(options.values());
    }

    public T getSelectedOption() {
        return selectedIndex == -1 ? null : options.get(selectedIndex);
    }

    public boolean selectionWasMade() {
        return selectedIndex >= 0 && options != null;
    }
}
