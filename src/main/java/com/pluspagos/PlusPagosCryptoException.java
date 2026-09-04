package com.pluspagos;

/**
 * Excepción no verificada (unchecked) que encapsula errores criptográficos
 * internos de la librería (fallas de cifrado/descifrado, HMAC o derivación de
 * clave).
 *
 * Se introduce para reemplazar el uso de {@code e.printStackTrace()} en los
 * bloques catch: en lugar de imprimir la traza (y con ella detalles internos
 * de clases, librerías y rutas del sistema) por la salida estándar, el error
 * se propaga como excepción para que el consumidor del SDK decida cómo
 * registrarlo o manejarlo (hallazgo COD-03 del informe de auditoría).
 */
public class PlusPagosCryptoException extends RuntimeException {

    public PlusPagosCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
