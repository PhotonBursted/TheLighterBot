package st.photonbur.Discord.Bot.lightbotv3.command.alias;

import java.util.ArrayList;

public class CommandAliasCollection extends ArrayList<String> {
    private final int aliasLength;

    CommandAliasCollection(ArrayList<String> combinations) {
        this.addAll(combinations);

        this.aliasLength = get(0).split("\\s+").length;
    }

    public int getAliasLength() {
        return aliasLength;
    }
}
