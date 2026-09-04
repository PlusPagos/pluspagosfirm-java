package com.pluspagos;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;


public final class AESEncrypter {

    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AESEncrypter() {
    }

    public static String encryptString(String plainText, String phrase) {
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(salt);

            SecretKey key = deriveKey(phrase, salt);

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] cipherTextAndTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[salt.length + iv.length + cipherTextAndTag.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(cipherTextAndTag, 0, combined, salt.length + iv.length, cipherTextAndTag.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new PlusPagosCryptoException("No se pudo cifrar el payload", e);
        }
    }

    public static String decryptString(String encodedCipherText, String phrase) {
        try {
            byte[] combined = Base64.getDecoder().decode(encodedCipherText);

            if (combined.length < SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES) {
                throw new PlusPagosCryptoException("Criptograma inválido: longitud insuficiente", null);
            }

            byte[] salt = new byte[SALT_LENGTH_BYTES];
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] cipherTextAndTag = new byte[combined.length - salt.length - iv.length];

            System.arraycopy(combined, 0, salt, 0, salt.length);
            System.arraycopy(combined, salt.length, iv, 0, iv.length);
            System.arraycopy(combined, salt.length + iv.length, cipherTextAndTag, 0, cipherTextAndTag.length);

            SecretKey key = deriveKey(phrase, salt);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plain = cipher.doFinal(cipherTextAndTag);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            // El tag de autenticación GCM no coincide: el criptograma fue
            // alterado en tránsito o la clave/phrase no es la correcta.
            throw new PlusPagosCryptoException("Falló la verificación de integridad del criptograma", e);
        } catch (GeneralSecurityException e) {
            throw new PlusPagosCryptoException("No se pudo descifrar el payload", e);
        } catch (IllegalArgumentException e) {
            throw new PlusPagosCryptoException("Criptograma con codificación Base64 inválida", e);
        }
    }

    private static SecretKey deriveKey(String phrase, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(phrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        try {
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            spec.clearPassword();
        }
    }
}
