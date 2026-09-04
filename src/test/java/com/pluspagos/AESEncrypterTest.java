package com.pluspagos;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Prueba de round-trip para AESEncrypter. Reemplaza a la antigua clase
 * Test.java (hallazgos SEC-04 y COD-01): ya no vive en src/main/java (no se
 * empaqueta en el JAR distribuido), no tiene un secreto hardcodeado, y usa
 * JUnit en lugar de un metodo main() con System.out.println.
 */
public class AESEncrypterTest {

    private static String testSecretKey() {
        Map<String, String> env = System.getenv();
        String override = env.get("PLUSPAGOS_TEST_SECRET");
        return override != null ? override : "clave-de-prueba-no-usar-en-produccion";
    }

    @Test
    public void encryptThenDecrypt_returnsOriginalPlainText() {
        String plainText = "2300";
        String secretKey = testSecretKey();

        String cipherText = AESEncrypter.encryptString(plainText, secretKey);
        assertNotEquals(plainText, cipherText);

        String decrypted = AESEncrypter.decryptString(cipherText, secretKey);
        assertEquals(plainText, decrypted);
    }

    @Test
    public void encryptingTwice_producesDifferentCipherTexts() {
        // Salt e IV aleatorios en cada llamada: dos cifrados del mismo texto
        // con la misma clave deben producir criptogramas distintos.
        String plainText = "2300";
        String secretKey = testSecretKey();

        String first = AESEncrypter.encryptString(plainText, secretKey);
        String second = AESEncrypter.encryptString(plainText, secretKey);

        assertNotEquals(first, second);
    }
}
