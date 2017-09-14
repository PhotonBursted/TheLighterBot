package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.*;

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

    public CommandParser() {
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
     * Triggers when a connected guild receives a new message.
     *
     * @param ev The event which triggered a call to this method
     */
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

    /**
     * Unregisters a command, stopping it from being identified as a new message comes in
     *
     * @param cmds The commands to remove from the registry
     */
    public void removeCommand(Command... cmds) {
        commands.removeAll(Arrays.asList(cmds));
    }
}
