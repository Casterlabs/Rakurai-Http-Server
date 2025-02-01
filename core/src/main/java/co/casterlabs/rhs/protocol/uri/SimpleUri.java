package co.casterlabs.rhs.protocol.uri;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleUri {
    public final String host;
    public final String path;
    public final Query query;

    public final String rawPath;

    public static SimpleUri from(String host, String pathAndQuery) {
        int idx = pathAndQuery.indexOf('?');
        if (idx == -1) {
            return new SimpleUri(
                host,
                pathAndQuery,
                Query.EMPTY,
                pathAndQuery
            );
        } else {
            return new SimpleUri(
                host,
                pathAndQuery.substring(0, idx),
                Query.from(pathAndQuery.substring(idx + 1)),
                pathAndQuery
            );
        }
    }

    @Override
    public String toString() {
        return this.host + this.rawPath;
    }

}
