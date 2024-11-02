package co.casterlabs.rhs.protocol;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HttpVersion {
    HTTP_0_9(0.9, "HTTP/0.9"),
    HTTP_1_0(1.0, "HTTP/1.0"),
    HTTP_1_1(1.1, "HTTP/1.1"),
//    HTTP_2_0(2.0, "HTTP/2"),
//    HTTP_3_0(3.0, "HTTP/3"), // Uncomment when we support them :^)
    ;

    public final double value;
    private final String name;

    @Override
    public String toString() {
        return this.name;
    }

    public static HttpVersion fromString(String str) {
        if (str.length() == 0) {
            return HTTP_0_9; // 0.9 did not have a version identifier.
        }

        str = str.trim();

        int indexOfSlash = str.indexOf('/');
        if (indexOfSlash == -1) {
            throw new IllegalArgumentException("Invalid http version: " + str);
        }

        // Chop off the leading "HTTP/"
        str = str.substring(indexOfSlash + 1);

        switch (str) {
            case "1":
            case "1.0":
                return HTTP_1_0;

            case "1.1":
                return HTTP_1_1;

            // Uncomment when we support them :^)
//            case "2":
//            case "2.0":
//                return HTTP_2_0;
//
//            case "3":
//            case "3.0":
//                return HTTP_3_0;

            default:
                throw new IllegalArgumentException("Invalid http version: " + str);
        }
    }

}
