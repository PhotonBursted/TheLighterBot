package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.Message;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CommandParser extends ListenerAdapter {
    private static final String SEP_SPACE = " ";

    private int level;
    private String msg;

    private static Set<Command> commands;
    private static GuildMessageReceivedEvent lastEvent;

    public static void addCommand(Command... cmds) {
        Collections.addAll(commands, cmds);
    }

    public static GuildMessageReceivedEvent getLastEvent() {
        return lastEvent;
    }

    private void handleError(GuildMessageReceivedEvent ev, Message msg) {
        handleError(ev, msg.getMessage());
    }

    private void handleError(GuildMessageReceivedEvent ev, String msg) {
        DiscordController.sendMessage(ev, msg, 10);
        ev.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
    }

    // Method checking if the string is part of a command, trimming up the message in the process
    boolean messageIsCommand(String command) {
        // Checks if the message complies with being a command
        boolean success = msg.startsWith((level == 0 ? DiscordController.getCommandPrefix() : "") + command);
        // If so, cut the first part of the message off
        if (success) {
            msg = String.join(SEP_SPACE, Arrays.copyOfRange(msg.split(SEP_SPACE), 1, msg.split(SEP_SPACE).length));
            level++;
        }

        return success;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent ev) {
        msg = ev.getMessage().getRawContent();
        level = 0;

        Command targetCmd = commands.stream()
                .filter(cmd -> cmd.getAliases().stream().anyMatch(this::messageIsCommand))
                .findFirst().orElse(null);

        if (targetCmd != null) {
            targetCmd.execute(msg);
        }
    }

    public static void removeCommand(Command... cmds) {
        commands.removeAll(Arrays.asList(cmds));
    }
}
