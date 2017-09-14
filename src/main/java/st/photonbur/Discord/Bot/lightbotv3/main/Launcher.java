package st.photonbur.Discord.Bot.lightbotv3.main;

/**
 * General manager of all parts of the bot.
 * Makes sure that everything is handled properly, from starting up to shutting down.
 */
public class Launcher {
    public static void main(String[] args) {
        new Launcher().run();
    }

    /**
     * Starts up the bot and every component necessary to work properly.
     */
    private void run() {

    }

    /**
     * Shuts down everything running within the bot.
     * This so that normal behaviour is forced for every element of it.
     */
    private void shutdown() {
        Logger.shutdown();
    }
}
