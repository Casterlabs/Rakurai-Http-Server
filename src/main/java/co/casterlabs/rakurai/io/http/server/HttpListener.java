package co.casterlabs.rakurai.io.http.server;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.io.http.server.websocket.WebsocketListener;
import co.casterlabs.rakurai.io.http.server.websocket.WebsocketSession;
import lombok.NonNull;

public interface HttpListener {

    public @Nullable HttpResponse serveSession(@NonNull String host, @NonNull HttpSession session, boolean secure);

    public @Nullable WebsocketListener serveWebsocketSession(@NonNull String host, @NonNull WebsocketSession session, boolean secure);

}
