package co.casterlabs.rhs.util;

public class SSLUtil {

    public static void applyDHSize(int DHSize) {
        // https://www.java.com/en/configure_crypto.html
        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#customizing_dh_keys
        System.setProperty("jdk.tls.ephemeralDHKeySize", String.valueOf(DHSize));
        String disabledAlgorithmsProperty = System.getProperty("jdk.tls.disabledAlgorithms", "DH keySize");
        String[] disabledAlgorithms = disabledAlgorithmsProperty.split(",");
        boolean replacedParameter = false;

        for (int i = 0; i != disabledAlgorithms.length; i++) {
            if (disabledAlgorithms[i].startsWith("DH keySize")) {
                replacedParameter = true;

                disabledAlgorithms[i] = "DH keySize < " + DHSize;

                break;
            }
        }

        if (replacedParameter) {
            System.setProperty("jdk.tls.disabledAlgorithms", String.join(", ", disabledAlgorithms));
        } else {
            System.setProperty("jdk.tls.disabledAlgorithms", disabledAlgorithmsProperty + ", DH keySize < " + DHSize);
        }
    }

}
