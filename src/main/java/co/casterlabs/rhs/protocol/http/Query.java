package co.casterlabs.rhs.protocol.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import lombok.NonNull;

public class Query extends CaseInsensitiveMultiMap<String> {
    public static final Query EMPTY = new Query(Collections.emptyMap(), "");

    public final String raw;

    private Query(Map<String, List<String>> src, String raw) {
        super(src);
        this.raw = raw;
    }

    public static Query from(@NonNull String src) {
        return new Query(
            Arrays // Magic.
                .stream(src.split("&"))
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
                ),
            src
        );
    }

}
