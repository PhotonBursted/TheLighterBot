package st.photonbur.Discord.Bot.lightbotv3.misc.map.channel;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class LinkedChannelMap extends ChannelMap {
    public LinkedChannelMap() {
        super();
    }

    @Override
    protected void addToDatabase(TextChannel tc, VoiceChannel vc) {
        l.getFileController().applyLinkAddition(tc, vc);
    }

    @Override
    protected void deleteFromDatabase(TextChannel tc, VoiceChannel vc) {
        l.getFileController().applyLinkDeletion(tc, vc);
    }
}
