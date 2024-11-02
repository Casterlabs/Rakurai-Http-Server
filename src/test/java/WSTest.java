import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rhs.protocol.websocket.Websocket;
import co.casterlabs.rhs.protocol.websocket.WebsocketListener;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtoAdapter;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;
import lombok.SneakyThrows;

public class WSTest {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
//        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);
        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(
                new WebsocketProtoAdapter(), (session) -> new WebsocketListener() {
                    @SneakyThrows
                    @Override
                    public void onText(Websocket websocket, String message) {
                        websocket.send(message);
                    }

                    @SneakyThrows
                    @Override
                    public void onBinary(Websocket websocket, byte[] bytes) {
                        websocket.send(bytes);
                    };
                }
            )
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

}
