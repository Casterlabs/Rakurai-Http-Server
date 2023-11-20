package co.casterlabs.rhs.session;

import java.io.IOException;
import java.io.InputStream;

public abstract class WebsocketSession extends HttpSession {

    @Override
    public boolean hasBody() {
        return false;
    }

    @Override
    public InputStream getRequestBodyStream() throws IOException {
        throw new UnsupportedOperationException("Websockets do not support request bodies.");
    }

//    /**
//     * @deprecated Websockets do not support this.
//     */
//    @Deprecated
//    @Override
//    public Either<MultipartForm, URLEncodedForm> parseFormBody() throws IOException {
//        throw new UnsupportedOperationException();
//    }

}
