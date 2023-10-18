package co.casterlabs.rhs.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import co.casterlabs.commons.io.marshalling.PrimitiveMarshall;
import co.casterlabs.rhs.session.Websocket;
import co.casterlabs.rhs.session.WebsocketSession;
import lombok.NonNull;

class WebsocketImpl extends Websocket {
    private final Closeable toClose;
    private final OutputStream out;

    public WebsocketImpl(@NonNull WebsocketSession session, @NonNull OutputStream out, @NonNull Closeable toClose) {
        super(session);
        this.out = out;
        this.toClose = toClose;
    }

    @Override
    public void send(@NonNull String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        this.sendOrFragment(WebsocketOpCode.TEXT, bytes);

    }

    @Override
    public void send(@NonNull byte[] bytes) throws IOException {
        this.sendOrFragment(WebsocketOpCode.BINARY, bytes);
    }

    @Override
    public void close() throws IOException {
        try {
            this.sendFrame(true, WebsocketOpCode.CLOSE, new byte[0]);
        } catch (IOException e) {
            // Ignored.
        } finally {
            RakuraiHttpServer.safeClose(this.toClose);
        }
    }

    private void sendOrFragment(WebsocketOpCode op, byte[] bytes) throws IOException {
        synchronized (this.out) {
            try {
                if (bytes.length <= WebsocketProtocol.MAX_PAYLOAD_LENGTH) {
                    // Don't fragment.
                    this.sendFrame(true, op, bytes);
                    return;
                }

                int toWrite = bytes.length;
                int written = 0;

                while (toWrite > 0) {
                    byte[] chunk = new byte[Math.min(toWrite, WebsocketProtocol.MAX_PAYLOAD_LENGTH)];
                    System.arraycopy(bytes, written, chunk, 0, chunk.length);
                    toWrite -= chunk.length;

                    boolean fin = toWrite == 0;
                    WebsocketOpCode chunkOp = written == 0 ? op : WebsocketOpCode.CONTINUATION;

                    this.sendFrame(fin, chunkOp, chunk);

                    written += chunk.length;
                }
            } catch (IOException e) {
                RakuraiHttpServer.safeClose(this.toClose);
                throw e;
            }
        }
    }

    void sendFrame(boolean fin, WebsocketOpCode op, byte[] bytes) throws IOException {
        synchronized (this.out) {
            int len7 = bytes.length;
            if (len7 > 125) {
                if (bytes.length > 65535) {
                    len7 = 127; // Use 64bit length.
                } else {
                    len7 = 126; // Use 16bit length.
                }
            }

            int header1 = 0;
            header1 |= (fin ? 1 : 0) << 7;
            header1 |= op.code;
            this.out.write(header1);

            int header2 = 0;
            header2 |= len7;
//        header2 |= 0b00000000; // Mask.
            this.out.write(header2);

            if (len7 == 126) {
                byte[] lenBytes = PrimitiveMarshall.BIG_ENDIAN.intToBytes(bytes.length);
                this.out.write(lenBytes[2]);
                this.out.write(lenBytes[3]); // We only need the first 16 bits.
            } else if (len7 == 127) {
                byte[] lenBytes = PrimitiveMarshall.BIG_ENDIAN.longToBytes(bytes.length);
                this.out.write(lenBytes);
            }

            this.out.write(bytes);
        }
    }

}
