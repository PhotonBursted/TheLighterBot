package st.photonbur.Discord.Bot.lightbotv3.command.accesslist.list;

import net.dv8tion.jda.core.entities.Guild;
import st.photonbur.Discord.Bot.lightbotv3.command.AvailableCommand;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;

import java.util.Set;

@AvailableCommand
public class WhitelistListCommand extends AccesslistListCommand {
    public WhitelistListCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("whitelist", "wl")
                .addAliasPart("-list", "-l"),
                "whitelist");
    }

    @Override
    protected Set<BannableEntity> getListForGuild(Guild g) {
        return l.getAccesslistController().getWhitelistForGuild(g);
    }
}
