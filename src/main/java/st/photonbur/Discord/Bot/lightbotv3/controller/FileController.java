package st.photonbur.Discord.Bot.lightbotv3.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;

import java.io.*;
import java.util.WeakHashMap;

public class FileController {
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
        File directory = new File("/guilds");

        if (!directory.exists()) {
            if (!directory.mkdir()) {
                Logger.log("Couldn't create the guild directory!\n" +
                        "Check writing permissions of the bot in the directory it is in.");
            }
        }

        File[] guildFiles = directory.listFiles((dir, name) -> dir.isFile() && name.endsWith(".guild.json"));
        if (guildFiles != null) {
            for (File file : guildFiles) {
                readGuild(file);
            }
        }
    }

    private void readGuild(File file) {
        try {
            JsonObject guildObject = new JsonParser().parse(new BufferedReader(new FileReader(file))).getAsJsonObject();

            if (guildObject.has("defCategory")) {
                Guild g = l.getBot().getGuildById(file.getName().substring(0, file.getName().indexOf(".")));
                Category c = l.getBot().getCategoryById(guildObject.get("defCategory").getAsString());

                if (g != null && c != null) {
                    l.getChannelController().getCategories().put(g, c);
                }
            }

            if (guildObject.has("linked")) {
                guildObject.get("linked").getAsJsonObject().entrySet().forEach((pair) -> {
                    VoiceChannel vc = l.getBot().getVoiceChannelById(pair.getKey());
                    TextChannel tc = l.getBot().getTextChannelById(pair.getValue().getAsString());

                    if (vc != null && tc != null) {
                        l.getChannelController().getLinkedChannels().put(vc, tc);
                    }
                });
            }

            if (guildObject.has("perm")) {
                guildObject.get("perm").getAsJsonObject().entrySet().forEach((pair) -> {
                    VoiceChannel vc = l.getBot().getVoiceChannelById(pair.getKey());
                    TextChannel tc = l.getBot().getTextChannelById(pair.getValue().getAsString());

                    if (vc != null && tc != null) {
                        l.getChannelController().getPermChannels().put(vc, tc);
                    }
                });
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(Logger.out);
        }
    }

    public void saveGuild(Guild g) {
        File dest = new File("/guilds/" + g.getId() + ".guild.json");
        WeakHashMap<VoiceChannel, TextChannel> linkedChannelPairs = l.getChannelController().getLinkedChannelsForGuild(g);
        WeakHashMap<VoiceChannel, TextChannel> permChannelPairs = l.getChannelController().getPermChannelsForGuild(g);

        if (linkedChannelPairs.size() > 0 || permChannelPairs.size() > 0 || l.getChannelController().getCategories().containsKey(g)) {
            try (JsonWriter jw = new JsonWriter(new BufferedWriter(new FileWriter(dest)))) {
                jw.setIndent("  ");
                jw.beginObject();

                if (l.getChannelController().getCategories().containsKey(g)) {
                    jw.name("defCategory").value(l.getChannelController().getCategories().get(g).getId());
                }

                if (linkedChannelPairs.size() > 0) {
                    jw.name("linked").beginObject();
                    linkedChannelPairs.forEach((vc, tc) -> {
                        try {
                            jw.name(vc.getId()).value(tc.getId());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                    jw.endObject();
                }

                if (permChannelPairs.size() > 0) {
                    jw.name("perm").beginObject();
                    permChannelPairs.forEach((vc, tc) -> {
                        try {
                            jw.name(vc.getId()).value(tc.getId());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                    jw.endObject();
                }
                jw.endObject();

                jw.endObject();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
