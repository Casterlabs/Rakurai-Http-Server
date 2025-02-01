package co.casterlabs.rhs.protocol.api;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.api.endpoints.WebsocketEndpoint;
import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol.WebsocketHandler;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;
import lombok.SneakyThrows;

abstract class _EndpointWrapper<R, S, A> {
    private final Method method;
    private final Object instance;

    protected final A annotation;
    protected final Pattern pattern;

    private String[] paramLabels; // Entry will be null if not a param
    private boolean hasParams;

    protected _EndpointWrapper(Method method, Object instance, String path, A annotation) {
        this.method = method;
        this.instance = instance;
        this.annotation = annotation;

        String[] pathParts = path.split("/");
        this.paramLabels = new String[pathParts.length];

        this.pattern = Pattern.compile(
            Arrays.stream(pathParts)
                .map((p) -> p.startsWith(":") ? "[^/]*" : p)
                .collect(Collectors.joining("/"))
        );

        for (int i = 0; i < pathParts.length; i++) {
            String part = pathParts[i];
            if (part.startsWith(":")) {
                this.hasParams = true;
                this.paramLabels[i] = part.substring(1);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected @Nullable R handle(S session, String rawPath) {
        EndpointData data;
        if (this.hasParams) {
            String[] pathParts = rawPath.split("/");
            Map<String, String> params = new HashMap<>();
            for (int i = 0; i < Math.min(pathParts.length, this.paramLabels.length); i++) {
                if (this.paramLabels[i] == null) continue;
                String part = pathParts[i];
                params.put(this.paramLabels[i], part);
            }
            data = new EndpointData(params);
        } else {
            data = new EndpointData(Collections.emptyMap());
        }

        return (R) this.method.invoke(this.instance, session, data);
    }

    static class _HttpEndpointWrapper extends _EndpointWrapper<HttpResponse, HttpSession, HttpEndpoint> implements HttpProtoHandler {

        public _HttpEndpointWrapper(Method method, Object instance, HttpEndpoint annotation) {
            super(method, instance, annotation.path(), annotation);
        }

        @Override
        public HttpResponse handle(HttpSession session) throws HttpException, DropConnectionException {
            if (!ApiFramework.arrayContains(session.method(), this.annotation.allowedMethods())) {
                return null;
            }

            if (!this.pattern.matcher(session.uri().rawPath).matches()) {
                return null;
            }

            return this.handle(session, session.uri().rawPath);
        }

    }

    static class _WebsocketEndpointWrapper extends _EndpointWrapper<WebsocketResponse, WebsocketSession, WebsocketEndpoint> implements WebsocketHandler {

        public _WebsocketEndpointWrapper(Method method, Object instance, WebsocketEndpoint annotation) {
            super(method, instance, annotation.path(), annotation);
        }

        @Override
        public WebsocketResponse handle(WebsocketSession session) {
            if (!this.pattern.matcher(session.uri().rawPath).matches()) {
                return null;
            }

            return this.handle(session, session.uri().rawPath);
        }

    }

}
