package digital.paynetics.phos.classes.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import digital.paynetics.phos.sdk.PhosLogger;

public class Sha256Calculator {
    public static String sha256(final String s, PhosLogger logger) {
        final String SHA256 = "SHA-256";
        try {
            // Create SHA256 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(SHA256);
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            logger.e(SHA256, "Failed to calculate a SHA256 checksum - " + e);
        }

        return null;
    }
}
