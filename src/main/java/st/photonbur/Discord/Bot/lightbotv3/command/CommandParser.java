package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.entity.MessageContent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CommandParser extends ListenerAdapter {
    private static final String SEP_SPACE = "\\s+";

    private Set<Command> commands;
    private GuildMessageReceivedEvent lastEvent;

    public CommandParser() {
        commands = new HashSet<>();
    }

    public void addCommand(Command... cmds) {
        Collections.addAll(commands, cmds);
    }

    public GuildMessageReceivedEvent getLastEvent() {
        return lastEvent;
    }

    void handleError(MessageContent msg) {
        handleError(msg.getMessage());
    }

    private void handleError(String msg) {
        DiscordController.sendMessage(lastEvent, msg, 10);
        lastEvent.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent ev) {
        String msg = ev.getMessage().getRawContent();

        ArrayDeque<String> input = new ArrayDeque<>();
        input.addAll(Arrays.asList(msg.split(SEP_SPACE)));

        Command targetCmd = commands.stream()
                .filter(cmd -> cmd.getAliases().stream().anyMatch(alias -> cmd.messageIsCommand(input)))
                .findFirst().orElse(null);

        if (targetCmd != null) {
            ev.getMessage().delete().reason("The message was part of a command.").queue();
            targetCmd.setInput(input).execute();
        }
    }

    public void removeCommand(Command... cmds) {
        commands.removeAll(Arrays.asList(cmds));
    }
}
