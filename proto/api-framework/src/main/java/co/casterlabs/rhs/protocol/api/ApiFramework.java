package co.casterlabs.rhs.protocol.api;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import co.casterlabs.rhs.protocol.api._EndpointWrapper._HttpEndpointWrapper;
import co.casterlabs.rhs.protocol.api._EndpointWrapper._WebsocketEndpointWrapper;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.api.endpoints.WebsocketEndpoint;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol.WebsocketHandler;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;

public class ApiFramework {
    private List<HttpProtoHandler> httpRoutes = new ArrayList<>();
    private List<WebsocketHandler> websocketRoutes = new ArrayList<>();

    public final HttpProtoHandler httpHandler = (session) -> {
        for (HttpProtoHandler handler : this.httpRoutes) {
            HttpResponse response = handler.handle(session);
            if (response != null) {
                return response;
            }
        }
        return null;
    };

    public final WebsocketHandler websocketHandler = (session) -> {
        for (WebsocketHandler handler : this.websocketRoutes) {
            WebsocketResponse response = handler.handle(session);
            if (response != null) {
                return response;
            }
        }
        return null;
    };

    public void register(EndpointProvider provider) {
        for (Method method : provider.getClass().getMethods()) {
            if (method.isAnnotationPresent(HttpEndpoint.class)) {
                if (method.getParameterCount() != 2 ||
                    !method.getParameters()[0].getType().isAssignableFrom(HttpSession.class) ||
                    !method.getParameters()[1].getType().isAssignableFrom(EndpointData.class) ||
                    !method.getReturnType().isAssignableFrom(HttpResponse.class)) {
                    throw new IllegalArgumentException("Method signature is invalid. Must be (HttpSession, EndpointData) -> HttpResponse");
                }

                HttpEndpoint annotation = method.getAnnotation(HttpEndpoint.class);
                this.httpRoutes.add(
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
                this.websocketRoutes.add(
                    new _WebsocketEndpointWrapper(method, provider, annotation)
                );
            }
        }
    }

    static <T extends Enum<?>> boolean arrayContains(T value, T[] arr) {
        for (T v : arr) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

}
