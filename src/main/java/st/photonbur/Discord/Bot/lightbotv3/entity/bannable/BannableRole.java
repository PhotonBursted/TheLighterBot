package st.photonbur.Discord.Bot.lightbotv3.entity.bannable;

import net.dv8tion.jda.core.entities.Role;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

public class BannableRole extends BannableEntity<Role> {
    public BannableRole(Role entity) {
        super(entity);
    }

    public BannableRole(long entityId) {
        super(entityId, id -> Launcher.getInstance().getBot().getRoleById(id));
    }

    public BannableRole(String entityId) {
        super(entityId, id -> Launcher.getInstance().getBot().getRoleById(id));
    }

    @Override
    public String getName() {
        return get().getName();
    }
}