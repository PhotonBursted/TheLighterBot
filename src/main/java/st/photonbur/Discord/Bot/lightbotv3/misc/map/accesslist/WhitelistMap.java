package st.photonbur.Discord.Bot.lightbotv3.misc.map.accesslist;

import net.dv8tion.jda.core.entities.Guild;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;

public class WhitelistMap extends AccesslistMap {
    @Override
    protected void addToDatabase(Guild g, BannableEntity entity) {
        l.getFileController().applyWhitelistAddition(g, entity);
    }

    @Override
    protected void deleteFromDatabase(Guild g, BannableEntity entity) {
        l.getFileController().applyWhitelistDeletion(g, entity);
    }
}
