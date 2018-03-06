package st.photonbur.Discord.Bot.lightbotv3.misc.map;

import java.util.Map;
import java.util.Optional;

abstract class DefaultedDbMap<K, V> extends GenericDbMap<K, V, V> {
    @Override
    public void putStoring(K key, V value) {
        put(key, value);
        addToDatabase(key, value);
    }

    @Override
    protected void removeEntryStoring(Map.Entry<K, V> entry, V value) {
        remove(entry.getKey(), value);
        deleteFromDatabase(entry.getKey(), value);
    }

    @Override
    public void removeByKeyStoring(K key) {
        Optional<Map.Entry<K, V>> optionalEntry = entrySet().stream()
                .filter(entry -> entry.getKey().equals(key))
                .findFirst();

        optionalEntry.ifPresent(entry -> {
            remove(entry.getKey());
            deleteFromDatabase(entry.getKey(), entry.getValue());
        });
    }

    @Override
    public void removeByValueStoring(V value) {
        Optional<Map.Entry<K, V>> optionalEntry = entrySet().stream()
                .filter(entry -> entry.getValue().equals(value))
                .findFirst();

        optionalEntry.ifPresent(entry -> removeEntryStoring(entry, value));
    }

    @Override
    public void removeStoring(K key, V value) {
        Optional<Map.Entry<K, V>> optionalEntry = entrySet().stream()
                .filter(entry -> entry.getKey().equals(key))
                .filter(entry -> entry.getValue().equals(value))
                .findFirst();

        optionalEntry.ifPresent(entry -> removeEntryStoring(entry, value));
    }
}
