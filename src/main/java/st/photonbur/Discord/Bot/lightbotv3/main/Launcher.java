package st.photonbur.Discord.Bot.lightbotv3.main;

import net.dv8tion.jda.core.JDA;
import st.photonbur.Discord.Bot.lightbotv3.command.*;
import st.photonbur.Discord.Bot.lightbotv3.controller.BlacklistController;
import st.photonbur.Discord.Bot.lightbotv3.controller.ChannelController;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.controller.FileController;
import st.photonbur.Discord.Bot.lightbotv3.misc.console.ConsoleInputListener;
import st.photonbur.Discord.Bot.lightbotv3.misc.console.ConsoleInputEvent;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

/**
 * General manager of all parts of the bot.
 * Makes sure that everything is handled properly, from starting up to shutting down.
 */
public class Launcher implements ConsoleInputListener {
    public static final String VERSION = "3.1.2";

    private static Launcher instance;
    private Properties props = new Properties();
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

    public FileController getFileController() {
        return FileController.getInstance();
    }

    public static synchronized Launcher getInstance() {
        if (instance == null) {
            instance = new Launcher();
        }

        return instance;
    }

    public Logger getLogger() {
        return Logger.getInstance();
    }

    public static void main(String[] args) {
        Logger.log("Launching v" + Launcher.VERSION);
        Launcher.getInstance().run();
    }

    @Override
    public void onConsoleInput(ConsoleInputEvent ev) {
        String input = ev.getInput();

        if (input.equals("exit")) {
            sc.close();
            shutdown();
        } else if (input.equals("reload")) {
            getFileController().readAllGuilds();
        }
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

            getFileController().readAllGuilds();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (inputCfg != null) try {
                inputCfg.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        getCommandParser().addCommand(
                new BlacklistCommand(),
                new HelpCommand(),
                new InfoCommand(),
                new LinkChannelCommand(),
                new PermanentChannelCommand(),
                new SetCategoryCommand(),
                new TemporaryChannelCommand(),
                new TemporaryChannelSizeCommand(),
                new UnlinkChannelCommand(),
                new UnpermanentChannelCommand(),
                new WhitelistCommand()
        );

        sc = new Scanner(System.in);
        //noinspection StatementWithEmptyBody
        while (sc.hasNextLine()) { }
    }

    /**
     * Shuts down everything running within the bot.
     * This so that normal behaviour is forced for every element of it.
     */
    private void shutdown() {
        Logger.log("Shutting down...");

        sc.close();
        getLogger().shutdown();
        getBot().shutdown();

        System.exit(0);
    }
}
