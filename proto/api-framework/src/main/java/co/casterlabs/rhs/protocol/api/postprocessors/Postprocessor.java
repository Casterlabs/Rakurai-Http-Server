package co.casterlabs.rhs.protocol.api.postprocessors;

import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;

/**
 * Runs after your request handler method is invoked. You can use this to add
 * cors headers, etc.
 * 
 * @implNote Websockets do not have post processors.
 */
public interface Postprocessor<R, S, A> {

    public void postprocess(S session, R response, EndpointData<A> data);

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    public static interface Http<A> extends Postprocessor<HttpResponse, HttpSession, A> {
    }

}
