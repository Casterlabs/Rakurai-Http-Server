package co.casterlabs.rhs.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CaseInsensitiveMultiMap implements Map<String, List<String>> {
    public static final CaseInsensitiveMultiMap EMPTY = new CaseInsensitiveMultiMap(Collections.emptyMap());

    private Map<String, List<String>> rawHeaders;
    private Map<String, List<String>> caseInsensitiveHeaders;

    public CaseInsensitiveMultiMap(Map<String, List<String>> src) {
        this.rawHeaders = Collections.unmodifiableMap(src);

        this.caseInsensitiveHeaders = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : this.rawHeaders.entrySet()) {
            this.caseInsensitiveHeaders.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        this.caseInsensitiveHeaders = Collections.unmodifiableMap(this.caseInsensitiveHeaders);
    }

    /* ---------------- */
    /* Case Insensitive */
    /* ---------------- */

    public String getSingle(String key) {
        List<String> values = this.get(key);
        if (values == null) {
            return null;
        } else {
            return values.get(0);
        }
    }

    public String getSingleOrDefault(String key, String defaultValue) {
        List<String> values = this.get(key);
        if (values == null) {
            return defaultValue;
        } else {
            return values.get(0);
        }
    }

    @Override
    public List<String> get(Object key) {
        return this.caseInsensitiveHeaders.get(String.valueOf(key).toLowerCase());
    }

    @Override
    public boolean containsKey(Object key) {
        return this.caseInsensitiveHeaders.containsKey(String.valueOf(key).toLowerCase());
    }

    /* ---------------- */
    /* Case Sensitive   */
    /* ---------------- */

    @Override
    public boolean containsValue(Object value) {
        return this.rawHeaders.containsValue(value);
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return this.rawHeaders.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return this.rawHeaders.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return this.rawHeaders.keySet();
    }

    @Override
    public int size() {
        return this.rawHeaders.size();
    }

    @Override
    public Collection<List<String>> values() {
        return this.rawHeaders.values();
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
        return this.rawHeaders.toString();
    }

    /* ---------------- */
    /* Builder          */
    /* ---------------- */

    public static class Builder {
        private Map<String, List<String>> headers = new HashMap<>();

        public Builder put(String key, String value) {
            this.getValueList(key).add(value);
            return this;
        }

        public Builder putAll(String key, String... values) {
            this.getValueList(key).addAll(Arrays.asList(values));
            return this;
        }

        public Builder putAll(String key, List<String> values) {
            this.getValueList(key).addAll(values);
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

        public CaseInsensitiveMultiMap build() {
            for (Entry<String, List<String>> entry : this.headers.entrySet()) {
                entry.setValue(
                    Collections.unmodifiableList(
                        new ArrayList<>(entry.getValue()) // We convert to ArrayList for faster access.
                    )
                );
            }
            return new CaseInsensitiveMultiMap(this.headers);
        }

    }

}
