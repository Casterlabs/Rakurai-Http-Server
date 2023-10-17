package co.casterlabs.rakurai.json.deserialization;

import co.casterlabs.rakurai.json.element.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParsedTokenPair {
    private JsonElement element;
    private int read;

}
