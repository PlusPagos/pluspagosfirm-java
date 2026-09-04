package com.pluspagos;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class SHA256Firm {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public String getFirm(String ipClient, String secretKey, String guidComercio, String sucursalId, String monto)
            throws NoSuchAlgorithmException, InvalidKeyException {

        String input = ipClient + "*" + guidComercio + "*" + sucursalId + "*" + monto;

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        byte[] hashedBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder output = new StringBuilder(2 * hashedBytes.length);
        for (byte b : hashedBytes) {
            output.append(String.format("%02x", b));
        }
        return output.toString();
    }
}
