package st.photonbur.Discord.Bot.lightbotv3.command.accesslist.modification.removal;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.AvailableCommand;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;

@AvailableCommand
public class UnblacklistCommand extends AccesslistRemovalCommand {
    public UnblacklistCommand() {
        super(new CommandAliasCollectionBuilder()
                        .addAliasPart("unblacklist", "ubl"),
                "unblacklist", "blacklist",
                LoggerFactory.getLogger(UnblacklistCommand.class));
    }

    @Override
    protected String performAction(Guild guild, BannableEntity target) {
        return l.getAccesslistController().unblacklist(guild, target);
    }

    @Override
    protected boolean performActionCheck(Guild guild, BannableEntity target) {
        if (target.isOfClass(User.class)) {
            return !l.getAccesslistController().isEffectivelyBlacklisted(guild.getMemberById(target.getIdLong()));
        }

        //noinspection SimplifiableIfStatement
        if (target.isOfClass(Role.class)) {
            return !l.getAccesslistController().isBlacklisted(guild.getRoleById(target.getIdLong()));
        }

        return false;
    }
}
