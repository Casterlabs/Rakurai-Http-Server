package co.casterlabs.rakurai.http.server.impl.nano;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.casterlabs.rakurai.collections.HeaderMap;
import co.casterlabs.rakurai.io.http.HttpVersion;
import co.casterlabs.rakurai.io.http.server.HttpSession;
import co.casterlabs.rakurai.io.http.server.config.HttpServerBuilder;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.ResponseException;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class NanoHttpSession extends HttpSession {
    private @Getter IHTTPSession nanoSession;
    private int port;

    private HeaderMap headers;

    public NanoHttpSession(IHTTPSession nanoSession, int port, HttpServerBuilder config, FastLogger parentLogger) {
        this.port = port;
        this.nanoSession = nanoSession;
        this.headers = new HeaderMap.Builder().putSingleMap(this.nanoSession.getHeaders()).build();

        this.postConstruct(config, parentLogger);
    }

    // Request headers
    @Override
    public HeaderMap getHeaders() {
        return this.headers;
    }

    // URI
    @Override
    public String getUri() {
        return this.nanoSession.getUri();
    }

    @Override
    public Map<String, List<String>> getAllQueryParameters() {
        return this.nanoSession.getParameters();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<String, String> getQueryParameters() {
        return this.nanoSession.getParms();
    }

    @Override
    public String getQueryString() {
        if (this.nanoSession.getQueryParameterString() == null) {
            return "";
        } else {
            return "?" + this.nanoSession.getQueryParameterString();
        }
    }

    // Request body
    @Override
    public boolean hasBody() {
        return this.getHeader("content-length") != null;
    }

    @Override
    public InputStream getRequestBodyStream() throws IOException {
        return this.nanoSession.getInputStream();
    }

    @Override
    public Map<String, String> parseFormBody() throws IOException {
        try {
            Map<String, String> files = new HashMap<>();
            this.nanoSession.parseBody(files);
            return files;
        } catch (ResponseException e) {
            throw new IOException(e);
        }
    }

    // Server info
    @Override
    public int getPort() {
        return this.port;
    }

    // Misc
    @Override
    public String getRawMethod() {
        return this.nanoSession.getMethod().name();
    }

    @Override
    public HttpVersion getVersion() {
        return HttpVersion.HTTP_1_1;
    }

    @Override
    protected String getNetworkIpAddress() {
        return this.nanoSession.getRemoteIpAddress();
    }

}
