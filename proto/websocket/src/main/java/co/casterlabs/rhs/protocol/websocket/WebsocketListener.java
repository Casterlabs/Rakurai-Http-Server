package co.casterlabs.rhs.protocol.websocket;

import java.io.IOException;

public interface WebsocketListener {

    /**
     * This is called in a {@link TaskType#HEAVY_IO} context. This method blocks the
     * read loop.
     */
    default void onOpen(Websocket websocket) throws IOException {}

    /**
     * This is called in a {@link TaskType#HEAVY_IO} context. This method blocks the
     * next read.
     */
    default void onText(Websocket websocket, String message) throws IOException {}

    /**
     * This is called in a {@link TaskType#HEAVY_IO} context. This method blocks the
     * next read.
     */
    default void onBinary(Websocket websocket, byte[] bytes) throws IOException {}

    /**
     * This is called in a {@link TaskType#LIGHT_IO} context. This method blocks
     * connection cleanup.
     * 
     * @implNote websocket MAY be `null` IF the connection failed early in the
     *           process.
     */
    default void onClose(Websocket websocket) {}

}
