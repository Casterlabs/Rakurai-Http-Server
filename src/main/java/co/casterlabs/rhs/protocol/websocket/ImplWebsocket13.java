package co.casterlabs.rhs.protocol.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import co.casterlabs.commons.io.marshalling.PrimitiveMarshall;
import co.casterlabs.rhs.protocol.RHSConnection;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@SuppressWarnings("deprecation")
@RequiredArgsConstructor
class ImplWebsocket13 extends Websocket {
    private static final int MAX_PAYLOAD_LENGTH = 64 /* 64mb */ * 1024 * 1024;

    private final WebsocketSession session;
    private final WebsocketListener listener;
    private final RHSConnection connection;

    private static class WebsocketOpCode {
        private static final int CONTINUATION = 0;
        private static final int TEXT = 1;
        private static final int BINARY = 2;
        private static final int CLOSE = 8;
        private static final int PING = 9;
        private static final int PONG = 10;
    }

    @Override
    public WebsocketSession session() {
        return this.session;
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
    public void close() {
        try {
            this.sendFrame(true, WebsocketOpCode.CLOSE, new byte[0]);
        } catch (IOException e) {
            // Ignored.
        }
    }

    private synchronized void sendOrFragment(int op, byte[] bytes) throws IOException {
        try {
            if (bytes.length <= MAX_PAYLOAD_LENGTH) {
                // Don't fragment.
                this.sendFrame(true, op, bytes);
                return;
            }

            int toWrite = bytes.length;
            int written = 0;

            while (toWrite > 0) {
                byte[] chunk = new byte[Math.min(toWrite, MAX_PAYLOAD_LENGTH)];
                System.arraycopy(bytes, written, chunk, 0, chunk.length);
                toWrite -= chunk.length;

                boolean fin = toWrite == 0;
                int chunkOp = written == 0 ? op : WebsocketOpCode.CONTINUATION;

                this.sendFrame(fin, chunkOp, chunk);

                written += chunk.length;
            }
        } catch (IOException e) {
            this.close();
            throw e;
        }
    }

    private synchronized void sendFrame(boolean fin, int op, byte[] bytes) throws IOException {
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
        header1 |= op;

        int header2 = 0;
        header2 |= len7;
//        header2 |= 0b00000000; // Mask.

        // Nagle's algorithm is disabled (aka no delay mode), so we batch writes to be
        // more efficient.
        if (len7 == 126) {
            byte[] headerBytes = PrimitiveMarshall.BIG_ENDIAN.intToBytes(bytes.length);
            headerBytes[0] = (byte) header1;
            headerBytes[1] = (byte) header2; // We only need the first 16 bits from length, so we can overwrite 1-2 safely.

            this.connection.output.write(headerBytes);
        } else if (len7 == 127) {
            byte[] lenBytes = PrimitiveMarshall.BIG_ENDIAN.longToBytes(bytes.length);

            byte[] headerBytes = new byte[Long.BYTES + 2];
            headerBytes[0] = (byte) header1;
            headerBytes[1] = (byte) header2;

            System.arraycopy(lenBytes, 0, headerBytes, 2, Long.BYTES);

            this.connection.output.write(headerBytes);
        } else {
            this.connection.output.write(new byte[] {
                    (byte) header1,
                    (byte) header2
            });
        }

        // Note we use an MTUOutputStream here so that we batch writes to be more
        // efficient when transmitted over the wire.
        this.connection.output.write(bytes);
    }

    /* ---------------- */
    /* Internal Methods */
    /* ---------------- */

    @Override
    void ping() {
        byte[] someBytes = PrimitiveMarshall.BIG_ENDIAN.longToBytes(System.currentTimeMillis());
        try {
            this.sendFrame(true, WebsocketOpCode.PING, someBytes);
        } catch (IOException ignored) {}
    }

    @Override
    void process() {
        try {
            // For continuation frames.
            int fragmentedOpCode = 0;
            byte[] fragmentedPacket = new byte[0];

            while (true) {
                if (Thread.interrupted()) return;

                // TODO accelerate reads by using a WorkBuffer.

                // @formatter:off
                int header1 = this.throwRead();
                
                boolean isFinished = (header1 & 0b10000000) != 0;
                boolean rsv1       = (header1 & 0b01000000) != 0;
                boolean rsv2       = (header1 & 0b00100000) != 0;
                boolean rsv3       = (header1 & 0b00010000) != 0;
                int op             =  header1 & 0b00001111;
                
                int header2 = this.throwRead();
                
                boolean isMasked   = (header2 & 0b10000000) != 0;
                int    len7        =  header2 & 0b01111111;
                // @formatter:on

                if (rsv1 || rsv2 || rsv3) {
                    this.connection.logger.fatal("Reserved bits are set, these are not supported! rsv1=%b rsv2=%b rsv3=%b", rsv1, rsv2, rsv3);
                    return;
                }

                this.connection.logger.trace("fin=%b op=%d mask=%b len7=%d", isFinished, op, isMasked, len7);

                long length;

                if (len7 == 126) {
                    // 16bit.
                    length = PrimitiveMarshall.BIG_ENDIAN.bytesToInt(new byte[] {
                            0,
                            0,
                            (byte) this.throwRead(),
                            (byte) this.throwRead(),
                    });
                } else if (len7 == 127) {
                    // 64bit.
                    length = PrimitiveMarshall.BIG_ENDIAN.bytesToLong(
                        this.connection.input.readNBytes(8)
                    );

                    if (Long.compareUnsigned(length, MAX_PAYLOAD_LENGTH) > 0) {
                        this.connection.logger.fatal("Payload length too big, max %dmb got %d.", MAX_PAYLOAD_LENGTH / 1024 / 1024, length);
                        return;
                    }
                } else {
                    length = len7;
                }

                this.connection.logger.trace("trueLength=%d", length);

                byte[] maskingKey = null;
                if (isMasked) {
                    maskingKey = this.connection.input.readNBytes(4);
                }

                // Read in the whole payload.
                byte[] payload = this.connection.input.readNBytes((int) length);

                // XOR decrypt.
                if (isMasked) {
                    for (int idx = 0; idx < payload.length; idx++) {
                        payload[idx] ^= maskingKey[idx % 4];
                    }
                }

                // We're starting a new fragmented message, store this info for later.
                if (!isFinished && op != 0) {
                    fragmentedOpCode = op;
                }

                // Handle fragmented messages.
                if (op == 0) {
                    int totalLength = fragmentedPacket.length + payload.length;
                    if (totalLength > MAX_PAYLOAD_LENGTH) {
                        this.connection.logger.fatal("Fragmented payload length too big, max %dmb got %d.", MAX_PAYLOAD_LENGTH / 1024 / 1024, length);
                        return;
                    }

                    byte[] wholePayload = new byte[totalLength];
                    System.arraycopy(fragmentedPacket, 0, wholePayload, 0, fragmentedPacket.length);
                    System.arraycopy(payload, 0, wholePayload, fragmentedPacket.length, payload.length);

                    fragmentedPacket = wholePayload;

                    if (!isFinished) {
                        // Client is not yet finished, next packet pls.
                        continue;
                    }

                    // We're finished! Parse it!
                    payload = fragmentedPacket;
                    op = fragmentedOpCode;
                    fragmentedPacket = new byte[0];
                    break;
                }

                // Parse the op code and do behavior tingz.
                switch (op) {
                    case WebsocketOpCode.TEXT: {
                        this.connection.logger.trace("Got frame: TEXT.");
                        try {
                            String text = new String(payload, StandardCharsets.UTF_8);
                            this.connection.logger.debug("Text frame: %s", text);
                            this.listener.onText(this, text);
                        } catch (Throwable t) {
                            this.connection.logger.severe("Listener produced exception:\n%s", t);
                        }
                        break;
                    }

                    case WebsocketOpCode.BINARY: {
                        try {
                            this.connection.logger.trace("Got frame: BINARY.");
                            this.connection.logger.debug("Binary frame: len=%d", payload.length);
                            byte[] $payload = payload;
                            this.listener.onBinary(this, $payload);
                        } catch (Throwable t) {
                            this.connection.logger.severe("Listener produced exception:\n%s", t);
                        }
                        break;
                    }

                    case WebsocketOpCode.CLOSE: {
                        this.connection.logger.trace("Got frame: CLOSE.");
                        this.close(); // Send close reply.
                        return;
                    }

                    case WebsocketOpCode.PING: {
                        this.connection.logger.trace("Got frame: PING.");
                        this.sendFrame(true, WebsocketOpCode.PONG, payload); // Send pong reply.
                        continue;
                    }

                    case WebsocketOpCode.PONG: {
                        this.connection.logger.trace("Got frame: PONG.");
                        continue;
                    }

                    default: // Reserved
                        continue;
                }
            }
        } catch (IOException ignored) {}
    }

    private int throwRead() throws IOException {
        int read = this.connection.input.read();
        if (read == -1) throw new IOException("Socket closed.");
        return read;
    }

}
