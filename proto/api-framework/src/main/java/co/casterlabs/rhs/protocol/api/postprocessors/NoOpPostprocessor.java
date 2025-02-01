package co.casterlabs.rhs.protocol.api.postprocessors;

import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;

public class NoOpPostprocessor {

    public static class Http implements Postprocessor.Http<Object> {
        @Override
        public void postprocess(HttpSession session, HttpResponse response, EndpointData<Object> data) {
            // NOOP
        }
    }

}
