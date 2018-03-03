package st.photonbur.Discord.Bot.lightbotv3.command.alias;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class CommandAliasCollectionBuilder {
    private final List<String[]> aliasParts;

    public CommandAliasCollectionBuilder() {
        aliasParts = new ArrayList<>();
    }

    public CommandAliasCollection build() {
        return fetchCombinations(aliasParts.toArray(new String[aliasParts.size()][]));
    }

    private CommandAliasCollection fetchCombinations(String[][] matrix) {
        ArrayList<String> combinations = new ArrayList<>();

        fetchCombinations(matrix, combinations, 0, "");

        return new CommandAliasCollection(combinations);
    }

    private void fetchCombinations(String[][] matrix, ArrayList<String> output, int depth, String current) {
        if (depth == matrix.length) {
            output.add(current);
            return;
        }

        for (int i = 0; i < matrix[depth].length; ++i) {
            fetchCombinations(matrix, output, depth + 1, current + (current.length() > 0 ? " " : "") + matrix[depth][i]);
        }
    }

    public CommandAliasCollectionBuilder addAliasPart(String... aliasPart) {
        return setAliasPart(aliasParts.size(), aliasPart);
    }

    public CommandAliasCollectionBuilder setAliasPart(int part, String... aliasPart) {
        while (aliasParts.size() <= part) {
            aliasParts.add(new String[0]);
        }

        aliasParts.set(part, aliasPart);

        return this;
    }
}
