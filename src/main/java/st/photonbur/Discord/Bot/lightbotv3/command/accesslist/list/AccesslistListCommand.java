package st.photonbur.Discord.Bot.lightbotv3.command.accesslist.list;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.Command;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.StringUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator.Paginator;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.paginator.PaginatorBuilder;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

abstract public class AccesslistListCommand extends Command implements Paginator<MessageEmbed> {
    private static final Logger log = LoggerFactory.getLogger(AccesslistListCommand.class);
    private final String primaryCommandName;

    AccesslistListCommand(CommandAliasCollectionBuilder aliasCollectionBuilder,
                          String primaryCommandName) {
        super(aliasCollectionBuilder);

        this.primaryCommandName = primaryCommandName;
    }

    @Override
    public MessageEmbed constructMessage(String[] contents, int currPage, int nPages) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(String.format("%sed entities - ", StringUtils.capitalize(primaryCommandName)) + String.format("Page %d/%d", currPage, nPages), null)
                .setColor(Color.WHITE)
                .setTimestamp(Instant.now());

        eb.addField(StringUtils.capitalize(contents[0].split("[^a-zA-Z]")[0]) + "s", Arrays.stream(contents)
                .map(entity -> String.format("- %s", entity))
                .collect(Collectors.joining("\n")), false);

        return eb.build();
    }

    @Override
    protected void execute() {
        Set<BannableEntity> entities = getListForGuild(ev.getGuild());

        LinkedList<String> entityList = entities.stream()
                .sorted((e1, e2) -> {
                    int compareClass = e1.get().getClass().getSimpleName().compareTo(e2.get().getClass().getSimpleName());

                    if (compareClass != 0) {
                        return compareClass;
                    } else {
                        return e1.getName().compareTo(e2.getName());
                    }
                })
                .map(BannableEntity::toString)
                .collect(Collectors.toCollection(LinkedList::new));

        LoggerUtils.logAndDelete(log, "Showed " + primaryCommandName + "!", (success) ->
                new PaginatorBuilder<>(this)
                        .setContent(entityList)
                        .setPlaceholderMessage("Showing " + primaryCommandName + "...")
                        .build());
    }

    @Override
    protected String getDescription() {
        return "Shows all " + primaryCommandName + "ed entities for this guild";
    }

    protected abstract Set<BannableEntity> getListForGuild(Guild g);

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[0];
    }

    @Override
    protected String getUsage() {
        return "{}" + primaryCommandName + " -list\n" +
                " - Shows all " + primaryCommandName + "ed entities for this guild";
    }

    @Override
    public LinkedList<String> groupContent(LinkedList<String> contents, String itemSeparator) {
        int groupID = -1, i = 0;
        final int groupSizeThreshold = 15;
        LinkedList<String> groups = new LinkedList<>();

        String currentGroup = null;

        for (String entityItem : contents) {
            if (i % groupSizeThreshold != 0 && entityItem.split("[^a-zA-Z]")[0].equals(currentGroup)) {
                groups.set(groupID, String.format("%s%s%s",
                        groups.get(groupID),
                        groups.get(groupID).equals("") ? "" : itemSeparator,
                        entityItem));
            } else {
                groupID++;
                groups.add(entityItem);
                currentGroup = entityItem.split("[^a-zA-Z]")[0];

                i = 0;
            }

            i++;
        }

        return groups;
    }
}
