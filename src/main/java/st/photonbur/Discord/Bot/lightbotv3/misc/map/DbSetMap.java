package st.photonbur.Discord.Bot.lightbotv3.misc.map;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class DbSetMap<K, V> extends GenericDbMap<K, V, Set<V>> {
    protected K getForValue(V value) {
        Map.Entry<K, Set<V>> mapEntry = entrySet().stream().filter(set -> set.getValue().contains(value)).findFirst().orElse(null);

        return mapEntry == null ? null : mapEntry.getKey();
    }

    @Override
    public void putStoring(K key, V value) {
        if (keySet().contains(key)) {
            get(key).add(value);
            addToDatabase(key, value);
        } else {
            // Create a new key value pair so the voice channels linked to this text channel can be put into the map
            put(key, new HashSet<>());

            // Since the key is now available, add the voice channel into the map
            putStoring(key, value);
        }
    }

    @Override
    protected void removeEntryStoring(Map.Entry<K, Set<V>> entry, V value) {
        entry.getValue().remove(value);
        deleteFromDatabase(entry.getKey(), value);

        if (entry.getValue().size() == 0) {
            remove(entry.getKey());
        }
    }

    @Override
    public void removeByKeyStoring(K key) {
        Optional<Map.Entry<K, Set<V>>> optionalEntry = entrySet().stream()
                .filter(entry -> entry.getKey().equals(key))
                .findFirst();

        optionalEntry.ifPresent(entry -> remove(entry.getKey()));
    }

    @Override
    public void removeByValueStoring(V value) {
        Optional<Map.Entry<K, Set<V>>> optionalEntry = entrySet().stream()
                .filter(entry -> entry.getValue().contains(value))
                .findFirst();

        optionalEntry.ifPresent(entry -> removeEntryStoring(entry, value));
    }

    @Override
    public void removeStoring(K key, V value) {
        Optional<Map.Entry<K, Set<V>>> optionalEntry = entrySet().stream()
                .filter(entry -> entry.getKey().equals(key))
                .filter(entry -> entry.getValue().contains(value))
                .findFirst();

        optionalEntry.ifPresent(entry -> removeEntryStoring(entry, value));
    }
}
