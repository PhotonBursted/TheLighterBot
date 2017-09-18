package st.photonbur.Discord.Bot.lightbotv3.main;

import net.dv8tion.jda.core.JDA;
import st.photonbur.Discord.Bot.lightbotv3.command.BlacklistCommand;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.command.TemporaryChannelCommand;
import st.photonbur.Discord.Bot.lightbotv3.command.TemporaryChannelSizeCommand;
import st.photonbur.Discord.Bot.lightbotv3.controller.BlacklistController;
import st.photonbur.Discord.Bot.lightbotv3.controller.ChannelController;
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
            ChannelController.getInstance(this);
            DiscordController.getInstance(this, props.getProperty("token"), props.getProperty("prefix"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (inputCfg != null) try {
                inputCfg.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        getCommandParser().addCommand(new BlacklistCommand(this), new TemporaryChannelCommand(this), new TemporaryChannelSizeCommand(this));
    }

    /**
     * Shuts down everything running within the bot.
     * This so that normal behaviour is forced for every element of it.
     */
    private void shutdown() {
        Logger.shutdown();
    }

    public JDA getBot() {
        return getDiscordController().getBot();
    }

    public BlacklistController getBlacklistController() {
        return BlacklistController.getInstance();
    }

    public ChannelController getChannelController() {
        return ChannelController.getInstance();
    }

    public CommandParser getCommandParser() {
        return CommandParser.getInstance();
    }

    public DiscordController getDiscordController() {
        return DiscordController.getInstance();
    }
}
