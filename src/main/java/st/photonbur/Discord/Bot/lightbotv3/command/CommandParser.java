package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CommandParser extends ListenerAdapter {
    private static final String SEP_SPACE = "\\s+";

    private ArrayDeque<String> input;
    private int level;

    private static Set<Command> commands;
    private static GuildMessageReceivedEvent lastEvent;

    public static void addCommand(Command... cmds) {
        Collections.addAll(commands, cmds);
    }

    public static GuildMessageReceivedEvent getLastEvent() {
        return lastEvent;
    }

    void handleError(MessageContent msg) {
        handleError(msg.getMessage());
    }

    private void handleError(String msg) {
        DiscordController.sendMessage(lastEvent, msg, 10);
        lastEvent.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
    }

    // Method checking if the string is part of a command, trimming up the message in the process
    boolean messageIsCommand(String command) {
        String elem = input.getFirst();

        // Checks if the message complies with being a command
        boolean success = elem.equals((level == 0 ? DiscordController.getCommandPrefix() : "") + command);
        // If so, cut the first part of the message off
        if (success) {
            input.pop();
            level++;
        }

        return success;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent ev) {
        String msg = ev.getMessage().getRawContent();
        level = 0;

        input.addAll(Arrays.asList(msg.split(SEP_SPACE)));

        Command targetCmd = commands.stream()
                .filter(cmd -> cmd.getAliases().stream().anyMatch(this::messageIsCommand))
                .findFirst().orElse(null);

        if (targetCmd != null) {
            targetCmd.execute(input);
        }
    }

    public static void removeCommand(Command... cmds) {
        commands.removeAll(Arrays.asList(cmds));
    }
}
