package st.photonbur.Discord.Bot.lightbotv3.entity.permissible;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.IPermissionHolder;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Class used to better handle entities of which the access to the bot can be limited.
 */
public class PermissibleEntity<T extends IPermissionHolder> implements IPermissionHolder {
    protected final T entity;
    protected static final Launcher l = Launcher.getInstance();

    PermissibleEntity(T entity) {
        this.entity = entity;
    }

    PermissibleEntity(long entityId, Function<Long, T> retrievalFunction) {
        this.entity = retrievalFunction.apply(entityId);
    }

    PermissibleEntity(String entityId, Function<String, T> retrievalFunction) {
        this.entity = retrievalFunction.apply(entityId);
    }

    public T get() {
        return entity;
    }

    public boolean isOfClass(Class<? extends IPermissionHolder> clazz) {
        return clazz.isAssignableFrom(entity.getClass());
    }

    @Override
    public Guild getGuild() {
        return entity.getGuild();
    }

    @Override
    public List<Permission> getPermissions() {
        return entity.getPermissions();
    }

    @Override
    public boolean hasPermission(Permission... permissions) {
        return entity.hasPermission(permissions);
    }

    @Override
    public boolean hasPermission(Collection<Permission> collection) {
        return entity.hasPermission(collection);
    }

    @Override
    public boolean hasPermission(Channel channel, Permission... permissions) {
        return entity.hasPermission(channel, permissions);
    }

    @Override
    public boolean hasPermission(Channel channel, Collection<Permission> collection) {
        return entity.hasPermission(channel, collection);
    }
}