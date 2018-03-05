package st.photonbur.Discord.Bot.lightbotv3.misc.map;

import st.photonbur.Discord.Bot.lightbotv3.controller.FileController;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;

import java.util.*;

public abstract class DbSetMap<K, V> extends LinkedHashMap<K, Set<V>> {
    protected static final Launcher l = Launcher.getInstance();

    protected abstract void addToDatabase(K key, V value);

    protected abstract void deleteFromDatabase(K key, V value);

    protected K getForValue(V value) {
        Map.Entry<K, Set<V>> mapEntry = entrySet().stream().filter(set -> set.getValue().contains(value)).findFirst().orElse(null);

        return mapEntry == null ? null : mapEntry.getKey();
    }

    public void putMerging(K tc, V vc) {
        if (keySet().contains(tc)) {
            get(tc).add(vc);

            if (FileController.shouldWriteToDb()) {
                addToDatabase(tc, vc);
            }
        } else {
            // Create a new key value pair so the voice channels linked to this text channel can be put into the map
            put(tc, new HashSet<>());

            // Since the key is now available, add the voice channel into the map
            putMerging(tc, vc);
        }
    }

    public void removeMerging(V value) {
        for (K key : keySet()) removeMerging(key, value);
    }

    public void removeMerging(K key, V value) {
        Optional<Map.Entry<K, Set<V>>> optionalEntry = entrySet().stream()
                .filter(set -> set.getValue().contains(value))
                .filter(set -> set.getKey().equals(key))
                .findFirst();

        optionalEntry.ifPresent(entry -> {
            entry.getValue().remove(value);

            if (FileController.shouldWriteToDb()) {
                deleteFromDatabase(entry.getKey(), value);
            }

            if (entry.getValue().size() == 0) {
                remove(entry.getKey());
            }
        });
    }
}
