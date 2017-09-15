package st.photonbur.Discord.Bot.lightbotv3.main;

import net.dv8tion.jda.core.JDA;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * General manager of all parts of the bot.
 * Makes sure that everything is handled properly, from starting up to shutting down.
 */
public class Launcher {
    private Properties props = new Properties();

    private DiscordController discordController;

    public static void main(String[] args) {
        new Launcher().run();
    }

    /**
     * Starts up the bot and every component necessary to work properly.
     */
    private void run() {
        FileInputStream inputCfg = null;
        try {
            inputCfg = new FileInputStream("config.properties");

            props.load(inputCfg);
            discordController = new DiscordController(this, props.getProperty("token"), props.getProperty("prefix"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (inputCfg != null) try {
                inputCfg.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Shuts down everything running within the bot.
     * This so that normal behaviour is forced for every element of it.
     */
    private void shutdown() {
        Logger.shutdown();
    }

    public JDA getBot() {
        return discordController.getBot();
    }
}
