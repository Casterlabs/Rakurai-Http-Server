package co.casterlabs.rakurai.io.http.server.websocket;

import java.io.IOException;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class Websocket {
    private @NonNull @Getter WebsocketSession session;

    private Object attachment;

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachment() {
        return (T) this.attachment;
    }

    /**
     * Sends a text payload to the receiving end.
     *
     * @param message the message
     */
    public abstract void send(@NonNull String message) throws IOException;

    /**
     * Sends a byte payload to the receiving end.
     *
     * @param bytes the bytes
     */
    public abstract void send(@NonNull byte[] bytes) throws IOException;

    /**
     * Closes the connection.
     */
    public abstract void close() throws IOException;

}
