package st.photonbur.Discord.Bot.lightbotv3.misc.map;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;

public class CategoryMap extends DefaultedDbMap<Guild, Category> {
    @Override
    protected void addToDatabase(Guild g, Category c) {
        l.getFileController().applyDefaultCategoryAddition(g, c);
    }

    @Override
    protected void deleteFromDatabase(Guild g, Category c) {
        l.getFileController().applyDefaultCategoryDeletion(g);
    }
}
