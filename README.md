# PlusPagosFirm-Java

Librería Java/Android para integrarse con el formulario de pago de PlusPagos: arma los campos cifrados y la firma (`Hash`) que exige el gateway, sin que el comercio tenga que implementar la criptografía a mano.

## Uso

```java
String secretKey = "secret";          // provisto por PlusPagos
String guidComercio = "000000-0000-0000-00000000"; // provisto por PlusPagos
String ip = ...; // IP pública del cliente

FormularioModel model = new FormularioModel.Builder(secretKey)
        .ip(ip)
        .comercio(guidComercio)
        .callbackSuccess("http://www.google.com")
        .callbackCancel("http://www.bing.com")
        .transaccionId(UUID.randomUUID().toString()) // debe ser único por transacción
        .sucursal("1") // opcional; si no se especifica, se envía "" por defecto
        .monto(new BigDecimal("120.47")) // monto real, con precisión decimal exacta
        .addProducto("Producto 1") // llamar una vez por cada producto
        .build();

byte[] body = model.toUrlEncodedForm();
```

`toUrlEncodedForm()` devuelve el body ya armado (campos cifrados y `Hash` incluidos) listo para enviarlo por POST a PlusPagos — por ejemplo, en Android, vía `WebView.postUrl(url, body)`.

Si en cambio necesitás generar el `Hash` o cifrar campos puntuales a mano (sin pasar por `FormularioModel`), podés usar directamente:

```java
String hash = new SHA256Firm().getFirm(ipClient, secretKey, guidComercio, sucursalId, monto);
String montoCifrado = AESEncrypter.encryptString(montoTransaccion, secretKey);
```

Ver el manual de integración de PlusPagos para el detalle completo de cada campo del formulario y el flujo end-to-end.

## Requisitos

- Java 8 o superior (target elegido por compatibilidad con versiones de Android más antiguas).

## Changelog

Ver el manual de integración de PlusPagos para el historial completo de cambios de la integración.
