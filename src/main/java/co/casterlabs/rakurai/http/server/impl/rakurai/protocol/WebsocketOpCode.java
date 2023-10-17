package co.casterlabs.rakurai.http.server.impl.rakurai.protocol;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum WebsocketOpCode {
    CONTINUATION(0),
    TEXT(1),
    BINARY(2),
    CLOSE(8),
    PING(9),
    PONG(10),
    ;

    public final int code;

}
