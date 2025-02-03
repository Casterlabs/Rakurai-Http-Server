package co.casterlabs.rhs.protocol.api;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.api.endpoints.WebsocketEndpoint;
import co.casterlabs.rhs.protocol.api.postprocessors.Postprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor.PreprocessorContext;
import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;
import lombok.NonNull;
import lombok.SneakyThrows;

abstract class _EndpointWrapper<R, S, A> {
    private final Method method;
    private final Object instance;

    protected final Pattern pattern;

    private String[] paramLabels; // Entry will be null if not a param
    private boolean hasParams;

    @SneakyThrows
    protected _EndpointWrapper(@NonNull Method method, @NonNull Object instance, @NonNull String path) {
        this.method = method;
        this.instance = instance;

        this.pattern = Pattern.compile(
            path.replaceAll(":[A-Za-z0-9-_]+", "[^/]*")
        );

        String[] pathParts = path.split("/");
        this.paramLabels = new String[pathParts.length];
        for (int i = 0; i < pathParts.length; i++) {
            String part = pathParts[i];
            if (part.startsWith(":")) {
                this.hasParams = true;
                this.paramLabels[i] = part.substring(1);
            }
        }
    }

    public abstract int priority();

    protected abstract @Nullable Class<? extends Preprocessor<R, S, ?>> preprocessor();

    protected abstract @Nullable Class<? extends Postprocessor<R, S, ?>> postprocessor();

    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected @Nullable R handle(ApiFramework fw, S session, String path) {
        Map<String, String> uriParameters = new HashMap<>();

        if (this.hasParams) {
            String[] pathParts = path.split("/");
            Map<String, String> params = new HashMap<>();
            int minLength = Math.min(pathParts.length, this.paramLabels.length);
            for (int i = 0; i < minLength; i++) {
                if (this.paramLabels[i] == null) continue;
                String part = pathParts[i];
                params.put(this.paramLabels[i], part);
            }
            uriParameters = Collections.unmodifiableMap(params);
        } else {
            uriParameters = Collections.emptyMap();
        }

        Preprocessor<R, S, A> preprocessor = fw.getOrInstantiatePreprocessor((Class<? extends Preprocessor<R, S, A>>) this.preprocessor());
        A preprocessorAttachment = null;
        if (preprocessor != null) {
            PreprocessorContext<R, A> context = new PreprocessorContext<>(uriParameters);

            preprocessor.preprocess(session, context);

            if (context.respondEarly() != null) {
                return context.respondEarly();
            }

            preprocessorAttachment = context.attachment();
        }

        EndpointData<A> data = new EndpointData<>(uriParameters, preprocessorAttachment);
        R response = (R) this.method.invoke(this.instance, session, data);

        Postprocessor<R, S, A> postprocessor = fw.getOrInstantiatePostprocessor((Class<? extends Postprocessor<R, S, A>>) this.postprocessor());
        if (response != null && postprocessor != null) {
            postprocessor.postprocess(session, response, data);
        }

        return response;
    }

    static class _HttpEndpointWrapper extends _EndpointWrapper<HttpResponse, HttpSession, Object> {
        private HttpEndpoint annotation;

        public _HttpEndpointWrapper(@NonNull Method method, @NonNull Object instance, @NonNull HttpEndpoint annotation) {
            super(
                method,
                instance,
                annotation.path()
            );
            this.annotation = annotation;
        }

        @Override
        public int priority() {
            return this.annotation.priority();
        }

        @Override
        protected @Nullable Class<? extends Preprocessor<HttpResponse, HttpSession, ?>> preprocessor() {
            return this.annotation.preprocessor();
        }

        @Override
        protected @Nullable Class<? extends Postprocessor<HttpResponse, HttpSession, ?>> postprocessor() {
            return this.annotation.postprocessor();
        }

        public HttpResponse handle(ApiFramework fw, HttpSession session) throws HttpException, DropConnectionException {
            if (!arrayContains(session.method(), this.annotation.allowedMethods())) {
                return null;
            }

            if (!this.pattern.matcher(session.uri().path).matches()) {
                return null;
            }

            return this.handle(fw, session, session.uri().path);
        }

    }

    static class _WebsocketEndpointWrapper extends _EndpointWrapper<WebsocketResponse, WebsocketSession, Object> {
        private WebsocketEndpoint annotation;

        public _WebsocketEndpointWrapper(@NonNull Method method, @NonNull Object instance, @NonNull WebsocketEndpoint annotation) {
            super(
                method,
                instance,
                annotation.path()
            );
            this.annotation = annotation;
        }

        @Override
        public int priority() {
            return this.annotation.priority();
        }

        @Override
        protected @Nullable Class<? extends Preprocessor<WebsocketResponse, WebsocketSession, ?>> preprocessor() {
            return this.annotation.preprocessor();
        }

        @Override
        protected @Nullable Class<? extends Postprocessor<WebsocketResponse, WebsocketSession, ?>> postprocessor() {
            return null; // Doesn't exist.
        }

        public WebsocketResponse handle(ApiFramework fw, WebsocketSession session) {
            if (!this.pattern.matcher(session.uri().path).matches()) {
                return null;
            }

            return this.handle(fw, session, session.uri().path);
        }

    }

    private static <T extends Enum<?>> boolean arrayContains(T value, T[] arr) {
        for (T v : arr) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

}
