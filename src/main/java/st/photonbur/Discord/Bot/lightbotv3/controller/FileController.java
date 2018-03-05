package st.photonbur.Discord.Bot.lightbotv3.controller;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableRole;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableUser;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.sql.*;
import java.util.function.Consumer;

public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private Connection conn;

    private static FileController instance;
    private final Launcher l;

    private FileController(String dbUrl, String dbPort, String dbName, String dbUser, String dbPass) {
        this.l = Launcher.getInstance();

        try {
            conn = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s", dbUrl, dbPort, dbName), dbUser, dbPass);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * As part of the Singleton design pattern, no clones of this instance are permitted.
     *
     * @return nothing
     * @throws CloneNotSupportedException No clones of this instance are permitted
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public static synchronized FileController getInstance() {
        return getInstance(null, null, null, null, null);
    }

    public static synchronized FileController getInstance(String dbUrl, String dbPort, String dbName, String dbUser, String dbPass) {
        if (instance == null) {
            instance = new FileController(dbUrl, dbPort, dbName, dbUser, dbPass);
        }

        return instance;
    }

    private void applyAccessListAddition(String tableName, Guild g, BannableEntity entity) throws SQLException {
        executeChangeQuery("INSERT INTO ? VALUES (?, ?, ?)", stmt -> {
            try {
                stmt.setString(1, tableName);
                stmt.setLong(2, g.getIdLong());
                stmt.setLong(3, entity.getIdLong());
                stmt.setString(4, entity.get().getClass().getSimpleName().toLowerCase());
            } catch (SQLException ex) {
                log.error("Something went wrong when setting parameters for an access list addition", ex);
            }
        });
    }

    private void applyAccessListDeletion(String tableName, Guild g, BannableEntity entity) throws SQLException {
        executeChangeQuery("DELETE FROM ? WHERE server_id = ? AND entity_id = ?", stmt -> {
            try {
                stmt.setString(1, tableName);
                stmt.setLong(2, g.getIdLong());
                stmt.setLong(3, entity.getIdLong());
            } catch (SQLException ex) {
                log.error("Something went wrong when setting parameters for an access list addition", ex);
            }
        });
    }

    public void applyBlacklistAddition(Guild g, BannableEntity entity) {
        try {
            applyAccessListAddition("Blacklists", g, entity);
        } catch (SQLException ex) {
            log.error("Something went wrong applying a blacklist addition", ex);
        }
    }

    public void applyBlacklistDeletion(Guild g, BannableEntity entity) {
        try {
            applyAccessListDeletion("Blacklists", g, entity);
        } catch (SQLException ex) {
            log.error("Something went wrong applying a blacklist deletion", ex);
        }
    }

    public void applyDefaultCategoryAddition(Guild g, Category c) {
        try {
            executeChangeQuery("INSERT INTO \"DefaultCategories\" VALUES (?, ?)", stmt -> {
                try {
                    stmt.setLong(1, g.getIdLong());
                    stmt.setLong(2, c.getIdLong());
                } catch (SQLException ex) {
                    log.error("Something went wrong when setting parameters for a default category addition", ex);
                }
            });
        } catch (SQLException ex) {
            log.error("Something went wrong applying a default category addition", ex);
        }
    }

    public void applyDefaultCategoryDeletion(Guild g) {
        try {
            executeChangeQuery("DELETE FROM \"DefaultCategories\" WHERE server_id = ?", stmt -> {
                try {
                    stmt.setLong(1, g.getIdLong());
                } catch (SQLException ex) {
                    log.error("Something went wrong when setting parameters for a default category deletion", ex);
                }
            });
        } catch (SQLException ex) {
            log.error("Something went wrong applying a default category deletion", ex);
        }
    }

    public void applyLinkAddition(TextChannel tc, VoiceChannel vc) {
        try {
            executeChangeQuery("INSERT INTO \"LinkedChannels\" VALUES (?, ?)", stmt -> {
                try {
                    stmt.setLong(1, tc.getIdLong());
                    stmt.setLong(2, vc.getIdLong());
                } catch (SQLException ex) {
                    log.error("Something went wrong when setting parameters for a linked channel addition", ex);
                }
            });
        } catch (SQLException ex) {
            log.error("Something went wrong applying a linked channel addition", ex);
        }
    }

    public void applyLinkDeletion(TextChannel tc, VoiceChannel vc) {
        try {
            executeChangeQuery("DELETE FROM \"LinkedChannels\" WHERE tc_id = ? AND vc_id = ?", stmt -> {
                try {
                    stmt.setLong(1, tc.getIdLong());
                    stmt.setLong(2, vc.getIdLong());
                } catch (SQLException ex) {
                    log.error("Something went wrong when setting parameters for a linked channel deletion", ex);
                }
            });
        } catch (SQLException ex) {
            log.error("Something went wrong applying a linked channel deletion", ex);
        }
    }

    public void applyPermAddition(TextChannel tc, VoiceChannel vc) {
        try {
            executeChangeQuery("INSERT INTO \"PermChannels\" VALUES (?, ?)", stmt -> {
                try {
                    stmt.setLong(1, tc.getIdLong());
                    stmt.setLong(2, vc.getIdLong());
                } catch (SQLException ex) {
                    log.error("Something went wrong when setting parameters for a permanent channel addition", ex);
                }
            });
        } catch (SQLException ex) {
            log.error("Something went wrong applying a permanent channel addition", ex);
        }
    }

    public void applyPermDeletion(TextChannel tc, VoiceChannel vc) {
        try {
            executeChangeQuery("DELETE FROM \"PermChannels\" WHERE tc_id = ? AND vc_id = ?", stmt -> {
                try {
                    stmt.setLong(1, tc.getIdLong());
                    stmt.setLong(2, vc.getIdLong());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        } catch (SQLException ex) {
            log.error("Something went wrong applying a permanent channel deletion", ex);
        }
    }

    public void applyWhitelistAddition(Guild g, BannableEntity entity) {
        try {
            applyAccessListAddition("Whitelists", g, entity);
        } catch (SQLException ex) {
            log.error("Something went wrong applying a whitelist addition", ex);
        }
    }

    public void applyWhitelistDeletion(Guild g, BannableEntity entity) {
        try {
            applyAccessListDeletion("Whitelists", g, entity);
        } catch (SQLException ex) {
            log.error("Something went wrong applying a whitelist deletion", ex);
        }
    }

    private void executeChangeQuery(String sql, Consumer<PreparedStatement> statementConsumer) throws SQLException {
        PreparedStatement query = conn.prepareStatement(sql);
        statementConsumer.accept(query);

        query.execute();
        query.close();
    }

    public void loadEverything() {
        log.info("Loading all guild data...");

        loadAllDataFromDatabase();

        log.info(String.format("Done processing guild files.\n\nFound %d pairs of permanent and %d pairs of linked channels.",
                l.getChannelController().getPermChannels().size(),
                l.getChannelController().getLinkedChannels().size()));
    }

    private void loadAllDataFromDatabase() {
        try {
            if (!conn.isClosed()) {
                retrieveBlacklist();
                retrieveWhitelist();

                retrieveDefaultCategories();

                retrieveLinkedChannels();
                retrievePermChannels();
            }
        } catch (SQLException ex) {
            log.error("Something went wrong retrieving data from the database", ex);
        }
    }

    private void retrieveBlacklist() throws SQLException {
        retrieveData(
                "SELECT bl.*\n" +
                        "FROM \"Servers\" AS s\n" +
                        "  LEFT JOIN \"Blacklists\" AS bl ON bl.server_id = s.discord_id\n" +
                        "WHERE bl.server_id IS NOT NULL",
                result -> {
                    try {
                        Guild g = l.getBot().getGuildById(result.getLong("server_id"));
                        long entityId = result.getLong("entity_id");
                        String entityType = result.getString("entity_type");

                        switch (entityType) {
                            case "role":
                                l.getAccesslistController().blacklist(g, new BannableRole(entityId), false);
                                break;
                            case "user":
                                l.getAccesslistController().blacklist(g, new BannableUser(entityId), false);
                                break;
                            default:
                        }
                    } catch (SQLException ex) {
                        log.error("Something went wrong retrieving the blacklist", ex);
                    }
                });
    }

    private void retrieveData(String sql, Consumer<ResultSet> resultConsumer) throws SQLException {
        PreparedStatement query = conn.prepareStatement(sql);

        ResultSet resultSet = query.executeQuery();
        while (resultSet.next()) resultConsumer.accept(resultSet);

        resultSet.close();
        query.close();
    }

    private void retrieveDefaultCategories() throws SQLException {
        retrieveData(
                "SELECT dc.*\n" +
                        "FROM \"Servers\" AS s\n" +
                        "  LEFT JOIN \"DefaultCategories\" AS dc ON dc.server_id = s.discord_id\n" +
                        "WHERE dc.server_id IS NOT NULL",
                result -> {
                    try {
                        Guild g = l.getBot().getGuildById(result.getLong("server_id"));
                        long entityId = result.getLong("category_id");

                        Category c = l.getBot().getCategoryById(entityId);

                        if (g != null && c != null) {
                            l.getChannelController().getCategories().put(g, c);
                        }
                    } catch (SQLException ex) {
                        log.error("Something went wrong retrieving the default categories", ex);
                    }
                });
    }

    private void retrieveLinkedChannels() throws SQLException {
        retrieveData(
                "SELECT *\n" +
                        "FROM \"LinkedChannels\"",
                result -> {
                    try {
                        TextChannel tc = l.getBot().getTextChannelById(result.getLong("tc_id"));
                        VoiceChannel vc = l.getBot().getVoiceChannelById(result.getLong("vc_id"));

                        if (tc != null && vc != null) {
                            l.getChannelController().getLinkedChannels().put(tc, vc);
                        }
                    } catch (SQLException ex) {
                        log.error("Something went wrong retrieving the linked channels", ex);
                    }
                });
    }

    private void retrievePermChannels() throws SQLException {
        retrieveData(
                "SELECT *\n" +
                        "FROM \"PermChannels\"",
                result -> {
                    try {
                        TextChannel tc = l.getBot().getTextChannelById(result.getLong("tc_id"));
                        VoiceChannel vc = l.getBot().getVoiceChannelById(result.getLong("vc_id"));

                        if (tc != null && vc != null) {
                            l.getChannelController().getPermChannels().put(tc, vc);
                        }
                    } catch (SQLException ex) {
                        log.error("Something went wrong retrieving the permanent channels", ex);
                    }
                });
    }

    private void retrieveWhitelist() throws SQLException {
        retrieveData(
                "SELECT wl.*\n" +
                        "FROM \"Servers\" AS s\n" +
                        "  LEFT JOIN \"Whitelists\" AS wl ON wl.server_id = s.discord_id\n" +
                        "WHERE wl.server_id IS NOT NULL",
                result -> {
                    try {
                        Guild g = l.getBot().getGuildById(result.getLong("server_id"));
                        long entityId = result.getLong("entity_id");
                        String entityType = result.getString("entity_type");

                        switch (entityType) {
                            case "role":
                                l.getAccesslistController().whitelist(g, new BannableRole(entityId), false);
                                break;
                            case "user":
                                l.getAccesslistController().whitelist(g, new BannableUser(entityId), false);
                                break;
                            default:
                        }
                    } catch (SQLException ex) {
                        log.error("Something went wrong retrieving the whitelist", ex);
                    }
                });
    }

    public void shutdown() {
        try {
            if (!conn.isClosed()) conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
