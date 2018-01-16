package st.photonbur.Discord.Bot.lightbotv3.misc.menu;

/**
 * Used as a template for building menu implementations.
 * This to clean up menu creation and to set a good foundation for any variants of this.
 *
 * @param <T> The type of the parent. This will serve as a 1-on-1 event listener setup
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class MenuBuilder<T> {
    protected static final String PLACEHOLDER_DISABLED_STRING = ">?DISABLED?<";

    /**
     * The message to display while the menu is loading
     */
    protected String placeholderMessage;
    /**
     * The controls to add to this menu with which interation will be carried out
     */
    protected Control[] controls;
    /**
     * The instance from which this menu is created.
     * This instance will be responsible for handling events happening within the menu.
     */
    protected T parent;

    protected MenuBuilder(T parent) {
        // Set the parent responsible for dealing with events
        setParent(parent);
    }

    /**
     * Builds the menu.
     */
    public void build() {
        if (placeholderMessage.equals(PLACEHOLDER_DISABLED_STRING)) {
            placeholderMessage = null;
        }

        buildImpl();
    }

    /**
     * Builds the menu.
     * Abstract since every implementation will a different constructor and different needs.
     */
    protected abstract void buildImpl();

    /**
     * Sets the controls used to interact with the menu.
     *
     * @param controls The controls to add to the menu
     * @return This instance of the builder
     */
    protected MenuBuilder setControls(Control... controls) {
        this.controls = controls;
        return this;
    }

    /**
     * Sets the message used as a placeholder for when the menu is loading in.
     *
     * @param placeholderMessage The message to display
     * @return This instance of the builder
     */
    public MenuBuilder setPlaceholderMessage(String placeholderMessage) {
        if (!placeholderMessage.equals(PLACEHOLDER_DISABLED_STRING)) {
            this.placeholderMessage = placeholderMessage;
        }
        return this;
    }

    /**
     * Sets the parent of this menu.
     *
     * @param parent The parent of this menu
     * @return This instance of the builder
     * @see #parent
     */
    private MenuBuilder setParent(T parent) {
        this.parent = parent;
        return this;
    }
}
