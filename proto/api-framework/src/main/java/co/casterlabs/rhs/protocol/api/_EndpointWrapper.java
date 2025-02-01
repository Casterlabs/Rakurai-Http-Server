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
import co.casterlabs.rhs.protocol.api.postprocessors.NoOpPostprocessor;
import co.casterlabs.rhs.protocol.api.postprocessors.Postprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.NoOpPreprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor.PreprocessorContext;
import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol.WebsocketHandler;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;
import lombok.SneakyThrows;

abstract class _EndpointWrapper<R, S, N, A> {
    private final Method method;
    private final Object instance;

    protected final N annotation;
    protected final Pattern pattern;

    private String[] paramLabels; // Entry will be null if not a param
    private boolean hasParams;

    private Preprocessor<R, S> preprocessor;
    private Postprocessor<R, S, A> postprocessor;

    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected _EndpointWrapper(
        Method method,
        Object instance,
        String path,
        Class<? extends Preprocessor<R, S>> preprocessorClazz,
        Class<? extends Postprocessor<R, S, ?>> postprocessorClazz,
        N annotation
    ) {
        this.method = method;
        this.instance = instance;
        this.annotation = annotation;

        if (preprocessorClazz != null &&
            !NoOpPreprocessor.Http.class.isAssignableFrom(preprocessorClazz) &&
            !NoOpPreprocessor.Websocket.class.isAssignableFrom(preprocessorClazz)) {
            this.preprocessor = preprocessorClazz.getDeclaredConstructor().newInstance();
        }

        if (postprocessorClazz != null &&
            !NoOpPostprocessor.class.isAssignableFrom(postprocessorClazz)) {
            this.postprocessor = (Postprocessor<R, S, A>) postprocessorClazz.getDeclaredConstructor().newInstance();
        }

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
    protected @Nullable R handle(S session, String path) {
        Map<String, String> uriParameters = new HashMap<>();

        if (this.hasParams) {
            String[] pathParts = path.split("/");
            Map<String, String> params = new HashMap<>();
            for (int i = 0; i < Math.min(pathParts.length, this.paramLabels.length); i++) {
                if (this.paramLabels[i] == null) continue;
                String part = pathParts[i];
                params.put(this.paramLabels[i], part);
            }
            uriParameters = Collections.unmodifiableMap(params);
        } else {
            uriParameters = Collections.emptyMap();
        }

        A preprocessorAttachment = null;
        if (this.preprocessor != null) {
            PreprocessorContext<R> context = new PreprocessorContext<>(uriParameters);

            this.preprocessor.preprocess(session, context);

            if (context.respondEarly() != null) {
                return context.respondEarly();
            }

            preprocessorAttachment = (A) context.attachment();
        }

        EndpointData<A> data = new EndpointData<>(uriParameters, preprocessorAttachment);
        R response = (R) this.method.invoke(this.instance, session, data);

        if (response != null && this.postprocessor != null) {
            this.postprocessor.postprocess(session, response, data);
        }

        return response;
    }

    static class _HttpEndpointWrapper extends _EndpointWrapper<HttpResponse, HttpSession, HttpEndpoint, Object> implements HttpProtoHandler {

        public _HttpEndpointWrapper(Method method, Object instance, HttpEndpoint annotation) {
            super(
                method,
                instance,
                annotation.path(),
                annotation.preprocessor(),
                annotation.postprocessor(),
                annotation
            );
        }

        @Override
        public HttpResponse handle(HttpSession session) throws HttpException, DropConnectionException {
            if (!ApiFramework.arrayContains(session.method(), this.annotation.allowedMethods())) {
                return null;
            }

            if (!this.pattern.matcher(session.uri().path).matches()) {
                return null;
            }

            return this.handle(session, session.uri().path);
        }

    }

    static class _WebsocketEndpointWrapper extends _EndpointWrapper<WebsocketResponse, WebsocketSession, WebsocketEndpoint, Object> implements WebsocketHandler {

        public _WebsocketEndpointWrapper(Method method, Object instance, WebsocketEndpoint annotation) {
            super(
                method,
                instance,
                annotation.path(),
                annotation.preprocessor(),
                null,
                annotation
            );
        }

        @Override
        public WebsocketResponse handle(WebsocketSession session) {
            if (!this.pattern.matcher(session.uri().path).matches()) {
                return null;
            }

            return this.handle(session, session.uri().path);
        }

    }

}
