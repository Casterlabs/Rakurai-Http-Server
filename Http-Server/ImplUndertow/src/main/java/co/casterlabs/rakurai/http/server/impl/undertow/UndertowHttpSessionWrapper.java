package co.casterlabs.rakurai.http.server.impl.undertow;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.collections.HeaderMap;
import co.casterlabs.rakurai.io.http.HttpVersion;
import co.casterlabs.rakurai.io.http.server.HttpSession;
import co.casterlabs.rakurai.io.http.server.config.HttpServerBuilder;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class UndertowHttpSessionWrapper extends HttpSession {
    private HttpServerExchange exchange;
    private int port;

    private Map<String, List<String>> allQueryParameters = new HashMap<>();
    private Map<String, String> queryParameters = new HashMap<>();
    private HeaderMap headers;

    @SuppressWarnings("deprecation")
    public UndertowHttpSessionWrapper(HttpServerExchange exchange, int port, HttpServerBuilder config, FastLogger parentLogger) {
        this.exchange = exchange;
        this.port = port;

        io.undertow.util.HeaderMap headerMap = exchange.getRequestHeaders();

        HeaderMap.Builder builder = new HeaderMap.Builder();

        long headersIndex = headerMap.fastIterate();
        while (headersIndex != -1) {
            HeaderValues header = headerMap.fiCurrent(headersIndex);
            HttpString headerName = header.getHeaderName();

            builder.putAll(headerName.toString(), header);

            headersIndex = headerMap.fiNext(headersIndex);
        }

        this.headers = builder.build();

        for (Map.Entry<String, Deque<String>> entry : exchange.getQueryParameters().entrySet()) {
            List<String> values = new LinkedList<>();

            for (String queryValue : entry.getValue()) {
                values.add(URLDecoder.decode(queryValue));
            }

            this.allQueryParameters.put(entry.getKey(), values);
            this.queryParameters.put(entry.getKey(), values.get(0));
        }

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
        return this.exchange.getRequestPath();
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
        if (this.exchange.getQueryString().isEmpty()) {
            return "";
        } else {
            return "?" + this.exchange.getQueryString();
        }
    }

    // Request body
    @Override
    public boolean hasBody() {
        try {
            return this.getRequestBodyStream().available() != -1;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public @Nullable InputStream getRequestBodyStream() throws IOException {
        return this.exchange.getInputStream();
    }

    @Override
    public Map<String, String> parseFormBody() throws IOException {
        FormDataParser formDataParser = FormParserFactory.builder().build().createParser(this.exchange);

        if (formDataParser == null) {
            throw new IOException("No form data to parse.");
        } else {
            Map<String, String> files = new HashMap<>();

            this.exchange.startBlocking();

            FormData formData = formDataParser.parseBlocking();

            for (String data : formData) {
                for (FormData.FormValue formValue : formData.get(data)) {
                    files.put(data, formValue.getValue());
                    break;
                }
            }

            return files;
        }
    }

    // Server Info
    @Override
    public int getPort() {
        return this.port;
    }

    // Misc
    @Override
    public String getRawMethod() {
        return this.exchange.getRequestMethod().toString();
    }

    @Override
    public HttpVersion getVersion() {
        return HttpVersion.valueOf(this.exchange.getProtocol().toString().replace('/', '_').replace('.', '_'));
    }

    @Override
    protected String getNetworkIpAddress() {
        return this.exchange.getSourceAddress().getHostString();
    }

}
