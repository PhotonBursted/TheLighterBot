package st.photonbur.Discord.Bot.lightbotv3.misc.map;

import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.LinkedHashMap;
import java.util.Map;

abstract class GenericDbMap<K, VA, VM> extends LinkedHashMap<K, VM> {
    protected static final Launcher l = Launcher.getInstance();

    protected abstract void addToDatabase(K key, VA value);

    protected abstract void deleteFromDatabase(K key, VA value);

    public abstract void putStoring(K key, VA value);

    protected abstract void removeEntryStoring(Map.Entry<K, VM> entry, VA value);

    public abstract void removeByKeyStoring(K key);

    public abstract void removeByValueStoring(VA value);

    public abstract void removeStoring(K key, VA value);
}
