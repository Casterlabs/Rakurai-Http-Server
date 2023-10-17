package co.casterlabs.rakurai.io.http.server.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.io.http.TLSVersion;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain = true)
public class SSLConfiguration {
    private @NonNull TLSVersion[] enabledTlsVersions = TLSVersion.values();
    private @Nullable List<String> enabledCipherSuites;
    private @Setter int DHSize = 2048;

    private File keystoreLocation;
    private char[] keystorePassword;

    public SSLConfiguration(@NonNull File keystoreLocation, @NonNull char[] keystorePassword) {
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
    }

    public SSLConfiguration setEnabledTlsVersions(@NonNull TLSVersion... enabledTlsVersions) {
        this.enabledTlsVersions = enabledTlsVersions;
        return this;
    }

    public SSLConfiguration setEnabledCipherSuites(@Nullable String... enabledCipherSuites) {
        if (enabledCipherSuites == null) {
            this.enabledCipherSuites = null;
        } else {
            this.enabledCipherSuites = Arrays.asList(enabledCipherSuites);
        }
        return this;
    }

    @Override
    public SSLConfiguration clone() {
        SSLConfiguration nu = new SSLConfiguration(this.keystoreLocation, this.keystorePassword);
        nu.enabledTlsVersions = this.enabledTlsVersions;
        nu.enabledCipherSuites = this.enabledCipherSuites;
        nu.DHSize = this.DHSize;
        return nu;
    }

    public String[] convertTLS() {
        TLSVersion[] tls = this.enabledTlsVersions;
        String[] versions = new String[tls.length];

        for (int i = 0; i != tls.length; i++) {
            versions[i] = tls[i].getRuntimeName();
        }

        return versions;
    }

    public void applyDHSize() {
        // https://www.java.com/en/configure_crypto.html
        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#customizing_dh_keys
        System.setProperty("jdk.tls.ephemeralDHKeySize", String.valueOf(this.DHSize));
        String disabledAlgorithmsProperty = System.getProperty("jdk.tls.disabledAlgorithms", "DH keySize");
        String[] disabledAlgorithms = disabledAlgorithmsProperty.split(",");
        boolean replacedParameter = false;

        for (int i = 0; i != disabledAlgorithms.length; i++) {
            if (disabledAlgorithms[i].startsWith("DH keySize")) {
                replacedParameter = true;

                disabledAlgorithms[i] = "DH keySize < " + this.DHSize;

                break;
            }
        }

        if (replacedParameter) {
            System.setProperty("jdk.tls.disabledAlgorithms", String.join(", ", disabledAlgorithms));
        } else {
            System.setProperty("jdk.tls.disabledAlgorithms", disabledAlgorithmsProperty + ", DH keySize < " + this.DHSize);
        }
    }

}
