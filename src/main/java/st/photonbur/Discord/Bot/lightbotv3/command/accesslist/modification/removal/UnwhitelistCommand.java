package st.photonbur.Discord.Bot.lightbotv3.command.accesslist.modification.removal;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.AvailableCommand;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;

@AvailableCommand
public class UnwhitelistCommand extends AccesslistRemovalCommand {
    public UnwhitelistCommand() {
        super(new CommandAliasCollectionBuilder()
                        .addAliasPart("unwhitelist", "uwl"),
                "unwhitelist", "whitelist",
                LoggerFactory.getLogger(UnwhitelistCommand.class));
    }

    @Override
    protected String performAction(Guild guild, BannableEntity target) {
        return l.getAccesslistController().unwhitelist(guild, target);
    }

    @Override
    protected boolean performActionCheck(Guild guild, BannableEntity target) {
        if (target.isOfClass(User.class)) {
            return !l.getAccesslistController().isEffectivelyWhitelisted(guild.getMemberById(target.getIdLong()));
        }

        //noinspection SimplifiableIfStatement
        if (target.isOfClass(Role.class)) {
            return !l.getAccesslistController().isWhitelisted(guild.getRoleById(target.getIdLong()));
        }

        return false;
    }
}
