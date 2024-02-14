import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.StandardHttpStatus;
import co.casterlabs.rhs.server.HttpListener;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;
import co.casterlabs.rhs.session.HttpSession;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.rhs.session.WebsocketSession;
import lombok.NonNull;

public class Test {

    public static void main(String[] args) throws IOException {
        HttpServer server = new HttpServerBuilder()
            .withPort(80)
            .build(new HttpListener() {
                @Override
                public @Nullable HttpResponse serveHttpSession(@NonNull HttpSession session) {
                    String body = String.format("Hello %s!", session.getRemoteIpAddress());

                    return HttpResponse
                        .newFixedLengthResponse(StandardHttpStatus.OK, body)
                        .setMimeType("text/plain");
                }

                @Override
                public @Nullable WebsocketListener serveWebsocketSession(@NonNull WebsocketSession session) {
                    // Returning null will drop the connection.
                    return null;
                }
            });

        server.start(); // Open up http://127.0.0.1:8080
    }

}
