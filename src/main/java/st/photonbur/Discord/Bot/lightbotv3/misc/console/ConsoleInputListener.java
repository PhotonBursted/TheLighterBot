package st.photonbur.Discord.Bot.lightbotv3.misc.console;

public interface ConsoleInputListener {
    /**
     * Whenever the user inputs anything through the console, this method will be fired.
     *
     * @param event The event accompanying the input
     */
    void onConsoleInput(ConsoleInputEvent event);
}
