package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.entities.*;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BlacklistController {
    private enum Action {
        BLACKLIST(1),
        WHITELIST(2);

        private final int id;

        Action(int id) {
            this.id = id;
        }
    }

    private final HashMap<Guild, Set<ISnowflake>> blacklist;
    private static BlacklistController instance;

    private BlacklistController() {
        blacklist = new HashMap<>();
    }

    public static synchronized BlacklistController getInstance() {
        if (instance == null) {
            instance = new BlacklistController();
        }

        return instance;
    }

    public <T extends ISnowflake & IMentionable> String blacklist(Guild g, T entity) {
        blacklist.putIfAbsent(g, new HashSet<>());
        blacklist.get(g).add(entity);

        return confirmAction(Action.BLACKLIST, g, entity);
    }

    private static <T extends ISnowflake & IMentionable> String confirmAction(Action action, Guild g, T entity) {
        String entityType = entity.getClass().getSimpleName().toLowerCase().replace("impl", "");

        return String.format("%sed a %s in guild \"%s:\"\n" +
                        " - Guild ID: %s\n" +
                        " - %s ID: %s (%s)",
                action.name().substring(0, 1).toUpperCase() + action.name().substring(1).toLowerCase(),
                entityType, g.getName(), g.getId(),
                entityType.substring(0, 1).toUpperCase() + entityType.substring(1), entity.getId(),
                entity instanceof User ? Utils.userAsString((User) entity) : g.getRoleById(entity.getId()).getName());
    }

    Set<? extends ISnowflake> getForGuild(Guild g) {
        return blacklist.get(g);
    }

    public boolean isBlacklisted(Guild g, String targetID) {
        Set<ISnowflake> blacklistees = blacklist.get(g);

        return blacklistees != null &&
                blacklistees.size() != 0 &&
                blacklistees.stream().anyMatch(item -> item.getId().equals(targetID));
    }

    public boolean isBlacklisted(Member m) {
        Set<ISnowflake> blacklistees = blacklist.get(m.getGuild());

        return blacklistees != null &&
                blacklistees.size() != 0 &&
                blacklistees.stream().anyMatch(item ->
                        m.getRoles().stream().anyMatch(role ->
                                item.getId().equals(role.getId())) ||
                                item.getId().equals(m.getUser().getId()));
    }

    public <T extends ISnowflake & IMentionable> boolean isBlacklisted(Guild g, T entity) {
        return isBlacklisted(g, entity.getId());
    }

    public <T extends ISnowflake & IMentionable> String whitelist(Guild g, T entity) {
        blacklist.get(g).remove(entity);

        return confirmAction(Action.WHITELIST, g, entity);
    }
}
