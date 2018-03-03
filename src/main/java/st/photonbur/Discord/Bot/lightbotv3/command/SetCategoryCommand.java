package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectionEvent;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.Selector;
import st.photonbur.Discord.Bot.lightbotv3.misc.menu.selector.SelectorBuilder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class SetCategoryCommand extends Command implements Selector<Category> {
    private static final Logger log = LoggerFactory.getLogger(SetCategoryCommand.class);

    private Category c;
    private String search;

    public SetCategoryCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("setcat", "sc", "setcategory"));
    }

    @Override
    void execute() {
        if (input.size() >= 1) {
            search = Utils.drainQueueToString(input);

            if (search.startsWith("cat:")) {
                List<Category> candidates = l.getBot().getCategoriesByName(String.join(":", Arrays.copyOfRange(search.split(":"), 1, search.split(":").length)), true);
                if (candidates.size() > 1) {
                    LinkedHashMap<String, Category> candidateMap = new LinkedHashMap<>();
                    candidates.forEach(candidate -> candidateMap.put(candidate.getName() + "(ID " + candidate.getId() + ")", candidate));

                    new SelectorBuilder<>(this)
                            .setOptionMap(candidateMap)
                            .build();
                    return;
                } else if (candidates.size() == 1) {
                    c = candidates.get(0);
                } else {
                    c = null;
                }
            } else if (search.matches("[0-9]+")) {
                c = l.getBot().getCategoryById(search);
            } else {
                c = null;
            }

            performCategoryChange();
        } else {
            handleError("The category to set wasn't specified!");
        }
    }

    @Override
    String getDescription() {
        return "Sets the default category to place new temporary channels into.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] { Permission.MANAGE_CHANNEL };
    }

    @Override
    String getUsage() {
        return "{}setcat <searchTerm>\\n\" +\n" +
                "    <searchTerm> can be any of:\n" +
                "       - <search> - searches for a category ID.\n" +
                "         This can also be `remove` or `null` to remove the default category.\n" +
                "       - cat:<search> - searches for a category with the name of <search>";
    }

    @Override
    public void onSelection(SelectionEvent<Category> selEv) {
        if (selEv.selectionWasMade()) {
            c = selEv.getSelectedOption();

            performCategoryChange();
        } else {
            handleError("The category change was cancelled.");
        }
    }

    private void performCategoryChange() {
        if (c != null || search.equals("remove") || search.equals("null")) {
            if (c == null) {
                l.getChannelController().getCategories().remove(ev.getGuild());
                l.getFileController().applyDefaultCategoryDeletion(ev.getGuild());

                l.getDiscordController().sendMessage(ev, "Successfully removed the category to put new temporary channels in.",
                        DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                LoggerUtils.logAndDelete(log, "Removed default category from " + ev.getGuild().getName());
            } else {
                l.getChannelController().getCategories().put(ev.getGuild(), c);
                l.getFileController().applyDefaultCategoryAddition(ev.getGuild(), c);

                l.getDiscordController().sendMessage(ev, "Successfully set category to put new temporary channels in to **" + c.getName() + "** (ID " + c.getId() + ")",
                        DiscordController.AUTOMATIC_REMOVAL_INTERVAL);
                LoggerUtils.logAndDelete(log, "Set default category to " + c.getName() + " for " + ev.getGuild().getName());
            }

        } else {
            handleError("The category you specified couldn't be found!");
        }
    }
}
