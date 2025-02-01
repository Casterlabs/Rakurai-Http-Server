package co.casterlabs.rhs.protocol.api.endpoints;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class EndpointData {
    private final Map<String, String> uriParameters;

}
