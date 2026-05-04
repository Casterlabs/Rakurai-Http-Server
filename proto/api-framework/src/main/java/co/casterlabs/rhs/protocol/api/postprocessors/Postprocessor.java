package co.casterlabs.rhs.protocol.api.postprocessors;

import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;

/**
 * Runs after your request handler method is invoked. You can use this to add
 * cors headers, etc.
 * 
 * @apiNote Websockets do not have post processors.
 */
public interface Postprocessor<RESPONSE, SESSION, ATTACHMENT> {

    public void postprocess(SESSION session, RESPONSE response, EndpointData<ATTACHMENT> data);

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    public static interface Http<ATTACHMENT> extends Postprocessor<HttpResponse, HttpSession, ATTACHMENT> {
    }

}
