package st.photonbur.Discord.Bot.lightbotv3.command;

import java.util.Collection;

interface Command {
    void execute(String args);

    Collection<String> getAliases();
}
