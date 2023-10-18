package co.casterlabs.rhs.impl;

import lombok.AllArgsConstructor;

@AllArgsConstructor
enum WebsocketOpCode {
    CONTINUATION(0),
    TEXT(1),
    BINARY(2),
    CLOSE(8),
    PING(9),
    PONG(10),
    ;

    public final int code;

}
