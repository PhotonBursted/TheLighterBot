package st.photonbur.Discord.Bot.lightbotv3.main;

import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.controller.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

/**
 * General manager of all parts of the bot.
 * Makes sure that everything is handled properly, from starting up to shutting down.
 */
public class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static final String VERSION = "3.2.3";

    private static Launcher instance;
    private final Properties props = new Properties();
    private Scanner sc;

    private Launcher() { }

    /**
     * As part of the Singleton design pattern, no clones of this instance are permitted.
     *
     * @return nothing
     * @throws CloneNotSupportedException No clones of this instance are permitted
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public JDA getBot() {
        return getDiscordController().getBot();
    }

    public AccesslistController getAccesslistController() {
        return AccesslistController.getInstance();
    }

    public ChannelController getChannelController() {
        return ChannelController.getInstance();
    }

    public ChannelPermissionController getChannelPermissionController() {
        return ChannelPermissionController.getInstance();
    }

    public CommandParser getCommandParser() {
        return CommandParser.getInstance();
    }

    public DiscordController getDiscordController() {
        return DiscordController.getInstance();
    }

    public FileController getFileController() {
        return FileController.getInstance();
    }

    public static synchronized Launcher getInstance() {
        if (instance == null) {
            instance = new Launcher();
        }

        return instance;
    }

    public static void main(String[] args) {
        log.info("Launching v" + Launcher.VERSION);
        Launcher.getInstance().run();
    }

    /**
     * Starts up the bot and every component necessary to work properly.
     */
    private void run() {
        FileInputStream inputCfg = null;
        try {
            inputCfg = new FileInputStream("config.properties");
            props.load(inputCfg);

            ChannelController.getInstance();
            DiscordController.getInstance(this, props.getProperty("token"), props.getProperty("prefix"));
            FileController.getInstance(
                    props.getProperty("dbhost"),
                    props.getProperty("dbport"),
                    props.getProperty("dbname"),
                    props.getProperty("dbuser"),
                    props.getProperty("dbpass")
            );

            getFileController().loadEverything();
        } catch (IOException ex) {
            log.error("Something went wrong loading the necessary settings to launch.", ex);
        } finally {
            if (inputCfg != null) try {
                inputCfg.close();
            } catch (IOException ex) {
                log.error("Something went wrong closing the configuration file for launching.", ex);
            }
        }

        getCommandParser().registerCommands();

        sc = new Scanner(System.in);
        //noinspection StatementWithEmptyBody
        while (sc.hasNextLine()) {
            String input = sc.nextLine();

            if (input.equals("exit")) {
                sc.close();
                shutdown();
            } else if (input.equals("reload")) {
                getFileController().loadEverything();
            }
        }
    }

    /**
     * Shuts down everything running within the bot.
     * This so that normal behaviour is forced for every element of it.
     */
    private void shutdown() {
        log.info("Shutting down...");

        sc.close();
        getBot().shutdown();
        getFileController().shutdown();

        System.exit(0);
    }
}
