package st.photonbur.Discord.Bot.lightbotv3.entity;

import net.dv8tion.jda.core.entities.Guild;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableRole;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableUser;
import st.photonbur.Discord.Bot.lightbotv3.entity.permissible.PermissibleMember;
import st.photonbur.Discord.Bot.lightbotv3.entity.permissible.PermissibleRole;

public class EntityConverter {
    public static PermissibleMember toPermissible(BannableUser user, Guild guild) {
        return new PermissibleMember(user.getIdLong(), guild);
    }

    public static PermissibleRole toPermissible(BannableRole role) {
        return new PermissibleRole(role.get());
    }
}
