package co.casterlabs.rhs.protocol.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HeaderValue {
    public static final HeaderValue EMPTY = new HeaderValue("");

    private final String raw;

    public String raw() {
        return this.raw;
    }

    @Override
    public int hashCode() {
        return this.raw.hashCode();
    }

    @Override
    public String toString() {
        return this.raw;
    }

    /* ------------ */
    /* Directives   */
    /* ------------ */

    private String cached_withoutDirectives = null;
    private CaseInsensitiveMultiMap<String> cached_directives = null;

    private void cacheDirectives() {
        if (this.cached_withoutDirectives != null) return;

        int directiveIndex = this.raw.indexOf(';');
        if (directiveIndex == -1) {
            this.cached_withoutDirectives = this.raw;
            this.cached_directives = CaseInsensitiveMultiMap.emptyMap();
            return;
        }

        CaseInsensitiveMultiMap.Builder<String> directives = new CaseInsensitiveMultiMap.Builder<>();
        for (String directive : this.raw.substring(directiveIndex + 1).trim().split(";")) {
            String[] split = directive.trim().split("=");
            if (split.length == 1) {
                directives.put(split[0], "");
            } else {
                directives.put(split[0], split[1]);
            }
        }

        this.cached_withoutDirectives = this.raw.substring(0, directiveIndex);
        this.cached_directives = directives.build();
    }

    public String withoutDirectives() {
        this.cacheDirectives();
        return this.cached_withoutDirectives;
    }

    public CaseInsensitiveMultiMap<String> directives() {
        this.cacheDirectives();
        return this.cached_directives;
    }

    /* ------------ */
    /* Delimiters   */
    /* ------------ */

    private Map<String, List<HeaderValue>> cached_delimits = new HashMap<>();

    public List<HeaderValue> delimited(String delimiter) {
        List<HeaderValue> result = this.cached_delimits.get(delimiter);
        if (result == null) {
            String[] split = this.raw.split(delimiter);
            if (split.length == 1) {
                result = Arrays.asList(this);
            } else {
                result = new ArrayList<>(split.length);
                for (String value : split) {
                    result.add(new HeaderValue(value.trim()));
                }
            }

            this.cached_delimits.put(delimiter, result);
        }
        return result;
    }

}
