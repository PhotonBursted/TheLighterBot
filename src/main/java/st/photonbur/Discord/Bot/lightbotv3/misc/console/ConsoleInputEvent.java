package st.photonbur.Discord.Bot.lightbotv3.misc.console;

/**
 * Fired when a user enters something into the console.
 */
public class ConsoleInputEvent {
    /**
     * The input entered through the console.
     */
    private final String input;

    ConsoleInputEvent(String input) {
        this.input = input;
    }

    /**
     * @return The input gathered from the console
     */
    public String getInput() {
        return input;
    }
}
