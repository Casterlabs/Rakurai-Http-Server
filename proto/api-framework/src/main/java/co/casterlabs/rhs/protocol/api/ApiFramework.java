package co.casterlabs.rhs.protocol.api;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.api._EndpointWrapper._HttpEndpointWrapper;
import co.casterlabs.rhs.protocol.api._EndpointWrapper._WebsocketEndpointWrapper;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.api.endpoints.WebsocketEndpoint;
import co.casterlabs.rhs.protocol.api.postprocessors.NoOpPostprocessor;
import co.casterlabs.rhs.protocol.api.postprocessors.Postprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.NoOpPreprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol.WebsocketHandler;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;
import lombok.NonNull;
import lombok.SneakyThrows;

public class ApiFramework {
    /* ---------------- */
    /* Endpoints        */
    /* ---------------- */
    private List<_HttpEndpointWrapper> httpEndpoints = new ArrayList<>();
    private List<_WebsocketEndpointWrapper> websocketEndpoints = new ArrayList<>();

    public void register(@NonNull EndpointProvider provider) {
        for (Method method : provider.getClass().getMethods()) {
            if (method.isAnnotationPresent(HttpEndpoint.class)) {
                if (method.getParameterCount() != 2 ||
                    !method.getParameters()[0].getType().isAssignableFrom(HttpSession.class) ||
                    !method.getParameters()[1].getType().isAssignableFrom(EndpointData.class) ||
                    !method.getReturnType().isAssignableFrom(HttpResponse.class)) {
                    throw new IllegalArgumentException("Method signature is invalid. Must be (HttpSession, EndpointData) -> HttpResponse");
                }

                HttpEndpoint annotation = method.getAnnotation(HttpEndpoint.class);
                this.httpEndpoints.add(
                    new _HttpEndpointWrapper(method, provider, annotation)
                );
            }

            if (method.isAnnotationPresent(WebsocketEndpoint.class)) {
                if (method.getParameterCount() != 2 ||
                    !method.getParameters()[0].getType().isAssignableFrom(WebsocketSession.class) ||
                    !method.getParameters()[1].getType().isAssignableFrom(EndpointData.class) ||
                    !method.getReturnType().isAssignableFrom(WebsocketResponse.class)) {
                    throw new IllegalArgumentException("Method signature is invalid. Must be (WebsocketSession, EndpointData) -> WebsocketResponse");
                }

                WebsocketEndpoint annotation = method.getAnnotation(WebsocketEndpoint.class);
                this.websocketEndpoints.add(
                    new _WebsocketEndpointWrapper(method, provider, annotation)
                );
            }
        }

        // Sort by priority. Higher value means it should be at the head of the list.
        this.httpEndpoints.sort((e1, e2) -> -Integer.compare(e1.priority(), e2.priority()));
        this.websocketEndpoints.sort((e1, e2) -> -Integer.compare(e1.priority(), e2.priority()));
    }

    /* ---------------- */
    /* Handlers         */
    /* ---------------- */

    public final HttpProtoHandler httpHandler = (session) -> {
        for (_HttpEndpointWrapper handler : this.httpEndpoints) {
            HttpResponse response = handler.handle(this, session);
            if (response != null) {
                return response;
            }
        }
        return null;
    };

    public final WebsocketHandler websocketHandler = (session) -> {
        for (_WebsocketEndpointWrapper handler : this.websocketEndpoints) {
            WebsocketResponse response = handler.handle(this, session);
            if (response != null) {
                return response;
            }
        }
        return null;
    };

    /* ---------------- */
    /* Processing       */
    /* ---------------- */

    private Map<Class<? extends Preprocessor<?, ?, ?>>, Preprocessor<?, ?, ?>> preprocessorInstances = new HashMap<>();
    private Map<Class<? extends Postprocessor<?, ?, ?>>, Postprocessor<?, ?, ?>> postprocessorInstances = new HashMap<>();

    /**
     * Allows you to instantiate a preprocessor. Otherwise, it is instantiated by
     * calling the default no-args constructor which may not be desirable for all
     * use cases.
     * 
     * @implNote You can replace the instance on-the-fly by calling this method
     *           again.
     */
    public <T extends Preprocessor<?, ?, ?>> void instantiatePreprocessor(@NonNull Class<T> clazz, @NonNull T instance) {
        if (!clazz.isAssignableFrom(instance.getClass())) {
            throw new IllegalArgumentException("clazz must be assignable from instance.");
        }
        this.preprocessorInstances.put(clazz, instance);
    }

    /**
     * Allows you to instantiate a postprocessor. Otherwise, it is instantiated by
     * calling the default no-args constructor which may not be desirable for all
     * use cases.
     * 
     * @implNote You can replace the instance on-the-fly by calling this method
     *           again.
     */
    public <T extends Postprocessor<?, ?, ?>> void instantiatePostprocessor(@NonNull Class<T> clazz, @NonNull T instance) {
        if (!clazz.isAssignableFrom(instance.getClass())) {
            throw new IllegalArgumentException("clazz must be assignable from instance.");
        }
        this.postprocessorInstances.put(clazz, instance);
    }

    @SneakyThrows
    <R, S, A> @Nullable Preprocessor<R, S, A> getOrInstantiatePreprocessor(@Nullable Class<? extends Preprocessor<R, S, A>> clazz) {
        if (clazz == null ||
            NoOpPreprocessor.Http.class.isAssignableFrom(clazz) ||
            NoOpPreprocessor.Websocket.class.isAssignableFrom(clazz)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Preprocessor<R, S, A> p = (Preprocessor<R, S, A>) this.preprocessorInstances.get(clazz);
        if (p == null) {
            p = clazz.getDeclaredConstructor().newInstance();
            this.preprocessorInstances.put(clazz, p);
        }
        return p;
    }

    @SneakyThrows
    <R, S, A> @Nullable Postprocessor<R, S, A> getOrInstantiatePostprocessor(@Nullable Class<? extends Postprocessor<R, S, A>> clazz) {
        if (clazz == null ||
            NoOpPostprocessor.class.isAssignableFrom(clazz)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Postprocessor<R, S, A> p = (Postprocessor<R, S, A>) this.postprocessorInstances.get(clazz);
        if (p == null) {
            p = clazz.getDeclaredConstructor().newInstance();
            this.postprocessorInstances.put(clazz, p);
        }
        return p;
    }

}
