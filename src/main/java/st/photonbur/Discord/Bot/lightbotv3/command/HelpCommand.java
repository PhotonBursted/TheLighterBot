package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.Control;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator.Paginator;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator.PaginatorImpl;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class HelpCommand extends Command implements Paginator<MessageEmbed> {
    public HelpCommand(Launcher l) {
        super(l);
    }

    @Override
    void execute() throws RateLimitedException {
        LinkedList<String> commandTextList = new LinkedList<>();

        l.getCommandParser().getCommands().stream()
                .sorted((cmd1, cmd2) -> cmd1.getAliases()[0].compareToIgnoreCase(cmd2.getAliases()[0]))
                .forEach(cmd -> {
            String sb =
                    String.format("**%s** - %s", l.getDiscordController().getCommandPrefix() + cmd.getAliases()[0], cmd.getDescription()) +
                    String.format("\n>>|%s", cmd.getAliases().length == 0 ? "None" : String.join(", ", Arrays.stream(cmd.getAliases()).skip(1).sorted(String::compareToIgnoreCase).collect(Collectors.toList())) +
                    String.format("\n>>|%s", cmd.getUsage().replace("{}", l.getDiscordController().getCommandPrefix())) +
                    String.format("\n>>|%s", cmd.getPermissionsRequired().length == 0 ? "None" : String.join(", ", Arrays.stream(cmd.getPermissionsRequired()).map(Enum::name).collect(Collectors.toList()))));

            commandTextList.add(sb);
        });

        Logger.logAndDelete("Begged for help!");
        l.getBot().addEventListener(new PaginatorImpl(this, commandTextList, ev.getMessage(),
                Control.PREV,
                Control.STOP,
                Control.NEXT));
    }

    @Override
    String[] getAliases() {
        return new String[] {"help", "h"};
    }

    @Override
    String getDescription() {
        return "Displays information about the commands being part of the bot";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] {};
    }

    @Override
    String getUsage() {
        return "{}help\n" +
                " - Shows information of all commands implemented as of now.";
    }

    @Override
    public MessageEmbed constructMessage(String[] contents, int currPage, int nPages) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Command help - " + String.format("Page %d/%d", currPage, nPages), null)
                .setColor(Color.WHITE)
                .setDescription("The following commands are available:")
                .setTimestamp(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Amsterdam")).withZoneSameInstant(ZoneOffset.UTC));

        for (String helpText : contents) {
            String[] helpElems = helpText.split("\\n>>\\|");

            eb.addField(helpElems[0],
                    String.format(
                            "**Aliases: **%s\n" +
                            "**Usage: **%s\n" +
                            "**Permissions needed: **%s",
                            helpElems[1], helpElems[2], helpElems[3]),
                    false);
        }
        return eb.build();
    }

    @Override
    public LinkedList<String> groupContent(LinkedList<String> contents, String itemSeparator) {
        int groupID = -1, i = 0;
        final int groupSizeThreshold = 3;
        LinkedList<String> groups = new LinkedList<>();

        for (String contentItem : contents) {
            if (contentItem.length() > 2) {
                if (i % groupSizeThreshold != 0) {
                    groups.set(groupID, String.format("%s%s%s",
                            groups.get(groupID), groups.get(groupID).equals("") ? "" : itemSeparator, contentItem));
                } else {
                    groupID++;
                    groups.add(contentItem);
                }

                i++;
            }
        }

        return groups;
    }
}