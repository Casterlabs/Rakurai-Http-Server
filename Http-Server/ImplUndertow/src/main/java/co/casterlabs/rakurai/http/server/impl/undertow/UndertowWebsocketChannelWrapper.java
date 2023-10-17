package co.casterlabs.rakurai.http.server.impl.undertow;

import java.io.IOException;
import java.nio.ByteBuffer;

import co.casterlabs.rakurai.StringUtil;
import co.casterlabs.rakurai.io.http.server.websocket.Websocket;
import co.casterlabs.rakurai.io.http.server.websocket.WebsocketSession;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import lombok.NonNull;

public class UndertowWebsocketChannelWrapper extends Websocket {
    private WebSocketChannel channel;
    private boolean logWebsocketFrames;

    public UndertowWebsocketChannelWrapper(WebSocketChannel channel, WebsocketSession session) {
        super(session);
        this.channel = channel;
        this.logWebsocketFrames = System.getProperty("rakurailogwebsocketframes", "").equals("true");
    }

    @Override
    public void send(@NonNull String message) throws IOException {
        if (this.logWebsocketFrames) {
            this.getSession().getLogger().trace("Sending text: %s", message);
        }
        WebSockets.sendText(message, this.channel, null);
    }

    @Override
    public void send(@NonNull byte[] bytes) throws IOException {
        if (this.logWebsocketFrames) {
            this.getSession().getLogger().trace("Sending bytes: %s", StringUtil.bytesToHex(bytes));
        }
        WebSockets.sendBinary(ByteBuffer.wrap(bytes), this.channel, null);
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel.sendClose();
        } catch (IOException e) {}
    }

}
