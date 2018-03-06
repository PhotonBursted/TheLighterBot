package st.photonbur.Discord.Bot.lightbotv3.entity.bannable;

import net.dv8tion.jda.core.entities.ISnowflake;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BannableEntity)) return false;

        BannableEntity<?> that = (BannableEntity<?>) o;
        return Objects.equals(entity.getIdLong(), that.entity.getIdLong());
    }

    public T get() {
        return entity;
    }

    @Override
    public long getIdLong() {
        return entity.getIdLong();
    }

    public boolean isOfClass(Class<? extends ISnowflake> clazz) {
        return clazz.isAssignableFrom(entity.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity);
    }
}