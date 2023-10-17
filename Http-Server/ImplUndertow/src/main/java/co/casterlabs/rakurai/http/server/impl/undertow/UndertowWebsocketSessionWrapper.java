package co.casterlabs.rakurai.http.server.impl.undertow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.casterlabs.rakurai.collections.HeaderMap;
import co.casterlabs.rakurai.io.http.HttpVersion;
import co.casterlabs.rakurai.io.http.server.config.HttpServerBuilder;
import co.casterlabs.rakurai.io.http.server.websocket.WebsocketSession;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class UndertowWebsocketSessionWrapper extends WebsocketSession {
    private WebSocketHttpExchange exchange;
    private WebSocketChannel channel;
    private int port;

    private Map<String, List<String>> allQueryParameters = new HashMap<>();
    private Map<String, String> queryParameters = new HashMap<>();
    private HeaderMap headers;

    public UndertowWebsocketSessionWrapper(WebSocketHttpExchange exchange, WebSocketChannel channel, int port, HttpServerBuilder config, FastLogger parentLogger) {
        this.exchange = exchange;
        this.channel = channel;
        this.port = port;

        for (Map.Entry<String, List<String>> entry : exchange.getRequestParameters().entrySet()) {
            this.allQueryParameters.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            this.queryParameters.put(entry.getKey(), entry.getValue().get(0));
        }

        this.headers = new HeaderMap.Builder().putMap(exchange.getRequestHeaders()).build();

        super.postConstruct(config, parentLogger);
    }

    // Request headers
    @Override
    public HeaderMap getHeaders() {
        return this.headers;
    }

    // URI
    @Override
    public String getUri() {
        return this.exchange.getRequestURI().split("\\?")[0];
    }

    @Override
    public Map<String, List<String>> getAllQueryParameters() {
        return this.allQueryParameters;
    }

    @Override
    public Map<String, String> getQueryParameters() {
        return this.queryParameters;
    }

    @Override
    public String getQueryString() {
        if (this.exchange.getQueryString() == null) {
            return "";
        } else {
            return "?" + this.exchange.getQueryString();
        }
    }

    // Server Info
    @Override
    public int getPort() {
        return this.port;
    }

    // Misc
    @Override
    public HttpVersion getVersion() {
        return HttpVersion.HTTP_2_0; // ?
    }

    @Override
    protected String getNetworkIpAddress() {
        return this.channel.getSourceAddress().getHostString();
    }

}
