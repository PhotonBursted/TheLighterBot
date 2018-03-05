package st.photonbur.Discord.Bot.lightbotv3.command.accesslist;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;

public class BlacklistCommand extends AccesslistAdditionCommand {
    public BlacklistCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("blacklist", "bl"),
                "blacklist",
                LoggerFactory.getLogger(BlacklistCommand.class));
    }

    @Override
    String performAction(Guild guild, BannableEntity target) {
        return l.getAccesslistController().blacklist(guild, target);
    }

    @Override
    boolean performActionCheck(Guild guild, BannableEntity target) {
        if (target.isOfClass(User.class)) {
            return l.getAccesslistController().isEffectivelyBlacklisted(guild.getMemberById(target.getIdLong()));
        }

        //noinspection SimplifiableIfStatement
        if (target.isOfClass(Role.class)) {
            return l.getAccesslistController().isBlacklisted(guild.getRoleById(target.getIdLong()));
        }

        return false;
    }
}
