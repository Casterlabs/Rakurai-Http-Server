package co.casterlabs.rhs.server;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

public enum TLSVersion {
    TLSv1,
    TLSv1_1,
    TLSv1_2,
    TLSv1_3,

    UNKNOWN; // Never used, hopefully.

    @Override
    public String toString() {
        return this.name().replace('_', '.');
    }

    public static TLSVersion parse(@NonNull String runtime) {
        try {
            return valueOf(runtime.replace('.', '_'));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static String[] toRuntimeNames(TLSVersion... versions) {
        List<String> list = new ArrayList<>(versions.length);
        for (TLSVersion v : versions) {
            if (v == UNKNOWN) continue;
            list.add(v.toString());
        }
        return list.toArray(new String[0]);
    }

}
