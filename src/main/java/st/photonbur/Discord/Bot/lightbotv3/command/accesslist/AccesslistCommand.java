package st.photonbur.Discord.Bot.lightbotv3.command.accesslist;

import net.dv8tion.jda.core.entities.Guild;
import st.photonbur.Discord.Bot.lightbotv3.command.Command;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;

public abstract class AccesslistCommand extends Command {
    protected AccesslistCommand(CommandAliasCollectionBuilder aliasBuilder) {
        super(aliasBuilder);
    }

    protected abstract String performAction(Guild guild, BannableEntity target);

    protected abstract boolean performActionCheck(Guild guild, BannableEntity target);
}
