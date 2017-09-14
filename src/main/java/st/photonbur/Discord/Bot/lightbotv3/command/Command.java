package st.photonbur.Discord.Bot.lightbotv3.command;

import java.util.ArrayDeque;
import java.util.Collection;

interface Command {
    void execute(ArrayDeque<String> args);

    Collection<String> getAliases();
}
