package st.photonbur.Discord.Bot.lightbotv3.misc.map.channel;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class PermanentChannelMap extends ChannelMap {
    public PermanentChannelMap() {
        super();
    }

    @Override
    protected void addToDatabase(TextChannel tc, VoiceChannel vc) {
        l.getFileController().applyPermAddition(tc, vc);
    }

    @Override
    protected void deleteFromDatabase(TextChannel tc, VoiceChannel vc) {
        l.getFileController().applyPermDeletion(tc, vc);
    }
}
