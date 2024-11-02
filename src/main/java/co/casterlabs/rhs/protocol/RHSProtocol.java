package co.casterlabs.rhs.protocol;

import java.io.IOException;

import co.casterlabs.rhs.util.DropConnectionException;
import co.casterlabs.rhs.util.HttpException;
import co.casterlabs.rhs.util.TaskExecutor;

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
    public abstract boolean process(S session, R response, RHSConnection connection, TaskExecutor executor) throws IOException, HttpException, DropConnectionException;

    public abstract R handle(S session, H handler);

    @Deprecated
    @SuppressWarnings("unchecked")
    public final boolean $process_cast(Object session, Object response, RHSConnection connection, TaskExecutor executor) throws IOException, HttpException {
        // This exists because of type erasure
        return process((S) session, (R) response, connection, executor);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public final Object $handle_cast(Object session, Object handler) {
        // This exists because of type erasure
        return handle((S) session, (H) handler);
    }

}
