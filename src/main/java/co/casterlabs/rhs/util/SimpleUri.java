package co.casterlabs.rhs.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleUri {
    public final String host;
    public final String path;
    public final @Nullable String query;

    private CaseInsensitiveMultiMap queryParameters;

    public static SimpleUri from(String host, String pathAndQuery) {
        int idx = pathAndQuery.indexOf('?');
        if (idx == -1) {
            return new SimpleUri(host, pathAndQuery, null);
        } else {
            return new SimpleUri(host, pathAndQuery.substring(0, idx), pathAndQuery.substring(idx + 1));
        }
    }

    public CaseInsensitiveMultiMap queryParameters() {
        this.cacheQueryParameters();
        return this.queryParameters;
    }

    public @Nullable List<String> getQueryParameter(@NonNull String key) {
        this.cacheQueryParameters();
        return this.queryParameters.get(key);
    }

    public @Nullable String getSingleQueryParameter(@NonNull String key) {
        List<String> values = this.getQueryParameter(key);
        if (values == null) {
            return null;
        }
        return values.get(0);
    }

    public String getSingleQueryParameter(@NonNull String key, @NonNull String defaultValue) {
        List<String> values = this.getQueryParameter(key);
        if (values == null) {
            return defaultValue;
        }
        return values.get(0);
    }

    public void cacheQueryParameters() {
        if (this.queryParameters != null) {
            return;
        }
        if (this.query == null) {
            this.queryParameters = CaseInsensitiveMultiMap.EMPTY;
            return;
        }

        this.queryParameters = new CaseInsensitiveMultiMap(
            Arrays // Magic.
                .stream(this.query.substring(1).split("&"))
                .map((it) -> {
                    try {
                        int eqIdx = it.indexOf("=");

                        if (eqIdx == -1) {
                            return new SimpleImmutableEntry<>(
                                URLDecoder.decode(it, "UTF-8"),
                                ""
                            );
                        }

                        String key = it.substring(0, eqIdx);
                        String value = it.substring(eqIdx + 1);

                        return new SimpleImmutableEntry<>(
                            URLDecoder.decode(key, "UTF-8"),
                            URLDecoder.decode(value, "UTF-8")
                        );
                    } catch (UnsupportedEncodingException ignored) {
                        return null;
                    }
                })
                .filter((e) -> e != null)
                .collect(
                    Collectors.groupingBy(
                        SimpleImmutableEntry::getKey,
                        HashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                    )
                )
        );
    }

}
