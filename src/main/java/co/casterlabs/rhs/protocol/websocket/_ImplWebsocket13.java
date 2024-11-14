package co.casterlabs.rhs.protocol.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.marshalling.PrimitiveMarshall;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse.AcceptedWebsocketResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@SuppressWarnings("deprecation")
@RequiredArgsConstructor
class _ImplWebsocket13 extends Websocket {
    private final WebsocketSession session;
    private final AcceptedWebsocketResponse response;
    private final RHSConnection connection;

    private boolean isClosed = false;

    @Override
    public WebsocketSession session() {
        return this.session;
    }

    @Override
    public @Nullable String protocol() {
        return this.response.acceptedProtocol;
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
        if (this.isClosed) return;
        this.isClosed = true;

        try {
            this.sendFrame(true, WebsocketOpCode.CLOSE, new byte[0]);
        } catch (IOException e) {
            // Ignored.
        }
    }

    private static class WebsocketOpCode {
        private static final int CONTINUATION = 0;
        private static final int TEXT = 1;
        private static final int BINARY = 2;
        private static final int CLOSE = 8;
        private static final int PING = 9;
        private static final int PONG = 10;
    }

    private synchronized void sendOrFragment(int op, byte[] bytes) throws IOException {
        try {
            if (bytes.length <= this.connection.guessedMtu) {
                // Don't fragment.
                this.sendFrame(true, op, bytes);
                return;
            }

            int toWrite = bytes.length;
            int written = 0;

            while (toWrite > 0) {
                byte[] chunk = new byte[Math.min(toWrite, this.connection.guessedMtu)];
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
    void process() throws IOException {
        try {
            this.response.listener.onOpen(this);
        } catch (Throwable t) {
            this.connection.logger.warn("An exception occurred whilst opening listener:\n%s", t);
            return;
        }

        // For continuation frames.
        int fragmentedOpCode = 0;
        int fragmentedLength = 0;
        List<byte[]> fragmentedPackets = new LinkedList<>();

        while (!Thread.interrupted() && !this.isClosed) {
            // TODO accelerate reads by using a WorkBuffer.

            // @formatter:off
                int header1 = this.throwRead();
                int header2 = this.throwRead();
                
                boolean isFinished = (header1 & 0b10000000) != 0;
                boolean rsv1       = (header1 & 0b01000000) != 0;
                boolean rsv2       = (header1 & 0b00100000) != 0;
                boolean rsv3       = (header1 & 0b00010000) != 0;
                int     op         =  header1 & 0b00001111;
                
                boolean isMasked   = (header2 & 0b10000000) != 0;
                int     len7       =  header2 & 0b01111111;
                // @formatter:on

            if (rsv1 || rsv2 || rsv3) {
                this.connection.logger.fatal("Reserved bits are set, these are not supported! rsv1=%b rsv2=%b rsv3=%b", rsv1, rsv2, rsv3);
                return;
            }

            this.connection.logger.trace("fin=%b op=%d mask=%b len7=%d", isFinished, op, isMasked, len7);

            int length;
            if (len7 == 127) {
                // Unsigned 64bit, possibly negative.
                long length_long = PrimitiveMarshall.BIG_ENDIAN.bytesToLong(
                    this.connection.input.readNBytes(8)
                );

                if (Long.compareUnsigned(length_long, this.response.maxPayloadLength) > 0) {
                    this.connection.logger.fatal("Payload length too large, max %d bytes got %s bytes.", this.response.maxPayloadLength, Long.toUnsignedString(length_long));
                    return;
                }

                length = (int) length_long;
            } else if (len7 == 126) {
                // Unsigned 16bit, never negative. This can never be larger than
                // MAX_PAYLOAD_LENGTH.
                length = PrimitiveMarshall.BIG_ENDIAN.bytesToInt(new byte[] {
                        0,
                        0,
                        (byte) this.throwRead(),
                        (byte) this.throwRead(),
                });
            } else {
                // Unsigned 7bit, never negative. This can never be larger than
                // MAX_PAYLOAD_LENGTH.
                length = len7;
            }

            byte[] maskingKey = null;
            if (isMasked) {
                maskingKey = this.connection.input.readNBytes(4);
            }

            // Read in the whole payload.
            byte[] payload = this.connection.input.readNBytes(length);

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
                fragmentedLength += payload.length;
                if (fragmentedLength > this.response.maxPayloadLength) {
                    this.connection.logger.fatal("Fragmented payload length too large, max %d bytes got %d bytes.", this.response.maxPayloadLength, fragmentedLength);
                    return;
                }

                fragmentedPackets.add(payload);

                if (!isFinished) {
                    // Client is not yet finished, next packet pls.
                    continue;
                }

                // Combine all the fragments together.
                payload = new byte[fragmentedLength];
                int off = 0;
                for (byte[] fp : fragmentedPackets) {
                    System.arraycopy(fp, 0, payload, off, fp.length);
                    off += fp.length;
                }

                // We're finished! Parse it!
                op = fragmentedOpCode;
                fragmentedLength = 0;
                fragmentedPackets.clear();
                break;
            }

            // Parse the op code and do behavior tingz.
            switch (op) {
                case WebsocketOpCode.TEXT: {
                    this.connection.logger.trace("Got frame: TEXT.");
                    try {
                        String text = new String(payload, StandardCharsets.UTF_8);
                        payload = null; // Early free attempt.
                        this.connection.logger.debug("Text frame: %s", text);
                        this.response.listener.onText(this, text);
                    } catch (Throwable t) {
                        this.connection.logger.severe("Listener produced exception:\n%s", t);
                    }
                    break;
                }

                case WebsocketOpCode.BINARY: {
                    try {
                        this.connection.logger.trace("Got frame: BINARY.");
                        this.connection.logger.debug("Binary frame: len=%d", payload.length);
                        this.response.listener.onBinary(this, payload);
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
    }

    private int throwRead() throws IOException {
        int read = this.connection.input.read();
        if (read == -1) throw new IOException("Socket closed.");
        return read;
    }

}
