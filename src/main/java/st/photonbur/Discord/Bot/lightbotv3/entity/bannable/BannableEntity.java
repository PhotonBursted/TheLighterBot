package st.photonbur.Discord.Bot.lightbotv3.entity.bannable;

import net.dv8tion.jda.core.entities.ISnowflake;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.function.Function;

/**
 * Class used to better handle entities of which the access to the bot can be limited.
 */
public class BannableEntity<T extends ISnowflake> implements ISnowflake {
    protected final T entity;
    protected static final Launcher l = Launcher.getInstance();

    BannableEntity(T entity) {
        this.entity = entity;
    }

    BannableEntity(long entityId, Function<Long, T> retrievalFunction) {
        this.entity = retrievalFunction.apply(entityId);
    }

    BannableEntity(String entityId, Function<String, T> retrievalFunction) {
        this.entity = retrievalFunction.apply(entityId);
    }

    public T get() {
        return entity;
    }

    public boolean isOfClass(Class<? extends ISnowflake> clazz) {
        return clazz.isAssignableFrom(entity.getClass());
    }

    @Override
    public long getIdLong() {
        return entity.getIdLong();
    }
}