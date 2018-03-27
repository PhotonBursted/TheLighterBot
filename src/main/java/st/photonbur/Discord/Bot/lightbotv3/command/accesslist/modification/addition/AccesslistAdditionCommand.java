package st.photonbur.Discord.Bot.lightbotv3.command.accesslist.modification.addition;

import org.slf4j.Logger;
import st.photonbur.Discord.Bot.lightbotv3.command.accesslist.modification.AccesslistModificationCommand;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.misc.StringUtils;

abstract class AccesslistAdditionCommand extends AccesslistModificationCommand {
    AccesslistAdditionCommand(CommandAliasCollectionBuilder aliasBuilder,
                              String primaryCommandName,
                              String targetAccesslist,
                              Logger log) {
        super(aliasBuilder, primaryCommandName, targetAccesslist, true, log);
    }

    @Override
    protected String getDescription() {
        return StringUtils.capitalize(targetAccesslist) + "s a role or user.";
    }
}
