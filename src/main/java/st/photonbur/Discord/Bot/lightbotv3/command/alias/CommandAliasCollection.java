package st.photonbur.Discord.Bot.lightbotv3.command.alias;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class CommandAliasCollection extends ArrayList<String> {
    private final int aliasLength;

    CommandAliasCollection(ArrayList<String> combinations) {
        this.addAll(combinations);

        this.aliasLength = get(0).split("\\s+").length;
    }

    public String[] asArray() {
        return toArray(new String[size()]);
    }

    public String asString() {
        return String.join("\n", stream().map(a -> " - " + a).collect(Collectors.toList()));
    }

    public int getAliasLength() {
        return aliasLength;
    }
}
