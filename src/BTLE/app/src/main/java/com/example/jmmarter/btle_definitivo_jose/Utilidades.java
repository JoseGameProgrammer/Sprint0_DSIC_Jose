package com.example.jmmarter.btle_definitivo_jose;


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

// ==========================================================
// DISEÑO: clase Utilidades (métodos ESTÁTICOS)
// ----------------------------------------------------------
//   texto: Text --> stringToBytes() --x       [ Z ] <--
//   uuid:  Text --> stringToUUID()  --x       UUID  <--
//   uuid: UUID --> uuidToString()   --x       Text  <--
//   uuid: UUID --> uuidToHexString() --x      Text  <--
//   bytes: [Z] --> bytesToString()  --x       Text  <--
//   ms: Z, ls: Z --> dosLongToBytes() --x     [ Z ]_16 <--
//   bytes: [Z] --> bytesToInt()     --x       Z <--
//   bytes: [Z] --> bytesToLong()    --x       Z <--
//   bytes: [Z] --> bytesToIntOK()   --x       Z <--
//   bytes: [Z] --> bytesToHexString() --x     Text <--
//
// Conjunto de conversiones entre bytes, texto y UUID que usan el
// detector BTLE y la trama iBeacon. No tiene estado.
// ==========================================================
public class Utilidades {

    // -------------------------------------------------------------------------------
    // DISEÑO: texto: Text --> stringToBytes() --> [ Z ]
    // Qué hace: convierte un String en su array de bytes
    // -------------------------------------------------------------------------------
    public static byte[] stringToBytes ( String texto ) {
        return texto.getBytes();
    } // ()

    // -------------------------------------------------------------------------------
    // DISEÑO: uuid: Text --> stringToUUID() --> UUID
    // Qué hace: interpreta los 16 caracteres como 16 dígitos
    //           hexadecimales (8 para la parte alta y 8 para la baja)
    //           y construye el UUID de 128 bits.
    //
    // BUG CORREGIDO: pasaba los bytes ASCII de
    // los caracteres a bytesToLong(), con lo que el resultado no era
    // el UUID esperado. Ahora se decodifica hexadecimal de verdad.
    // -------------------------------------------------------------------------------
    public static UUID stringToUUID( String uuid ) {
        if ( uuid.length() != 16 ) {
            throw new Error( "stringUUID: string no tiene 16 caracteres ");
        }

        String masSignificativo = uuid.substring(0, 8);
        String menosSignificativo = uuid.substring(8, 16);

        long parteAlta;
        long parteBaja;
        try {
            parteAlta = Long.parseUnsignedLong( masSignificativo, 16 );
            parteBaja  = Long.parseUnsignedLong( menosSignificativo, 16 );
        } catch ( NumberFormatException e ) {
            throw new Error( "stringToUUID: el texto no es hexadecimal válido" );
        }

        return new UUID( parteAlta, parteBaja );
    } // ()

    // -------------------------------------------------------------------------------
    // DISEÑO: uuid: UUID --> uuidToString() --> Text
    // Qué hace: reconstruye el texto de 16 caracteres a partir de los
    //           dos long del UUID.
    // -------------------------------------------------------------------------------
    public static String uuidToString ( UUID uuid ) {
        return bytesToString( dosLongToBytes( uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ) );
    } // ()

    // -------------------------------------------------------------------------------
    // DISEÑO: uuid: UUID --> uuidToHexString() --> Text
    // Qué hace: igual que uuidToString() pero en representación
    //           hexadecimal con ':' entre bytes.
    // -------------------------------------------------------------------------------
    public static String uuidToHexString ( UUID uuid ) {
        return bytesToHexString( dosLongToBytes( uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() ) );
    } // ()

    // -------------------------------------------------------------------------------
    // DISEÑO: bytes: [ Z ] --> bytesToString() --> Text
    // Qué hace: interpreta cada byte como un carácter y los concatena.
    // -------------------------------------------------------------------------------
    public static String bytesToString( byte[] bytes ) {
        if (bytes == null ) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append( (char) b );
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: ms: Z, ls: Z --> dosLongToBytes() --> [ Z ]_16
    // Qué hace: empaqueta dos long (parte más y menos significativa)
    //           en un array de 16 bytes (el UUID en crudo).
    // -------------------------------------------------------------------------------
    public static byte[] dosLongToBytes( long masSignificativos, long menosSignificativos ) {
        ByteBuffer buffer = ByteBuffer.allocate( 2 * Long.BYTES );
        buffer.putLong( masSignificativos );
        buffer.putLong( menosSignificativos );
        return buffer.array();
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: bytes: [ Z ] --> bytesToInt() --> Z
    // Qué hace: convierte un array de bytes (en complemento a 2) a int.
    //           Interpreta el número CON signo.
    // -------------------------------------------------------------------------------
    public static int bytesToInt( byte[] bytes ) {
        return new BigInteger(bytes).intValue();
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: bytes: [ Z ] --> bytesToLong() --> Z
    // Qué hace: convierte un array de bytes (complemento a 2) a long.
    // -------------------------------------------------------------------------------
    public static long bytesToLong( byte[] bytes ) {
        return new BigInteger(bytes).longValue();
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: bytes: [ Z ] --> bytesToIntOK() --> Z
    // Qué hace: convierte de bytes a int de forma explícita, byte a
    //           byte, y aplica el signo del primer byte.
    // -------------------------------------------------------------------------------
    public static int bytesToIntOK( byte[] bytes ) {
        if (bytes == null ) {
            return 0;
        }

        if ( bytes.length > 4 ) {
            throw new Error( "demasiados bytes para pasar a int ");
        }
        int res = 0;

        for( byte b : bytes ) {
            res =  (res << 8) // * 16
                    + (b & 0xFF); // para quedarse con 1 byte (2 cuartetos) de lo que haya en b
        } // for

        if ( (bytes[ 0 ] & 0x80) != 0 ) {
            // si tiene signo negativo (un 1 a la izquierda del primer byte
            res = -(~(byte)res)-1; // complemento a 2 (~) de res pero como byte, -1
        }

        return res;
    } // ()

    // -------------------------------------------------------------------------------
    // DISEÑO: bytes: [ Z ] --> bytesToHexString() --> Text
    // Qué hace: devuelve los bytes como texto hexadecimal separados
    //           por ':' (útil para depuración por Log).
    // -------------------------------------------------------------------------------
    public static String bytesToHexString( byte[] bytes ) {

        if (bytes == null ) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
            sb.append(':');
        }
        return sb.toString();
    } // ()
} // class
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------

