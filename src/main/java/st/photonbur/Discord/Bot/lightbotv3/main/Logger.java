package st.photonbur.Discord.Bot.lightbotv3.main;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.misc.console.ConsoleInputReader;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Custom Logger implementation for use throughout the project
 */
public class Logger extends ListenerAdapter {
    /**
     * Retains the ID of the last message deleted by the logger.
     * It is used to verify if a message marked for deletion should really be deleted or not.
     *
     * @see Message#getId()
     */
    private static String lastDeletedMessageId = "";

    /**
     * The file to write the log to.
     */
    private File logFile = new File("logs/lb-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".log.txt");
    /**
     * Stream handling output to the log file.
     */
    private PrintStream fOut;
    /**
     * Stream handling output to both the console and log file.
     */
    public static PrintStream out;
    private static PrintStream err;
    private static ConsoleInputReader in;

    private static Logger instance;

    static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }

        return instance;
    }

    private Logger() {
        setupLogger();
    }

    /**
     * Logs something towards both the log file and console.
     *
     * @param s The string to log
     */
    public static void log(String s) {
        log(s, 0);
    }

    /**
     * Logs something towards both the log file and console.
     *
     * The offset is used as a specifier for determining what stack trace element to reference in the log message.
     *
     * @param s      The string to log
     * @param offset The offset to determine the referenced stack trace element with
     */
    @SuppressWarnings("SameParameterValue")
    private static void log(String s, int offset) {
        if (out == null) {
            Logger.getInstance();
        }

        out.println(LocalDateTime.now().toString().replace("T", " ") + " - " + findCaller(offset) + "\n" + s + "\n");
    }

    /**
     * Finds the caller of anything within the Logger class using a certain offset.
     * This offset plays a role in determining how far to look for the source of the call.
     *
     * @param offset The minimum depth of the search for the caller of anything within the Logger class
     * @return The representation of the targeted caller of anything within the Logger class (of the form [className]#[methodName]:[lineNumber])
     * @see StackTraceElement
     */
    private static String findCaller(int offset) {
        // The string object to store the represenation of the source of the call in
        String caller = null;
        // The stack trace of the current call. This includes things from the Logger class
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();

        // Iterate over the stack trace elements, barring the one calling this method
        for (int i = 1; i < stes.length; i++) {
            // Store the element targeted
            StackTraceElement ste = stes[i];

            // Check if the stack trace element is currently from within the Logger class or the last available
            if ((
                    !ste.getClassName().contains(Logger.class.getName())
            ) || i == stes.length - 1) {
                // Determine the actual stack trace element target (compensated by the offset)
                ste = stes[Arrays.asList(stes).indexOf(ste) - offset];
                // Generate the string representation of the stack trace element
                caller = String.format("%s#%s:%d", ste.getClassName(), ste.getMethodName(), ste.getLineNumber());

                // Break the loop once a viable target has been found
                break;
            }
        }

        return caller;
    }

    /**
     * Besides logging, this method will also grab Discord's received message events as parsed by the {@link st.photonbur.Discord.Bot.lightbotv3.command.CommandParser CommandParser}.
     * If viable, it will delete the message and put the author and source of the sent message in the log.
     *
     * @param msg The string to log
     * @see GuildMessageReceivedEvent
     * @see CommandParser#getLastEvent
     */
    public static void logAndDelete(String msg) {
        logAndDelete(msg, null);
    }

    /**
     * Besides logging, this method will also grab Discord's received message events as parsed by the {@link st.photonbur.Discord.Bot.lightbotv3.command.CommandParser CommandParser}.
     * If viable, it will delete the message and put the author and source of the sent message in the log.
     *
     * @param msg              The string to log
     * @param successOperation The action to execute if the message deletes appropriately
     * @see GuildMessageReceivedEvent
     * @see CommandParser#getLastEvent
     */
    public static void logAndDelete(String msg, Consumer<Void> successOperation) {
        // Get the last event parsed by the command parser
        GuildMessageReceivedEvent ev = CommandParser.getLastEvent();

        // If the fetched event hasn't yet been deleted by the Logger, print a detailed message. Otherwise, just log it
        if (!lastDeletedMessageId.equals(ev.getMessageId())) {
            // Log a detailed message including author and source
            log(String.format("%s\n" +
                            " - Author: %s#%s (%s)\n" +
                            " - Source: %s",
                    msg,
                    ev.getAuthor().getName(), ev.getAuthor().getDiscriminator(), ev.getAuthor().getId(),
                    ev.getChannel().getName()));
            // Delete the message
            ev.getMessage().delete().queue(successOperation, error -> DiscordController.MESSAGE_ACTION_FAIL.accept(error, ev.getMessage()));
            // Mark the event as last handled
            lastDeletedMessageId = ev.getMessageId();
        } else {
            log(msg);
        }
    }

    @Override
    public void onException(ExceptionEvent ev) {
        ev.getCause().printStackTrace(Logger.out);
    }

    /**
     * Sets up whatever is needed for the logger to work. This includes file name and file streams for example.
     */
    private void setupLogger() {
        try {
            // Create the file to write the log into
            if (!logFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                logFile.getParentFile().mkdir();
                //noinspection ResultOfMethodCallIgnored
                logFile.createNewFile();
            }

            // Create the streams outputting the log
            fOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile)), true);

            // Store references to the standard out- and input streams
            PrintStream stdOut = System.out;
            PrintStream stdErr = System.err;
            InputStream stdIn = System.in;

            // Initialize and set streams to replace the standard ones.
            // This to allow for exporting all of standard in- and out to the log file.
            out = new PrintStream(new BufferedOutputStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    fOut.write(b);
                    stdOut.write(b);
                }
            }), true);
            System.setOut(out);

            err = new PrintStream(new BufferedOutputStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    fOut.write(b);
                    stdErr.write(b);
                }
            }), true);
            System.setErr(err);

            in = new ConsoleInputReader(stdIn, fOut);
            in.addListener(Launcher.getInstance());
            System.setIn(in);

            // Notify that the logger has started outputting info
            Logger.log("Started routing to file and System.out...\n" +
                    " - File: " + logFile.getPath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Acts when the bot is shutting down to properly handle closing streams.
     */
    void shutdown() {
        try {
            if (fOut != null) fOut.close();
            if (out != null) out.close();
            if (err != null) err.close();
            if (in != null) in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
