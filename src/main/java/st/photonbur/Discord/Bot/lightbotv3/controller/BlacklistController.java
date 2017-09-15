package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.User;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BlacklistController {    private static final HashMap<Guild, Set<ISnowflake>> blacklist;

    static {
        blacklist = new HashMap<>();
    }

    public static <T extends ISnowflake & IMentionable> String blacklist(Guild g, T entity) {
        blacklist.putIfAbsent(g, new HashSet<>());
        blacklist.get(g).add(entity);

        return confirmAction(Action.BLACKLIST, g, entity);
    }

    private static <T extends ISnowflake & IMentionable> String confirmAction(Action action, Guild g, T entity) {
        String entityType = entity.getClass().getSimpleName().toLowerCase().replace("impl", "");
        return String.format("Blacklisted a %s in guild \"%s:\"\n" +
                        " - Guild ID: %s\n" +
                        " - %s ID: %s (%s)",
                entityType, g.getName(), g.getId(),
                entityType.substring(0, 1).toUpperCase() + entityType.substring(1), entity.getId(),
                entity instanceof User ? Utils.userAsString((User) entity) : g.getRoleById(entity.getId()).getName());
    }

    static Set<? extends ISnowflake> getForGuild(Guild g) {
        return blacklist.get(g);
    }

    public static <T extends ISnowflake & IMentionable> String whitelist(Guild g, T entity) {
        blacklist.get(g).remove(entity);

        return confirmAction(Action.WHITELIST, g, entity);
    }


private enum Action {
        BLACKLIST(1),
        WHITELIST(2);

        private final int id;

        Action(int id) {
            this.id = id;
        }
    }
}
