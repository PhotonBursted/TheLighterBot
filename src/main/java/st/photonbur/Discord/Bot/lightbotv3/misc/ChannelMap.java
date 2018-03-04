package st.photonbur.Discord.Bot.lightbotv3.misc;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.*;
import java.util.stream.Collectors;

public class ChannelMap extends LinkedHashMap<TextChannel, Set<VoiceChannel>> {
    private static final Logger log = LoggerFactory.getLogger(ChannelMap.class);
    private static final Launcher l = Launcher.getInstance();

    private final String name;

    public ChannelMap(String name) {
        super();
        this.name = name;
    }

    public TextChannel getForVoiceChannel(VoiceChannel vc) {
        Map.Entry<TextChannel, Set<VoiceChannel>> textChannelEntry = entrySet().stream().filter(set -> set.getValue().contains(vc)).findFirst().orElse(null);

        return textChannelEntry == null ? null : textChannelEntry.getKey();
    }

    public void put(TextChannel tc, VoiceChannel vc) {
        if (keySet().contains(tc)) {
            get(tc).add(vc);

            if (name.equals("link")) l.getFileController().applyLinkAddition(tc, vc);
            if (name.equals("perm")) l.getFileController().applyPermAddition(tc, vc);
        } else {
            put(tc, new HashSet<>());
            put(tc, vc);
        }
    }

    public void remove(VoiceChannel vc) {
        Optional<Map.Entry<TextChannel, Set<VoiceChannel>>> optionalEntry = entrySet().stream().filter(set -> set.getValue().contains(vc)).findFirst();

        optionalEntry.ifPresent(entry -> {
            String result = String.format("The list now contains %s items: %s", entry.getValue().size() - 1, entry.getValue().stream().map(Channel::getName).collect(Collectors.joining(", ")));

            log.info(String.format("Removing \"%s\" from list of " + name + " channels\n% - s", vc.getName(), result));
            entry.getValue().remove(vc);

            if (name.equals("link")) l.getFileController().applyLinkDeletion(entry.getKey(), vc);
            if (name.equals("perm")) l.getFileController().applyPermDeletion(entry.getKey(), vc);

            if (entry.getValue().size() == 0) {
                log.info(String.format("Removing \"#%s\" from list of " + name + " channels", getForVoiceChannel(vc).getName()));
                remove(entry.getKey());
            }
        });
    }
}