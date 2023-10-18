package co.casterlabs.rhs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import co.casterlabs.commons.io.marshalling.PrimitiveMarshall;
import co.casterlabs.rhs.session.WebsocketListener;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

class WebsocketProtocol {
    public static final int MAX_CHUNK_LENGTH = Integer.parseInt(System.getProperty("rakurai.rhs.ws.maxchunklength", "65535"));
    public static final int MAX_PAYLOAD_LENGTH = 16 /* 16mb */ * 1024 * 1024;
    public static final long READ_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

    public static void doPing(WebsocketImpl websocket) {
        byte[] someBytes = PrimitiveMarshall.BIG_ENDIAN.longToBytes(System.currentTimeMillis());
        try {
            websocket.sendFrame(true, WebsocketOpCode.PING, someBytes);
        } catch (IOException ignored) {}
    }

    public static void handleWebsocketRequest(Socket client, HttpSessionImpl session, WebsocketImpl websocket, WebsocketListener listener) throws IOException {
        InputStream in = client.getInputStream();
        FastLogger sessionLogger = session.getLogger();

        // For continuation frames.
        int fragmentedOpCode = 0;
        byte[] fragmentedPacket = new byte[0];

        while (!client.isClosed()) {
            if (Thread.interrupted()) return;

            // @formatter:off
            int header1 = throwRead(in);
            
            boolean isFinished = (header1 & 0b10000000) != 0;
            boolean rsv1       = (header1 & 0b01000000) != 0;
            boolean rsv2       = (header1 & 0b00100000) != 0;
            boolean rsv3       = (header1 & 0b00010000) != 0;
            int op             =  header1 & 0b00001111;
            
            int header2 = throwRead(in);
            
            boolean isMasked   = (header2 & 0b10000000) != 0;
            int    len7        =  header2 & 0b01111111;
            // @formatter:on

            if (rsv1 || rsv2 || rsv3) {
                session.getLogger().fatal("Reserved bits are set, these are not supported! rsv1=%b rsv2=%b rsv3=%b", rsv1, rsv2, rsv3);
                return;
            }

            sessionLogger.trace("fin=%b op=%d mask=%b len7=%d", isFinished, op, isMasked, len7);

            long length;

            if (len7 == 126) {
                // 16bit.
                length = PrimitiveMarshall.BIG_ENDIAN.bytesToInt(new byte[] {
                        0,
                        0,
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                });
            } else if (len7 == 127) {
                // 64bit.
                length = PrimitiveMarshall.BIG_ENDIAN.bytesToLong(new byte[] {
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                });

                if (Long.compareUnsigned(length, MAX_PAYLOAD_LENGTH) > 0) {
                    sessionLogger.fatal("Payload length too big, max 16mb got %d.", length);
                    return;
                }
            } else {
                length = len7;
            }

            sessionLogger.trace("trueLength=%d", length);

            byte[] maskingKey = null;
            if (isMasked) {
                maskingKey = new byte[] {
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                        (byte) throwRead(in),
                };
            }

            // Read in the whole payload.
            byte[] payload = new byte[(int) length];

            int read = 0;
            while (read < payload.length) {
                int bytesRead = in.read(payload, read, payload.length - read);
                if (bytesRead == -1) throw new IOException("Socket closed.");
                read += bytesRead;
            }

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
                    sessionLogger.fatal("Fragmented payload length too big, max 16mb got %d.", length);
                    return;
                }

                byte[] wholePayload = new byte[totalLength];
                System.arraycopy(fragmentedPacket, 0, wholePayload, 0, fragmentedPacket.length);
                System.arraycopy(payload, 0, wholePayload, fragmentedPacket.length, payload.length);

                fragmentedPacket = wholePayload;

                if (!isFinished) {
                    // Client is not yet finished next packet pls.
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

                case 0x1: { // Text
                    sessionLogger.trace("Got frame: TEXT.");
                    try {
                        String text = new String(payload, StandardCharsets.UTF_8);
                        sessionLogger.debug("Text frame: %s", text);
                        listener.onText(websocket, text);
                    } catch (Throwable t) {
                        sessionLogger.severe("Listener produced exception:\n%s", t);
                    }
                    break;
                }

                case 0x2: { // Binary
                    try {
                        sessionLogger.trace("Got frame: BINARY.");
                        sessionLogger.debug("Binary frame: len=%d", payload.length);
                        listener.onBinary(websocket, payload);
                    } catch (Throwable t) {
                        sessionLogger.severe("Listener produced exception:\n%s", t);
                    }
                    break;
                }

                case 0x8: { // Close
                    sessionLogger.trace("Got frame: CLOSE.");
                    websocket.close(); // Send close reply.
                    return;
                }

                case 0x9: { // Ping
                    sessionLogger.trace("Got frame: PING.");
                    websocket.sendFrame(true, WebsocketOpCode.PONG, payload); // Send pong reply.
                    continue;
                }

                case 0xa: { // Pong
                    sessionLogger.trace("Got frame: PONG.");
                    continue;
                }

                default: // Reserved
                    continue;

            }
        }
    }

    private static int throwRead(InputStream in) throws IOException {
        int read = in.read();
        if (read == -1) throw new IOException("Socket closed.");
        return read;
    }

}
