package co.casterlabs.rakurai.io.http.server.websocket;

public interface WebsocketListener {

    default void onOpen(Websocket websocket) {}

    default void onText(Websocket websocket, String message) {}

    default void onBinary(Websocket websocket, byte[] bytes) {}

    default void onClose(Websocket websocket) {}

}
