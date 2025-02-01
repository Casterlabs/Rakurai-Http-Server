package co.casterlabs.rhs.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CaseInsensitiveMultiMap<T> implements Map<String, List<T>> {
    private static final CaseInsensitiveMultiMap<Object> EMPTY = new CaseInsensitiveMultiMap<Object>(Collections.emptyMap());

    private Map<String, List<T>> raw;
    private Map<String, List<T>> caseInsensitive;

    public CaseInsensitiveMultiMap(Map<String, List<T>> src) {
        this.raw = Collections.unmodifiableMap(src);

        this.caseInsensitive = new HashMap<>();
        for (Map.Entry<String, List<T>> entry : this.raw.entrySet()) {
            this.caseInsensitive.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        this.caseInsensitive = Collections.unmodifiableMap(this.caseInsensitive);
    }

    /* ---------------- */
    /* Case Insensitive */
    /* ---------------- */

    public T getSingle(String key) {
        List<T> values = this.get(key);
        if (values == null) {
            return null;
        } else {
            return values.get(0);
        }
    }

    public T getSingleOrDefault(String key, T defaultValue) {
        List<T> values = this.get(key);
        if (values == null) {
            return defaultValue;
        } else {
            return values.get(0);
        }
    }

    @Override
    public List<T> get(Object key) {
        return this.caseInsensitive.get(String.valueOf(key).toLowerCase());
    }

    @Override
    public boolean containsKey(Object key) {
        return this.caseInsensitive.containsKey(String.valueOf(key).toLowerCase());
    }

    /* ---------------- */
    /* Case Sensitive   */
    /* ---------------- */

    @Override
    public boolean containsValue(Object value) {
        return this.raw.containsValue(value);
    }

    @Override
    public Set<Entry<String, List<T>>> entrySet() {
        return this.raw.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return this.raw.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return this.raw.keySet();
    }

    @Override
    public int size() {
        return this.raw.size();
    }

    @Override
    public Collection<List<T>> values() {
        return this.raw.values();
    }

    /* ---------------- */
    /* Unsupported      */
    /* ---------------- */

    @Override
    public List<T> put(String key, List<T> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<T>> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return this.raw.toString();
    }

    /* ---------------- */
    /* Builder          */
    /* ---------------- */

    public static class Builder<T> {
        private Map<String, List<T>> headers = new HashMap<>();

        public Builder<T> put(String key, T value) {
            this.getValueList(key).add(value);
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder<T> putAll(String key, T... values) {
            this.getValueList(key).addAll(Arrays.asList(values));
            return this;
        }

        public Builder<T> putAll(String key, List<T> values) {
            this.getValueList(key).addAll(values);
            return this;
        }

        public Builder<T> putMap(Map<String, List<T>> map) {
            for (Map.Entry<String, List<T>> entry : map.entrySet()) {
                this.putAll(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder<T> putSingleMap(Map<String, T> map) {
            for (Map.Entry<String, T> entry : map.entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        private List<T> getValueList(String key) {
            List<T> values = this.headers.get(key);
            if (values == null) {
                values = new ArrayList<>();

                this.headers.put(key, values);
            }
            return values;
        }

        public CaseInsensitiveMultiMap<T> build() {
            for (Entry<String, List<T>> entry : this.headers.entrySet()) {
                entry.setValue(
                    Collections.unmodifiableList(
                        new ArrayList<>(entry.getValue()) // We convert to ArrayList for faster access.
                    )
                );
            }
            return new CaseInsensitiveMultiMap<T>(this.headers);
        }

    }

    /* ---------------- */
    /* Util             */
    /* ---------------- */

    @SuppressWarnings("unchecked")
    public static <T> CaseInsensitiveMultiMap<T> emptyMap() {
        return (CaseInsensitiveMultiMap<T>) EMPTY;
    }

}
