package co.casterlabs.rhs.server;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.session.HttpSession;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.rhs.session.WebsocketSession;
import lombok.NonNull;

public interface HttpListener {

    public @Nullable HttpResponse serveHttpSession(@NonNull HttpSession session);

    public @Nullable WebsocketListener serveWebsocketSession(@NonNull WebsocketSession session);

}
