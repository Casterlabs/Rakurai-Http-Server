package co.casterlabs.rhs.protocol.api;

import java.lang.reflect.Method;
import java.net.URLDecoder;
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

abstract class _EndpointWrapper<RESPONSE, SESSION, ATTACHMENT> {
    private final Method method;
    private final Object instance;
    private final String path;

    protected final Pattern pattern;

    private String[] paramLabels; // Entry will be null if not a param
    private boolean hasParams;

    @SneakyThrows
    protected _EndpointWrapper(@NonNull Method method, @NonNull Object instance, @NonNull String path) {
        this.method = method;
        this.instance = instance;
        this.path = path;

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

    protected abstract @Nullable Class<? extends Preprocessor<RESPONSE, SESSION, ?>> preprocessor();

    protected abstract @Nullable Class<? extends Postprocessor<RESPONSE, SESSION, ?>> postprocessor();

    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected @Nullable RESPONSE handle(ApiFramework fw, SESSION session, String path) {
        Map<String, String> uriParameters = new HashMap<>();

        if (this.hasParams) {
            String[] pathParts = path.split("/");
            Map<String, String> params = new HashMap<>();
            int minLength = Math.min(pathParts.length, this.paramLabels.length);
            for (int i = 0; i < minLength; i++) {
                if (this.paramLabels[i] == null) continue;
                String part = URLDecoder.decode(pathParts[i], "UTF-8");
                params.put(this.paramLabels[i], part);
            }
            uriParameters = Collections.unmodifiableMap(params);
        } else {
            uriParameters = Collections.emptyMap();
        }

        // We run the preprocessor first, and if it returns a response, we skip the
        // handler method. This allows the post processor to run regardless of whether
        // the preprocessor or the handler method returned a response.
        RESPONSE response = null;

        Preprocessor<RESPONSE, SESSION, ATTACHMENT> preprocessor = fw.getOrInstantiatePreprocessor((Class<? extends Preprocessor<RESPONSE, SESSION, ATTACHMENT>>) this.preprocessor());
        ATTACHMENT preprocessorAttachment = null;
        if (preprocessor != null) {
            PreprocessorContext<RESPONSE, ATTACHMENT> context = new PreprocessorContext<>(uriParameters, this.path);

            preprocessor.preprocess(session, context);

            response = context.respondEarly(); // Either null or a response.
            preprocessorAttachment = context.attachment();
        }

        EndpointData<ATTACHMENT> data = new EndpointData<>(uriParameters, this.path, preprocessorAttachment);
        if (response == null) {
            response = (RESPONSE) this.method.invoke(this.instance, session, data);
        }

        Postprocessor<RESPONSE, SESSION, ATTACHMENT> postprocessor = fw.getOrInstantiatePostprocessor((Class<? extends Postprocessor<RESPONSE, SESSION, ATTACHMENT>>) this.postprocessor());
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
