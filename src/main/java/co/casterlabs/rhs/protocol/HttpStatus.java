package co.casterlabs.rhs.protocol;

import org.jetbrains.annotations.Nullable;

public interface HttpStatus {

    default String getStatusString() {
        String str = String.valueOf(this.getStatusCode());

        String desc = this.getDescription();
        if (desc != null) str += " " + desc;

        return str;
    }

    public String getDescription();

    public int getStatusCode();

    public static HttpStatus adapt(int code, @Nullable String description) {
        return new HttpStatus() {
            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public int getStatusCode() {
                return code;
            }
        };
    }

}
