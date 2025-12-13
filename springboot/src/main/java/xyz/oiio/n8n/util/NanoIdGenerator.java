package xyz.oiio.n8n.util;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Component
public class NanoIdGenerator implements IdentifierGenerator {

    private static final String DEFAULT_ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int DEFAULT_SIZE = 21;
    private static final SecureRandom random = new SecureRandom();

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return randomNanoId();
    }

    public static String randomNanoId() {
        return randomNanoId(DEFAULT_ALPHABET, DEFAULT_SIZE);
    }

    public static String randomNanoId(String alphabet, int size) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("alphabet must not be empty");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }

        StringBuilder id = new StringBuilder(size);
        int mask = (2 << (int) (Math.log(alphabet.length() - 1) / Math.log(2))) - 1;
        int step = (int) Math.ceil(1.6 * mask * size / alphabet.length());

        byte[] bytes = new byte[step];
        random.nextBytes(bytes);

        for (int i = 0; i < step; i++) {
            int alphabetIndex = bytes[i] & mask;
            if (alphabetIndex < alphabet.length()) {
                id.append(alphabet.charAt(alphabetIndex));
                if (id.length() == size) {
                    break;
                }
            }
        }

        if (id.length() != size) {
            // Fallback to hash-based approach if random approach fails
            String hash = generateHash();
            id = new StringBuilder(hash.substring(0, Math.min(size, hash.length())));
            while (id.length() < size) {
                int alphabetIndex = random.nextInt(alphabet.length());
                id.append(alphabet.charAt(alphabetIndex));
            }
        }

        return id.toString();
    }

    private static String generateHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((System.nanoTime() + String.valueOf(random.nextLong())).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            return "" + System.currentTimeMillis() + random.nextInt(1000);
        }
    }
}