package st.photonbur.Discord.Bot.lightbotv3.misc;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChannelMap extends LinkedHashMap<TextChannel, Set<VoiceChannel>> {
    private static final Logger log = LoggerFactory.getLogger(ChannelMap.class);

    private final String name;

    public ChannelMap(String name) {
        super();
        this.name = name;
    }

    public TextChannel getForVoiceChannel(VoiceChannel vc) {
        Map.Entry<TextChannel, Set<VoiceChannel>> textChannelEntry = entrySet().stream().filter(set -> set.getValue().contains(vc)).findFirst().orElse(null);

        return textChannelEntry == null ? null : textChannelEntry.getKey();
    }

    public void put(TextChannel key, VoiceChannel value) {
        if (keySet().contains(key)) {
            get(key).add(value);
        } else {
            put(key, Collections.singleton(value));
        }
    }

    public void remove(VoiceChannel vc) {
        Optional<Map.Entry<TextChannel, Set<VoiceChannel>>> optionalEntry = entrySet().stream().filter(set -> set.getValue().contains(vc)).findFirst();

        optionalEntry.ifPresent(entry -> {
            log.info(String.format("Removing \"%s\" from list of " + name + " channels", vc.getName()));
            entry.getValue().remove(vc);

            if (entry.getValue().size() == 0) {
                log.info(String.format("Removing \"#%s\" from list of " + name + " channels", getForVoiceChannel(vc).getName()));
                remove(entry.getKey());
            }
        });
    }
}