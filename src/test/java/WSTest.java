import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.protocol.websocket.Websocket;
import co.casterlabs.rhs.protocol.websocket.WebsocketListener;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;

public class WSTest {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
//        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);
        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(
                new WebsocketProtocol(), (session) -> WebsocketResponse.accept(
                    new WebsocketListener() {
                        @Override
                        public void onText(Websocket websocket, String message) throws IOException {
                            websocket.send(message);
                        }

                        @Override
                        public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
                            websocket.send(bytes);
                        }
                    },
                    session.firstProtocol()
                )
            )
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

}
