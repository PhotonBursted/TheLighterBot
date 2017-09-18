package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class CommandParser extends ListenerAdapter {
    /**
     * Constant to store the space separator in. This to avoid many String object creations
     */
    private static final String SEP_SPACE = "\\s+";

    /**
     * The last event to have been parsed by the parser
     */
    private static GuildMessageReceivedEvent lastEvent;
    /**
     * The commands currently registered
     */
    private final Set<Command> commands;
    private static CommandParser instance;

    public static CommandParser getInstance() {
        if (instance == null) {
            instance = new CommandParser();
        }

        return instance;
    }

    private CommandParser() {
        commands = new HashSet<>();
    }

    /**
     * @return The last handled message event
     */
    public static GuildMessageReceivedEvent getLastEvent() {
        return lastEvent;
    }

    /**
     * Registers commands so that can be identified what command is being issued
     *
     * @param cmds The commands to register
     */
    public void addCommand(Command... cmds) {
        Collections.addAll(commands, cmds);
    }

    /**
     * Unregisters a command, stopping it from being identified as a new message comes in
     *
     * @param cmds The commands to remove from the registry
     */
    public void removeCommand(Command... cmds) {
        commands.removeAll(Arrays.asList(cmds));
    }

    /**
     * Triggers when a connected guild receives a new message.
     *
     * @param ev The event which triggered a call to this method
     */
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent ev) {
        // Get the raw message. This includes all markup as well.
        String msg = ev.getMessage().getRawContent();

        // Create a queue of the input. Every space separated piece of the string is a new element
        LinkedBlockingQueue<String> input = new LinkedBlockingQueue<>(Arrays.asList(msg.split(SEP_SPACE)));

        // Try to find if a command was referenced, and if there was any, find which
        Command targetCmd = commands.stream()
                .filter(cmd -> cmd.messageIsCommand(input))
                .sorted((cmd1, cmd2) -> {
                    Double nParts1 = cmd1.getAliases().stream().mapToInt(alias -> alias.split(SEP_SPACE).length).average().orElse(0);
                    Double nParts2 = cmd2.getAliases().stream().mapToInt(alias -> alias.split(SEP_SPACE).length).average().orElse(0);
                    return nParts2.compareTo(nParts1);
                })
                .findFirst().orElse(null);

        // If a command was referenced, save the event and execute the command
        if (targetCmd != null) {
            lastEvent = ev;

            targetCmd.prepareWithInput(input).executeCmd();
        }
    }
}
