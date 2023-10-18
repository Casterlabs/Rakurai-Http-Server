package co.casterlabs.rhs.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LowercaseHashMap<T> implements Map<String, T> {
    protected Map<String, T> lowercased = new HashMap<>();
    protected Map<String, T> src;

    public LowercaseHashMap(Map<String, T> src) {
        this.src = Collections.unmodifiableMap(src);

        for (Entry<String, T> entry : this.src.entrySet()) {
            this.lowercased.put(entry.getKey().toLowerCase(), entry.getValue());
        }

        this.lowercased = Collections.unmodifiableMap(this.lowercased);
    }

    @Override
    public boolean containsKey(Object key) {
        return this.lowercased.containsKey(String.valueOf(key).toLowerCase());
    }

    @Override
    public boolean containsValue(Object value) {
        return this.src.containsValue(value);
    }

    @Override
    public Set<Entry<String, T>> entrySet() {
        return this.src.entrySet();
    }

    @Override
    public T get(Object key) {
        return this.lowercased.get(String.valueOf(key).toLowerCase());
    }

    @Override
    public boolean isEmpty() {
        return this.src.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return this.src.keySet();
    }

    @Override
    public int size() {
        return this.src.size();
    }

    @Override
    public Collection<T> values() {
        return this.src.values();
    }

    /* ---------------- */
    /* Unsupported      */
    /* ---------------- */

    @Override
    public T put(String key, T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

}
