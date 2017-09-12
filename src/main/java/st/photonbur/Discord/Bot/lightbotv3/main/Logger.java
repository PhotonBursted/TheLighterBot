package st.photonbur.Discord.Bot.lightbotv3.main;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import st.photonbur.Discord.Bot.lightbotv3.command.CommandParser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;

public class Logger {
    private static String lastMessageId = "";

    private static File logFile;
    private static FileOutputStream fos;
    private static PrintStream out;

    static {
        setupLogger();
    }

    public static void log(String s) {
        log(s, 0);
    }

    @SuppressWarnings("SameParameterValue")
    private static void log(String s, int offset) {
        out.println(LocalDateTime.now().toString().replace("T", " ") + " - " + findCaller(Thread.currentThread().getStackTrace(), offset) + "\n" + s + "\n");
    }

    private static String findCaller(StackTraceElement[] stes, int offset) {
        String caller = null;

        for (int i = 1; i < stes.length; i++) {
            StackTraceElement ste = stes[i];

            if (!ste.getClassName().contains(Logger.class.getName()) || i == stes.length - 1) {
                ste = stes[Arrays.asList(stes).indexOf(ste) - offset];
                caller = ste.getClassName() + "#" + ste.getMethodName() + ":" + ste.getLineNumber();
                break;
            }
        }

        return caller;
    }

    public static void logAndDelete(String msg) {
        GuildMessageReceivedEvent ev = CommandParser.getLastEvent();

        if (!lastMessageId.equals(ev.getMessageId())) {
            log(String.format("%s\n" +
                            " - Author: %s#%s (%s)\n" +
                            " - Source: %s",
                    msg,
                    ev.getAuthor().getName(), ev.getAuthor().getDiscriminator(), ev.getAuthor().getId(),
                    ev.getChannel().getName()));
            ev.getMessage().delete().complete();
            lastMessageId = ev.getMessageId();
        } else {
            log(msg);
        }
    }

    private static void setupLogger() {
        try {
            logFile = new File("mblog-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".txt");

            fos = new FileOutputStream(logFile);
            out = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    System.out.write(b);
                    fos.write(b);
                }
            });

            Logger.log("Started routing to file and System.out...\n" +
                    " - File: " + logFile.getPath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void shutdown() {
        try {
            if (fos != null) fos.close();
            if (out != null) out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
