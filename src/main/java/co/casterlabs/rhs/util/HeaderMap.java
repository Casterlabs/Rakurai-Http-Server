package co.casterlabs.rhs.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HeaderMap implements Map<String, List<String>> {
    private Map<String, List<String>> headers;

    private HeaderMap(Map<String, List<String>> src) {
        this.headers = Collections.unmodifiableMap(src);
    }

    public String getSingle(String key) {
        List<String> values = this.get(key);

        if (values == null) {
            return null;
        } else {
            return values.get(0);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return this.headers.containsKey(String.valueOf(key).toLowerCase());
    }

    @Override
    public boolean containsValue(Object value) {
        return this.headers.containsValue(value);
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return this.headers.entrySet();
    }

    @Override
    public List<String> get(Object key) {
        return this.headers.get(String.valueOf(key).toLowerCase());
    }

    @Override
    public boolean isEmpty() {
        return this.headers.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return this.headers.keySet();
    }

    @Override
    public int size() {
        return this.headers.size();
    }

    @Override
    public Collection<List<String>> values() {
        return this.headers.values();
    }

    /* ---------------- */
    /* Unsupported      */
    /* ---------------- */

    @Override
    public List<String> put(String key, List<String> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return this.headers.toString();
    }

    /* ---------------- */
    /* Builder          */
    /* ---------------- */

    public static class Builder {
        private Map<String, List<String>> headers = new HashMap<>();

        public Builder put(String key, String value) {
            this.getValueList(key.toLowerCase()).add(value);

            return this;
        }

        public Builder putAll(String key, String... values) {
            this.getValueList(key.toLowerCase()).addAll(Arrays.asList(values));

            return this;
        }

        public Builder putAll(String key, List<String> values) {
            this.getValueList(key.toLowerCase()).addAll(values);

            return this;
        }

        public Builder putMap(Map<String, List<String>> map) {
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                this.putAll(entry.getKey(), entry.getValue());
            }

            return this;
        }

        public Builder putSingleMap(Map<String, String> map) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                this.putAll(entry.getKey(), entry.getValue());
            }

            return this;
        }

        private List<String> getValueList(String key) {
            List<String> values = this.headers.get(key);

            if (values == null) {
                values = new ArrayList<>();

                this.headers.put(key, values);
            }

            return values;
        }

        public HeaderMap build() {
            for (Entry<String, List<String>> entry : this.headers.entrySet()) {
                entry.setValue(Collections.unmodifiableList(entry.getValue()));
            }

            return new HeaderMap(this.headers);
        }

    }

}
