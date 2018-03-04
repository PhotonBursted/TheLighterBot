package st.photonbur.Discord.Bot.lightbotv3.misc.channelmap;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.*;

abstract class ChannelMap extends LinkedHashMap<TextChannel, Set<VoiceChannel>> {
    private static final Logger log = LoggerFactory.getLogger(ChannelMap.class);
    protected static final Launcher l = Launcher.getInstance();

    private final String name;

    ChannelMap(String name) {
        super();
        this.name = name;
    }

    abstract void addToDatabase(TextChannel tc, VoiceChannel vc);

    abstract void deleteFromDatabase(TextChannel tc, VoiceChannel vc);

    public TextChannel getForVoiceChannel(VoiceChannel vc) {
        Map.Entry<TextChannel, Set<VoiceChannel>> textChannelEntry = entrySet().stream().filter(set -> set.getValue().contains(vc)).findFirst().orElse(null);

        return textChannelEntry == null ? null : textChannelEntry.getKey();
    }

    public void put(TextChannel tc, VoiceChannel vc) {
        if (keySet().contains(tc)) {
            get(tc).add(vc);
            addToDatabase(tc, vc);
        } else {
            // Create a new key value pair so the voice channels linked to this text channel can be put into the map
            put(tc, new HashSet<>());

            // Since the key is now available, add the voice channel into the map
            put(tc, vc);
        }
    }

    public void remove(VoiceChannel vc) {
        Optional<Map.Entry<TextChannel, Set<VoiceChannel>>> optionalEntry = entrySet().stream().filter(set -> set.getValue().contains(vc)).findFirst();

        optionalEntry.ifPresent(entry -> {
            log.info(String.format("Removing \"%s\" from list of " + name + " channels", vc.getName()));
            entry.getValue().remove(vc);

            deleteFromDatabase(entry.getKey(), vc);

            if (entry.getValue().size() == 0) {
                log.info(String.format("Removing \"#%s\" from list of " + name + " channels", entry.getKey().getName()));
                remove(entry.getKey());
            }
        });
    }
}