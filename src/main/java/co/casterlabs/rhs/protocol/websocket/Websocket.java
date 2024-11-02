package co.casterlabs.rhs.protocol.websocket;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public abstract class Websocket {
    private Object attachment;

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachment() {
        return (T) this.attachment;
    }

    public abstract WebsocketSession session();

    public final @Nullable String protocol() {
        return this.session().protocol();
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

    /* ---------------- */
    /* Internal Methods */
    /* ---------------- */

    abstract void ping();

    abstract void process();

}
