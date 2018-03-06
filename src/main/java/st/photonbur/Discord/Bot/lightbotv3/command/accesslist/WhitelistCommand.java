package st.photonbur.Discord.Bot.lightbotv3.command.accesslist;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;

public class WhitelistCommand extends AccesslistAdditionCommand {
    public WhitelistCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("whitelist", "wl"),
                "whitelist",
                LoggerFactory.getLogger(WhitelistCommand.class));
    }

    @Override
    String performAction(Guild guild, BannableEntity target) {
        return l.getAccesslistController().whitelist(guild, target);
    }

    @Override
    boolean performActionCheck(Guild guild, BannableEntity target) {
        if (target.isOfClass(User.class)) {
            return l.getAccesslistController().isEffectivelyWhitelisted(guild.getMemberById(target.getIdLong()));
        }

        //noinspection SimplifiableIfStatement
        if (target.isOfClass(Role.class)) {
            return l.getAccesslistController().isWhitelisted(guild.getRoleById(target.getIdLong()));
        }

        return false;
    }
}