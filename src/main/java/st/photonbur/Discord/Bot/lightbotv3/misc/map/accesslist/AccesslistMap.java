package st.photonbur.Discord.Bot.lightbotv3.misc.map.accesslist;

import net.dv8tion.jda.core.entities.Guild;
import st.photonbur.Discord.Bot.lightbotv3.entity.bannable.BannableEntity;
import st.photonbur.Discord.Bot.lightbotv3.misc.map.DbSetMap;

abstract class AccesslistMap extends DbSetMap<Guild, BannableEntity> { }
