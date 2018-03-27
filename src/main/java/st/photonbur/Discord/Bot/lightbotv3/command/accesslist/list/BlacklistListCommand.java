package st.photonbur.Discord.Bot.lightbotv3.command.accesslist.list;

import net.dv8tion.jda.core.entities.Guild;
import st.photonbur.Discord.Bot.lightbotv3.command.AvailableCommand;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;

import java.util.Set;

@AvailableCommand
public class BlacklistListCommand extends AccesslistListCommand {
    public BlacklistListCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("blacklist", "bl")
                .addAliasPart("-list", "-l"),
                "blacklist");
    }

    @Override
    protected Set<BannableEntity> getListForGuild(Guild g) {
        return l.getAccesslistController().getBlacklistForGuild(g);
    }
}
