package co.casterlabs.rhs.session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public abstract class WebsocketSession extends HttpSession {

    /**
     * @deprecated Websockets do not support this.
     */
    @Deprecated
    @Override
    public boolean hasBody() {
        return false;
    }

    /**
     * @deprecated Websockets do not support this.
     */
    @Deprecated
    @Override
    public @Nullable byte[] getRequestBodyBytes() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Websockets do not support this.
     */
    @Deprecated
    @Override
    public InputStream getRequestBodyStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Websockets do not support this.
     */
    @Deprecated
    @Override
    public Map<String, String> parseFormBody() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Websockets do not support this.
     */
    @Deprecated
    @Override
    public String getRawMethod() {
        return "GET";
    }

}
