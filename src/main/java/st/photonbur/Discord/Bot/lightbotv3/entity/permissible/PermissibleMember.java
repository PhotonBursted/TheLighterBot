package st.photonbur.Discord.Bot.lightbotv3.entity.permissible;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

@SuppressWarnings("unused")
public class PermissibleMember extends PermissibleEntity<Member> {
    public PermissibleMember(Member entity) {
        super(entity);
    }

    public PermissibleMember(long entityId, Guild guild) {
        super(entityId, guild::getMemberById);
    }

    PermissibleMember(String entityId, Guild guild) {
        super(entityId, guild::getMemberById);
    }
}
