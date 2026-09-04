package com.pluspagos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.pluspagos.AESEncrypter;
import com.pluspagos.PlusPagosCryptoException;
import com.pluspagos.SHA256Firm;


public class FormularioModel {

    private final String hash;
    private final String callbackSuccess;
    private final String callbackCancel;
    private final String comercio;
    private final String sucursalComercio;
    private final String transaccionComercioId;
    private final String monto;
    private final List<String> productos;
    private final String secretKey;
    private final String ip;

    private FormularioModel(Builder builder) {
        this.callbackSuccess = builder.callbackSuccess;
        this.callbackCancel = builder.callbackCancel;
        this.comercio = builder.comercio;
        this.sucursalComercio = builder.sucursalComercio;
        this.transaccionComercioId = builder.transaccionComercioId;
        this.monto = builder.montoCentavos == null ? null : builder.montoCentavos.toString();
        this.productos = Collections.unmodifiableList(new ArrayList<String>(builder.productos));
        this.secretKey = builder.secretKey;
        this.ip = builder.ip;

        if (builder.hash != null) {
            this.hash = builder.hash;
        } else {
            try {
                this.hash = new SHA256Firm().getFirm(ip, secretKey, comercio, sucursalComercio, monto);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new PlusPagosCryptoException("No se pudo generar la firma del formulario", e);
            }
        }
    }

    public static class Builder {
        private String hash;
        private String callbackSuccess;
        private String callbackCancel;
        private String comercio;
        private String sucursalComercio = "";
        private String transaccionComercioId;
        private Long montoCentavos;
        private final List<String> productos = new ArrayList<String>();
        private final String secretKey;
        private String ip;

        public Builder(String secretKey) {
            this.secretKey = secretKey;
        }

        public Builder hash(String hash) {
            this.hash = hash;
            return this;
        }

        public Builder callbackSuccess(String callbackSuccess) {
            this.callbackSuccess = callbackSuccess;
            return this;
        }

        public Builder callbackCancel(String callbackCancel) {
            this.callbackCancel = callbackCancel;
            return this;
        }

        public Builder comercio(String comercio) {
            this.comercio = comercio;
            return this;
        }

        public Builder sucursal(String sucursal) {
            this.sucursalComercio = sucursal;
            return this;
        }

        public Builder transaccionId(String transaccionId) {
            this.transaccionComercioId = transaccionId;
            return this;
        }

        /**
         * @deprecated El tipo {@code double} no representa con precision
         * montos financieros (IEEE 754). Usar {@link #monto(BigDecimal)} o
         * {@link #montoCentavos(long)}.
         */
        @Deprecated
        public Builder monto(double monto) {
            return monto(BigDecimal.valueOf(monto));
        }

        public Builder monto(BigDecimal monto) {
            this.montoCentavos = monto
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
            return this;
        }

        public Builder montoCentavos(long centavos) {
            this.montoCentavos = centavos;
            return this;
        }

        public Builder addProducto(String producto) {
            this.productos.add(producto);
            return this;
        }

        public Builder ip(String ip) {
            this.ip = ip.trim();
            return this;
        }

        public FormularioModel build() {
            return new FormularioModel(this);
        }
    }

    public byte[] toUrlEncodedForm() {
        char fieldSep = '&';
        char valueSep = '=';

        StringBuilder result = new StringBuilder();
        result.append("Hash").append(valueSep).append(urlEncode(hash)).append(fieldSep);
        result.append("TransaccionComercioId").append(valueSep).append(urlEncode(transaccionComercioId)).append(fieldSep);
        result.append("COMERCIO").append(valueSep).append(urlEncode(comercio)).append(fieldSep);

        for (int p = 0; p < productos.size(); p++) {
            result.append("Producto[").append(p).append("]").append(valueSep).append(urlEncode(productos.get(p))).append(fieldSep);
        }

        result.append("Monto").append(valueSep).append(urlEncode(AESEncrypter.encryptString(monto, secretKey))).append(fieldSep);
        result.append("SucursalComercio").append(valueSep).append(urlEncode(AESEncrypter.encryptString(sucursalComercio, secretKey))).append(fieldSep);
        result.append("CallbackCancel").append(valueSep).append(urlEncode(AESEncrypter.encryptString(callbackCancel, secretKey))).append(fieldSep);
        result.append("CallbackSuccess").append(valueSep).append(urlEncode(AESEncrypter.encryptString(callbackSuccess, secretKey)));

        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 no soportado", e);
        }
    }
}
