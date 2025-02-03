package co.casterlabs.rhs.protocol.api.endpoints;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@RequiredArgsConstructor
public class EndpointData<A /* This parameter type exists to provide a generic for Preprocessor attachments. */> {

    @Getter
    private final Map<String, String> uriParameters;

    /**
     * This comes from the preprocessor. Normally it is null.
     */
    @Getter(onMethod_ = {
            @Nullable
    })
    private final A attachment;

}
