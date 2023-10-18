package co.casterlabs.rhs.server;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.session.HttpSession;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.rhs.session.WebsocketSession;
import lombok.NonNull;

public interface HttpListener {

    public @Nullable HttpResponse serveSession(@NonNull String host, @NonNull HttpSession session, boolean secure);

    public @Nullable WebsocketListener serveWebsocketSession(@NonNull String host, @NonNull WebsocketSession session, boolean secure);

}
