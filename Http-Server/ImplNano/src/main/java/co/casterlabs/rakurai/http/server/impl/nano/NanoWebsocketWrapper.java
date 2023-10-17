package co.casterlabs.rakurai.http.server.impl.nano;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import co.casterlabs.rakurai.io.http.server.websocket.Websocket;
import co.casterlabs.rakurai.io.http.server.websocket.WebsocketListener;
import co.casterlabs.rakurai.io.http.server.websocket.WebsocketSession;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoWSD.WebSocket;
import fi.iki.elonen.NanoWSD.WebSocketFrame;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;
import fi.iki.elonen.NanoWSD.WebSocketFrame.OpCode;
import lombok.NonNull;

public class NanoWebsocketWrapper extends WebSocket {
    private WebsocketListener listener;

    private WebSocket instance = this;
    private RakuraiWebsocket rakWebsocket;

    public NanoWebsocketWrapper(IHTTPSession nanoSession, WebsocketListener listener, NanoWebsocketSessionWrapper wrapper) {
        super(nanoSession);

        this.listener = listener;
        this.rakWebsocket = new RakuraiWebsocket(wrapper);
    }

    // Nano WebSocket Impl
    @Override
    protected void onOpen() {
        new Thread(() -> {
            while (this.isOpen()) {
                try {
                    this.ping(":x-ping".getBytes());

                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception ignored) {}
            }
        }).start();

        this.listener.onOpen(this.rakWebsocket);
    }

    @Override
    protected void onClose(CloseCode code, String reason, boolean remote) {
        if (remote) {
            this.listener.onClose(this.rakWebsocket);
        }
    }

    @Override
    protected void onMessage(WebSocketFrame frame) {
        if (frame.getOpCode() == OpCode.Binary) {
            this.listener.onBinary(rakWebsocket, frame.getBinaryPayload());
        } else if (frame.getOpCode() == OpCode.Text) {
            this.listener.onText(rakWebsocket, frame.getTextPayload());
        }
    }

    @Override
    protected void onPong(WebSocketFrame pong) {}

    @Override
    protected void onException(IOException ignored) {}

    private class RakuraiWebsocket extends Websocket {

        public RakuraiWebsocket(@NonNull WebsocketSession session) {
            super(session);
        }

        @Override
        public void send(@NonNull String message) throws IOException {
            instance.send(message);
        }

        @Override
        public void send(@NonNull byte[] bytes) throws IOException {
            instance.send(bytes);
        }

        @Override
        public void close() throws IOException {
            try {
                instance.close(CloseCode.NormalClosure, "", false);
            } catch (Exception ignored) {}
        }

    }

}
