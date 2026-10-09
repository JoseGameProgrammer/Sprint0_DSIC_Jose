package com.example.jmmarter.btle_definitivo_jose;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Pruebas de las conversiones entre bytes, texto y UUID.
 *
 * Estas pruebas NO necesitan el móvil: Utilidades es Java puro, así que se ejecutan en el
 * ordenador con `./gradlew :app:testDebugUnitTest`.
 *
 * El caso más importante es el del signo: la placa Arduino transmite la temperatura en el
 * campo "minor" del iBeacon, que según la norma es un entero de 16 bits SIN signo. Una
 * temperatura de -13 grados viaja como 0xFFF3 (65523 sin signo). Estas pruebas comprueban
 * que el móvil la vuelve a leer como -13, que es lo que acaba en la base de datos.
 */
public class UtilidadesTest {

    // -----------------------------------------------------------------------------------
    // stringToBytes()
    // -----------------------------------------------------------------------------------

    @Test
    public void stringToBytes_devuelveUnBytePorCaracter() {
        byte[] resultado = Utilidades.stringToBytes("abc");

        assertEquals(3, resultado.length);
        assertArrayEquals(new byte[]{97, 98, 99}, resultado);
    }

    @Test
    public void stringToBytes_textoVacioDevuelveCeroBytes() {
        assertEquals(0, Utilidades.stringToBytes("").length);
    }

    @Test
    public void stringToBytes_respetaLosEspacios() {
        // Un espacio es un byte normal, no se ignora: el nombre del emisor viaja con espacios.
        assertArrayEquals(new byte[]{32}, Utilidades.stringToBytes(" "));
    }

    // -----------------------------------------------------------------------------------
    // stringToUUID()
    // -----------------------------------------------------------------------------------

    @Test
    public void stringToUUID_reparteLos16CaracteresEn8Y8() {
        // OJO con las magnitudes: 16 caracteres hexadecimales son 16 bytes, es decir 128 bits,
        // que es justo un UUID entero... pero repartidos en dos mitades de 8 bytes (64 bits).
        // Por eso "0123456789abcdef" NO da un UUID con esos 16 bytes dentro: da un UUID cuyo
        // byte alto es 0x01234567 y cuyo byte bajo es 0x89abcdef, y los 64 bits superiores
        // del byte alto se quedan a cero.
        UUID resultado = Utilidades.stringToUUID("0123456789abcdef");

        assertEquals(0x01234567L, resultado.getMostSignificantBits());
        assertEquals(0x89abcdefL, resultado.getLeastSignificantBits());
    }

    @Test
    public void stringToUUID_los16CaracteresSon16DigitosYNo16Bytes() {
        // OJO, esta es la trampa fácil: 16 bytes en hexadecimal son 32 caracteres, no 16.
        // Si se pasan 32 caracteres, stringToUUID() avisa en vez de dar un UUID que no es el
        // que se cree.
        try {
            Utilidades.stringToUUID("0102030405060708090a0b0c0d0e0f10");
            fail("Debería lanzar Error: son 32 caracteres, no 16");
        } catch (Error e) {
            assertEquals("stringUUID: string no tiene 16 caracteres ", e.getMessage());
        }
    }

    @Test
    public void stringToUUID_aceptaLasMinusculasYLasMayusculas() {
        assertEquals(
                Utilidades.stringToUUID("ABCDEF0123456789"),
                Utilidades.stringToUUID("abcdef0123456789"));
    }

    @Test
    public void stringToUUID_todosLosCerosDaElUuidCero() {
        UUID resultado = Utilidades.stringToUUID("0000000000000000");

        assertEquals(0L, resultado.getMostSignificantBits());
        assertEquals(0L, resultado.getLeastSignificantBits());
    }

    @Test
    public void stringToUUID_rechazaUnTextoQueNoMide16() {
        try {
            Utilidades.stringToUUID("ABC");
            fail("Debería lanzar Error con un texto de menos de 16 caracteres");
        } catch (Error e) {
            assertEquals("stringUUID: string no tiene 16 caracteres ", e.getMessage());
        }
    }

    @Test
    public void stringToUUID_rechazaUnTextoNoHexadecimal() {
        // 16 caracteres de sobra, pero 'z' no es un dígito hexadecimal. Este es el fallo que
        // tenía antes: se pasaba el texto a bytes ASCII y salía un UUID que no era el bueno.
        try {
            Utilidades.stringToUUID("zzzzzzzzzzzzzzzz");
            fail("Debería lanzar Error con un texto no hexadecimal");
        } catch (Error e) {
            assertEquals("stringToUUID: el texto no es hexadecimal válido", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------
    // dosLongToBytes()
    // -----------------------------------------------------------------------------------

    @Test
    public void dosLongToBytes_devuelve16BytesEnOrdenDeAltoABajo() {
        byte[] resultado = Utilidades.dosLongToBytes(0x0102030405060708L, 0x090a0b0c0d0e0f10L);

        assertEquals(16, resultado.length);
        assertArrayEquals(new byte[]{
                0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10
        }, resultado);
    }

    @Test
    public void dosLongToBytes_cerosDaDieciseisCeros() {
        byte[] resultado = Utilidades.dosLongToBytes(0L, 0L);

        assertEquals(16, resultado.length);
        for (byte b : resultado) {
            assertEquals(0, b);
        }
    }

    // -----------------------------------------------------------------------------------
    // bytesToString()
    // -----------------------------------------------------------------------------------

    @Test
    public void bytesToString_cadaByteSeConvierteEnSuCaracter() {
        assertEquals("AB", Utilidades.bytesToString(new byte[]{0x41, 0x42}));
    }

    @Test
    public void bytesToString_nullDevuelveCadenaVacia() {
        assertEquals("", Utilidades.bytesToString(null));
    }

    @Test
    public void bytesToString_noInterpretaLosBytesComoTextoHexadecimal() {
        // OJO, esto es intencionado y conviene no olvidarlo: bytesToString() NO saca
        // "0a0b", saca los caracteres U+000A y U+000B, que no se ven. Para depurar hay que
        // usar bytesToHexString(), que MainActivity llama en los Logs precisamente por esto.
        String resultado = Utilidades.bytesToString(new byte[]{0x0a, 0x0b});

        assertEquals(2, resultado.length());
        assertEquals('\n', resultado.charAt(0));
        assertEquals(0x0b, resultado.charAt(1));
    }

    // -----------------------------------------------------------------------------------
    // bytesToHexString()
    // -----------------------------------------------------------------------------------

    @Test
    public void bytesToHexString_separadoPorDosPuntosYConDosDigitos() {
        assertEquals("00:0f:ff:",
                Utilidades.bytesToHexString(new byte[]{0x00, 0x0f, (byte) 0xff}));
    }

    @Test
    public void bytesToHexString_nullDevuelveCadenaVacia() {
        assertEquals("", Utilidades.bytesToHexString(null));
    }

    @Test
    public void bytesToHexString_arrayVacioDevuelveCadenaVacia() {
        assertEquals("", Utilidades.bytesToHexString(new byte[0]));
    }

    @Test
    public void uuidToHexString_sacaLos16BytesDelUuid() {
        assertEquals("01:02:03:04:05:06:07:08:09:0a:0b:0c:0d:0e:0f:10:",
                Utilidades.uuidToHexString(new UUID(0x0102030405060708L, 0x090a0b0c0d0e0f10L)));
    }

    // -----------------------------------------------------------------------------------
    // bytesToInt(): aquí está lo importante, el signo de la temperatura
    // -----------------------------------------------------------------------------------

    @Test
    public void bytesToInt_leeElMajorDelCo2() {
        // 0x0B01 = 2817 = (CO2 = 11) * 256 + 1, es decir "primera muestra de CO2".
        assertEquals(2817, Utilidades.bytesToInt(new byte[]{0x0b, 0x01}));
    }

    @Test
    public void bytesToInt_leeElMajorDeLaTemperatura() {
        // 0x0C01 = 3073 = (TEMPERATURA = 12) * 256 + 1.
        assertEquals(3073, Utilidades.bytesToInt(new byte[]{0x0c, 0x01}));
    }

    @Test
    public void bytesToInt_unaTemperaturaNegativaVuelveNegativa() {
        // -13 en complemento a dos de 16 bits es 0xFFF3, es decir 65523 si se lee SIN signo.
        // Si esta prueba falla, el móvil está guardando 65523 en lugar de -13.
        assertEquals(-13, Utilidades.bytesToInt(new byte[]{(byte) 0xff, (byte) 0xf3}));
    }

    @Test
    public void bytesToInt_elMenosNegativoDelRango() {
        assertEquals(-32768, Utilidades.bytesToInt(new byte[]{(byte) 0x80, (byte) 0x00}));
    }

    @Test
    public void bytesToInt_elMayorPositivoDelRango() {
        assertEquals(32767, Utilidades.bytesToInt(new byte[]{0x7f, (byte) 0xff}));
    }

    @Test
    public void bytesToInt_unSoloByteTambienEsComplementoADos() {
        // Un único byte 0xFF NO es 255: en complemento a dos vale -1. Es el comportamiento
        // correcto para bytesToInt(), que interpreta la secuencia entera como un número con
        // signo, y por eso los campos del iBeacon, que van sin signo, nunca dan negativo
        // cuando el primer byte tiene el bit alto a cero.
        assertEquals(-1, Utilidades.bytesToInt(new byte[]{(byte) 0xff}));
    }

    // -----------------------------------------------------------------------------------
    // bytesToIntOK(): la otra manera de hacerlo, byte a byte
    // -----------------------------------------------------------------------------------

    @Test
    public void bytesToIntOK_coincideConBytesToIntEnPositivo() {
        assertEquals(2817, Utilidades.bytesToIntOK(new byte[]{0x0b, 0x01}));
        assertEquals(3073, Utilidades.bytesToIntOK(new byte[]{0x0c, 0x01}));
    }

    @Test
    public void bytesToIntOK_interpretaElSignoCuandoLaMagnitudCabeEnUnByte() {
        // -1, -13 y -128: la parte baja cabe en un byte, y el signo sale bien.
        assertEquals(-1, Utilidades.bytesToIntOK(new byte[]{(byte) 0xff}));
        assertEquals(-13, Utilidades.bytesToIntOK(new byte[]{(byte) 0xff, (byte) 0xf3}));
        assertEquals(-128, Utilidades.bytesToIntOK(new byte[]{(byte) 0x80}));
    }

    // -----------------------------------------------------------------------------------
    // LIMITACIÓN CONOCIDA DE bytesToIntOK()
    //
    // bytesToIntOK() calcula el signo con un cast a byte, que se queda solo con los 8 bits
    // bajos. Mientras la magnitud cabe en un byte (de -1 a -128) acierta, pero a partir de
    // -129 se equivoca: -32768 sale 0.
    //
    // No afecta a este proyecto por dos razones: MainActivity no usa esta función, sino
    // bytesToInt(), que sí interpreta bien todo el rango de 16 bits; y las temperaturas del
    // enunciado van de -13 a -11, muy por encima de 128.
    // -----------------------------------------------------------------------------------

    @Test
    public void bytesToIntOK_limitaElSignoAUnByte() {
        // Esta prueba documenta el comportamiento REAL, no el deseado. Si algún día se
        // arregla esta función, hay que cambiar aquí el 0 por -32768.
        assertEquals(0, Utilidades.bytesToIntOK(new byte[]{(byte) 0x80, 0x00}));

        // Con bytesToInt(), que es la que usa la app, el mismo caso sale bien:
        assertEquals(-32768, Utilidades.bytesToInt(new byte[]{(byte) 0x80, 0x00}));
    }

    @Test
    public void bytesToIntOK_nullDevuelveCero() {
        assertEquals(0, Utilidades.bytesToIntOK(null));
    }

    @Test
    public void bytesToIntOK_rechazaMasDeCuatroBytes() {
        try {
            Utilidades.bytesToIntOK(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});
            fail("Debería lanzar Error con más de 4 bytes");
        } catch (Error e) {
            assertEquals("demasiados bytes para pasar a int ", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------
    // bytesToLong()
    // -----------------------------------------------------------------------------------

    @Test
    public void bytesToLong_leeUnNumeroDeOchoBytes() {
        byte[] bytes = Utilidades.dosLongToBytes(0L, 0x090a0b0c0d0e0f10L);

        assertEquals(0x090a0b0c0d0e0f10L, Utilidades.bytesToLong(bytes));
    }

    @Test
    public void bytesToLong_tambienInterpretaElSigno() {
        // -10 en complemento a dos de 64 bits son ocho bytes 0xFF y el último 0xF6.
        byte[] bytes = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf6};

        assertEquals(-10L, Utilidades.bytesToLong(bytes));
    }

    @Test
    public void bytesToLong_ochoBytesACerosValenCero() {
        assertEquals(0L, Utilidades.bytesToLong(Utilidades.dosLongToBytes(0L, 0L)));
    }
}