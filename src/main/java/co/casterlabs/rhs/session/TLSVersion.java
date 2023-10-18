package co.casterlabs.rhs.session;

import lombok.NonNull;

public enum TLSVersion {
    TLSv1,
    TLSv1_1,
    TLSv1_2,
    TLSv1_3,

    UNKNOWN; // Never used, hopefully.

    public String getRuntimeName() {
        return this.name().replace('_', '.');
    }

    @Override
    public String toString() {
        return this.getRuntimeName();
    }

    public static TLSVersion parse(@NonNull String runtime) {
        try {
            return valueOf(runtime.replace('.', '_'));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

}
