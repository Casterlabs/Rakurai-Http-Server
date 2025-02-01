package co.casterlabs.rhs.protocol.api.endpoints;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class EndpointData<A /* This parameter type exists to provide a generic for Preprocessor attachments. */> {
    private final Map<String, String> uriParameters;

    /**
     * This comes from the preprocessor. Normally it is null.
     */
    private final A attachment;

}
