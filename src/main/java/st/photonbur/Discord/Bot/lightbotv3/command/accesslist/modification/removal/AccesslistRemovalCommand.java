package st.photonbur.Discord.Bot.lightbotv3.command.accesslist.modification.removal;

import org.slf4j.Logger;
import st.photonbur.Discord.Bot.lightbotv3.command.accesslist.modification.AccesslistModificationCommand;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.misc.StringUtils;

abstract class AccesslistRemovalCommand extends AccesslistModificationCommand {
    AccesslistRemovalCommand(CommandAliasCollectionBuilder aliasBuilder,
                             String primaryCommandName,
                             String targetAccesslist,
                             Logger log) {
        super(aliasBuilder, primaryCommandName, targetAccesslist, false, log);
    }

    @Override
    protected String getDescription() {
        return "Removes a user or role from the " + StringUtils.capitalize(targetAccesslist);
    }
}
