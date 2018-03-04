package st.photonbur.Discord.Bot.lightbotv3.entity.permissible;

import net.dv8tion.jda.core.entities.Role;

@SuppressWarnings("unused")
public class PermissibleRole extends PermissibleEntity<Role> {
    public PermissibleRole(Role entity) {
        super(entity);
    }

    PermissibleRole(long entityId) {
        super(entityId, id -> l.getBot().getRoleById(id));
    }

    PermissibleRole(String entityId) {
        super(entityId, id -> l.getBot().getRoleById(id));
    }
}
