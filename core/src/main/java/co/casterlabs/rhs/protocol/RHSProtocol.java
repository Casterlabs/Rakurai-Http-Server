package co.casterlabs.rhs.protocol;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;

public abstract class RHSProtocol<S, R, H> {

    public abstract String name();

    public abstract S accept(RHSConnection connection) throws IOException, HttpException;

    /**
     * @return   true, if the connection was set up for Connection keep-alive. Most
     *           implementations probably want to return false to immediately close
     *           the connection.
     * 
     * @implSpec If you do plan on reusing the connection, you <b>MUST</b> swallow
     *           the response by the time this method returns.
     */
    public abstract boolean process(S session, R response, RHSConnection connection) throws IOException, HttpException, DropConnectionException, InterruptedException;

    public abstract @Nullable R handle(S session, H handler) throws DropConnectionException, HttpException;

    @Deprecated
    @SuppressWarnings("unchecked")
    public final boolean $process_cast(Object session, Object response, RHSConnection connection) throws IOException, HttpException, DropConnectionException, InterruptedException {
        // This exists because of type erasure
        return process((S) session, (R) response, connection);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public final @Nullable Object $handle_cast(Object session, Object handler) throws DropConnectionException, HttpException {
        // This exists because of type erasure
        return handle((S) session, (H) handler);
    }

}
