package st.photonbur.Discord.Bot.lightbotv3.misc.map.channel;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import st.photonbur.Discord.Bot.lightbotv3.misc.map.DbSetMap;

abstract class ChannelMap extends DbSetMap<TextChannel, VoiceChannel> {
    public TextChannel getForVoiceChannel(VoiceChannel vc) {
        return getForValue(vc);
    }
}