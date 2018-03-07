package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

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

    private static final Logger log = LoggerFactory.getLogger(CommandParser.class);

    /**
     * The last event to have been parsed by the parser
     */
    private static GuildMessageReceivedEvent lastEvent;
    /**
     * The commands currently registered
     */
    private final Set<Command> commands;
    private static CommandParser instance;

    private CommandParser() {
        commands = new HashSet<>();
    }

    /**
     * Registers commands so that can be identified what command is being issued
     *
     * @param cmds The commands to register
     */
    private void addCommand(Command... cmds) {
        Collections.addAll(commands, cmds);
    }

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

    Set<Command> getCommands() {
        return commands;
    }

    public static CommandParser getInstance() {
        if (instance == null) {
            instance = new CommandParser();
        }

        return instance;
    }

    /**
     * @return The last handled message event
     */
    public static GuildMessageReceivedEvent getLastEvent() {
        return lastEvent;
    }

    /**
     * Triggers when a connected guild receives a new message.
     *
     * @param ev The event which triggered a call to this method
     */
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent ev) {
        // Get the raw message. This includes all markup as well.
        String msg = ev.getMessage().getContentRaw();

        if (msg.startsWith(DiscordController.getInstance().getCommandPrefix())) {
            // Create a queue of the input. Every space separated piece of the string is a new element
            LinkedBlockingQueue<String> input = new LinkedBlockingQueue<>(Arrays.asList(msg.split(SEP_SPACE)));

            StringBuilder results = new StringBuilder("Found commands for user input:")
                    .append(String.format("\n - Input: |%s|\n", msg));

            // Try to find if a command was referenced, and if there was any, find which
            Command targetCmd = commands.stream()
                    .filter(cmd -> cmd.messageIsCommand(input))
                    .peek(cmd -> results.append("\n - ").append(cmd.getAliasCollection().get(0)))
                    .sorted((cmd1, cmd2) -> Integer.compare(cmd2.getAliasCollection().getAliasLength(), cmd1.getAliasCollection().getAliasLength()))
                    .findFirst().orElse(null);

            // If a command was referenced, save the event and execute the command
            if (targetCmd != null) {
                log.info(results.append(String.format("\n\nChosen: %s", targetCmd.getAliasCollection().get(0))).toString());

                lastEvent = ev;

                Utils.shortenQueueBy(input, targetCmd.getAliasCollection().getAliasLength());
                targetCmd.prepareWithInput(input).executeCmd();
            }
        }
    }

    public void registerCommands() {
        StringBuilder status = new StringBuilder("Registered the following commands:");
        int commandCount, activatedCommandCount = 0;

        Reflections r = new Reflections(Command.class.getPackage().getName());
        Set<Class<?>> commandObjects = r.getTypesAnnotatedWith(AvailableCommand.class);
        commandCount = commandObjects.size();

        for (Class<?> cmdClass : commandObjects) {
            try {
                addCommand((Command) cmdClass.newInstance());
                activatedCommandCount++;

                status.append("\n - ").append(cmdClass.getSimpleName());
            } catch (InstantiationException | IllegalAccessException ex) {
                log.warn(String.format("Something went wrong adding command %s:", cmdClass.getSimpleName()), ex);
            }
        }

        log.info(status.append(String.format("\n\nActivated %s/%s commands succesfully.", activatedCommandCount, commandCount)).toString());
    }
}
