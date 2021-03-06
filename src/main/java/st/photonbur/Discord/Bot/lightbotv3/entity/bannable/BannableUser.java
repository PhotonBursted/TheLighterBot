package st.photonbur.Discord.Bot.lightbotv3.entity.bannable;

import net.dv8tion.jda.core.entities.User;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

public class BannableUser extends BannableEntity<User> {
    public BannableUser(User entity) {
        super(entity);
    }

    public BannableUser(long entityId) {
        super(entityId, id -> Launcher.getInstance().getBot().getUserById(id));
    }

    public BannableUser(String entityId) {
        super(entityId, id -> Launcher.getInstance().getBot().getUserById(id));
    }

    @Override
    public String getName() {
        return Utils.userAsString(get());
    }
}