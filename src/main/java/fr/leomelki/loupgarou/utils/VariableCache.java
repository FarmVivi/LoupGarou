package fr.leomelki.loupgarou.utils;

import java.util.HashMap;

public class VariableCache {
    private final HashMap<String, Object> cache = new HashMap<>();

    public boolean getBoolean(String key) {
        Object object = get(key);
        if (object instanceof Boolean)
            return (boolean) object;
        return object != null;
    }

    public void set(String key, Object value) {
        if (cache.containsKey(key))
            cache.replace(key, value);
        else
            cache.put(key, value);
    }

    public boolean has(String key) {
        return cache.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(String key) {
        return (T) cache.remove(key);
    }

    public void reset() {
        cache.clear();
    }
}