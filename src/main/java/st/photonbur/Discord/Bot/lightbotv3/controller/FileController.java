package st.photonbur.Discord.Bot.lightbotv3.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.io.*;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static FileController instance;
    private final Launcher l;

    private FileController() {
        this.l = Launcher.getInstance();
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
        if (instance == null) {
            instance = new FileController();
        }

        return instance;
    }

    public void readAllGuilds() {
        File directory = new File("guilds/");

        if (!directory.exists()) {
            if (!directory.mkdir()) {
                log.error("Couldn't create the guild directory!\n" +
                        "Check writing permissions of the bot in the directory it is in.");
            }
        }

        File[] guildFiles = directory.listFiles((dir, name) -> name.endsWith(".guild.json"));
        log.info("Processing all guild files...");
        if (guildFiles != null) {
            for (File file : guildFiles) {
                readGuild(file);
            }
        }
        log.info(String.format("Done processing guild files.\n\nFound %d pairs of permanent and %d pairs of linked channels.",
                l.getChannelController().getPermChannels().size(), l.getChannelController().getLinkedChannels().size()));
    }

    private void readGuild(File file) {
        try {
            JsonObject guildObject = new JsonParser().parse(new BufferedReader(new FileReader(file))).getAsJsonObject();
            Guild g = l.getBot().getGuildById(file.getName().substring(0, file.getName().indexOf(".")));

            if (guildObject.has("blacklist")) {
                guildObject.get("blacklist").getAsJsonArray().iterator().forEachRemaining(elem -> {
                    String entity = elem.getAsString();

                    if (entity.startsWith("role")) {
                        l.getBlacklistController().blacklist(g, l.getBot().getRoleById(entity.split("\\|")[1]));
                    } else if (entity.startsWith("user")) {
                        l.getBlacklistController().blacklist(g, l.getBot().getUserById(entity.split("\\|")[1]));
                    }
                });
            }

            if (guildObject.has("defCategory")) {
                Category c = l.getBot().getCategoryById(guildObject.get("defCategory").getAsString());

                if (g != null && c != null) {
                    l.getChannelController().getCategories().put(g, c);
                }
            }

            if (guildObject.has("linked")) {
                guildObject.get("linked").getAsJsonObject().entrySet().forEach(set -> {
                    TextChannel tc = l.getBot().getTextChannelById(set.getKey());

                    Set<VoiceChannel> vcs = Collections.emptySet();
                    try {
                        set.getValue().getAsJsonArray().iterator().forEachRemaining(vcId ->
                        {
                            VoiceChannel vc = l.getBot().getVoiceChannelById(vcId.getAsLong());
                            if (vc != null) {
                                vcs.add(vc);
                            }
                        });

                        if (vcs.size() > 0 && tc != null) {
                            l.getChannelController().getLinkedChannels().put(tc, vcs);
                        }
                    } catch (IllegalStateException ex) {
                        log.error("Something went wrong while loading the guild state file.", ex);
                    }
                });
            }

            if (guildObject.has("perm")) {
                guildObject.get("perm").getAsJsonObject().entrySet().forEach(set -> {
                    TextChannel tc = l.getBot().getTextChannelById(set.getKey());

                    Set<VoiceChannel> vcs = Collections.emptySet();
                    set.getValue().getAsJsonArray().iterator().forEachRemaining(vcId ->
                    {
                        VoiceChannel vc = l.getBot().getVoiceChannelById(vcId.getAsLong());
                        if (vc != null) {
                            vcs.add(vc);
                        }
                    });

                    if (vcs.size() > 0 && tc != null) {
                        l.getChannelController().getPermChannels().put(tc, vcs);
                    }
                });
            }
        } catch (FileNotFoundException ex) {
            log.error("Loading the guild's state file failed.", ex);
        }
    }

    public void saveGuild(Guild g) {
        File dest = new File("guilds/" + g.getId() + ".guild.json");

        WeakHashMap<TextChannel, Set<VoiceChannel>> linkedChannelPairs = l.getChannelController().getLinkedChannelsForGuild(g);
        WeakHashMap<TextChannel, Set<VoiceChannel>> permChannelPairs = l.getChannelController().getPermChannelsForGuild(g);

        if (linkedChannelPairs.size() > 0 || permChannelPairs.size() > 0 || l.getChannelController().getCategories().containsKey(g)) {
            try (JsonWriter jw = new JsonWriter(new BufferedWriter(new FileWriter(dest)))) {
                if (dest.createNewFile()) {
                    log.info("Created new file for saving guild " + g.getId());
                }

                jw.setIndent("  ");
                jw.beginObject();

                if (l.getBlacklistController().getForGuild(g) != null) {
                    jw.name("blacklist").beginArray();
                    l.getBlacklistController().getForGuild(g).forEach(entity -> {
                        try {
                            jw.value(entity.getClass().getSimpleName().toLowerCase().replace("impl", "") + "|" + entity.getId());
                        } catch (IOException ex) {
                            log.error("Writing to the guild's config file failed.", ex);
                        }
                    });
                    jw.endArray();
                }

                if (l.getChannelController().getCategories().containsKey(g)) {
                    jw.name("defCategory").value(l.getChannelController().getCategories().get(g).getId());
                }

                if (linkedChannelPairs.size() > 0) {
                    jw.name("linked").beginObject();
                    writeChannelSet(linkedChannelPairs, jw);
                    jw.endObject();
                }

                if (permChannelPairs.size() > 0) {
                    jw.name("perm").beginObject();
                    writeChannelSet(permChannelPairs, jw);
                    jw.endObject();
                }

                jw.endObject();
            } catch (IOException ex) {
                log.error("Writing to the guild's config file failed.", ex);
            }
        } else {
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
        }
    }

    private void writeChannelSet(WeakHashMap<TextChannel, Set<VoiceChannel>> channelSets, JsonWriter jw) {
        channelSets.forEach((tc, vcs) -> {
            try {
                jw.name(tc.getId());
                jw.beginArray();

                for (VoiceChannel vc : vcs) {
                    jw.value(vc.getId());
                }

                jw.endArray();
            } catch (IOException ex) {
                log.error("Writing a channel set to the guild's config file failed.", ex);
            }
        });
    }
}
