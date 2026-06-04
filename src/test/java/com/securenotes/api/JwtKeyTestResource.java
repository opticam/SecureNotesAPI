package com.securenotes.api;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

/**
 * Generates an ephemeral JWT key pair for tests before Quarkus starts.
 */
public class JwtKeyTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Path KEY_DIRECTORY = Path.of("target", "test-jwt-keys");

    @Override
    public Map<String, String> start() {
        try {
            Files.createDirectories(KEY_DIRECTORY);
            Path privateKeyPath = KEY_DIRECTORY.resolve("privateKey.pem").toAbsolutePath();
            Path publicKeyPath = KEY_DIRECTORY.resolve("publicKey.pem").toAbsolutePath();

            KeyPair keyPair = generateKeyPair();
            writePem(privateKeyPath, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
            writePem(publicKeyPath, "PUBLIC KEY", keyPair.getPublic().getEncoded());

            return Map.of(
                    "smallrye.jwt.sign.key.location", privateKeyPath.toString(),
                    "mp.jwt.verify.publickey.location", publicKeyPath.toString());
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to generate test JWT keys", exception);
        }
    }

    @Override
    public void stop() {
        // Keys are written under target/ and are removed by Maven clean.
    }

    private KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private void writePem(Path path, String label, byte[] encodedKey) throws IOException {
        String encoded = Base64.getMimeEncoder(64, System.lineSeparator().getBytes(StandardCharsets.UTF_8))
                .encodeToString(encodedKey);
        String pem = "-----BEGIN " + label + "-----"
                + System.lineSeparator()
                + encoded
                + System.lineSeparator()
                + "-----END " + label + "-----"
                + System.lineSeparator();
        Files.writeString(path, pem, StandardCharsets.UTF_8);
    }
}
