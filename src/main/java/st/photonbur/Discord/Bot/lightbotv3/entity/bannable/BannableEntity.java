package st.photonbur.Discord.Bot.lightbotv3.entity.bannable;

import net.dv8tion.jda.core.entities.ISnowflake;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.function.Function;

/**
 * Class used to better handle entities of which the access to the bot can be limited.
 */
public class BannableEntity<T extends ISnowflake> implements ISnowflake {
    private final T entity;
    protected Launcher l = Launcher.getInstance();

    public BannableEntity(T entity) {
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

    public String getClassName() {
        return entity.getClass().getSimpleName();
    }

    @Override
    public long getIdLong() {
        return entity.getIdLong();
    }

    public boolean isOfClass(Class clazz) {
        return entity.getClass() == clazz;
    }
}