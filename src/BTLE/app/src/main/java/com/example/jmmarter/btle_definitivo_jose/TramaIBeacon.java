package com.example.jmmarter.btle_definitivo_jose;

import java.util.Arrays;

// ==========================================================
// DISEÑO LÓGICO: clase TramaIBeacon
// ----------------------------------------------------------
//   bytes: [ Z ] --> TramaIBeacon() -->
//       getPrefijo()      <-- [ Z ]_9
//       getUUID()         <-- [ Z ]_16
//       getMajor()        <-- [ Z ]_2
//       getMinor()        <-- [ Z ]_2
//       getTxPower()      <-- Z
//       getLosBytes()     <-- [ Z ]
//       getAdvFlags()     <-- [ Z ]_3
//       getAdvHeader()    <-- [ Z ]_2
//       getCompanyID()    <-- [ Z ]_2
//       getiBeaconType()  <-- Z
//       getiBeaconLength()<-- Z
//
// Trocea los bytes crudos de un anuncio iBeacon (recibidos en el
// ScanRecord) en los campos que lo componen:
//   prefijo (9) = advFlags(3) + advHeader(2) + companyID(2)
//                 + iBeaconType(1) + iBeaconLength(1)
//   uuid(16) + major(2) + minor(2) + txPower(1)
// ==========================================================
public class TramaIBeacon {
    // Longitud mínima de un anuncio iBeacon: 9 + 16 + 2 + 2 + 1 = 30
    private static final int LONGITUD_MINIMA = 30;

    private byte[] prefijo = null; // 9 bytes
    private byte[] uuid = null; // 16 bytes
    private byte[] major = null; // 2 bytes
    private byte[] minor = null; // 2 bytes
    private byte txPower = 0; // 1 byte

    private byte[] losBytes;

    private byte[] advFlags = null; // 3 bytes
    private byte[] advHeader = null; // 2 bytes
    private byte[] companyID = new byte[2]; // 2 bytes
    private byte iBeaconType = 0 ; // 1 byte
    private byte iBeaconLength = 0 ; // 1 byte

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getPrefijo() <-- [ Z ]_9
    // -------------------------------------------------------------------------------
    public byte[] getPrefijo() {
        return prefijo;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getUUID() <-- [ Z ]_16
    // -------------------------------------------------------------------------------
    public byte[] getUUID() {
        return uuid;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getMajor() <-- [ Z ]_2
    // -------------------------------------------------------------------------------
    public byte[] getMajor() {
        return major;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getMinor() <-- [ Z ]_2
    // -------------------------------------------------------------------------------
    public byte[] getMinor() {
        return minor;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getTxPower() <-- Z
    // -------------------------------------------------------------------------------
    public byte getTxPower() {
        return txPower;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getLosBytes() <-- [ Z ]
    // -------------------------------------------------------------------------------
    public byte[] getLosBytes() {
        return losBytes;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getAdvFlags() <-- [ Z ]_3
    // -------------------------------------------------------------------------------
    public byte[] getAdvFlags() {
        return advFlags;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getAdvHeader() <-- [ Z ]_2
    // -------------------------------------------------------------------------------
    public byte[] getAdvHeader() {
        return advHeader;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getCompanyID() <-- [ Z ]_2
    // -------------------------------------------------------------------------------
    public byte[] getCompanyID() {
        return companyID;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getiBeaconType() <-- Z
    // -------------------------------------------------------------------------------
    public byte getiBeaconType() {
        return iBeaconType;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: --> getiBeaconLength() <-- Z
    // -------------------------------------------------------------------------------
    public byte getiBeaconLength() {
        return iBeaconLength;
    }

    // -------------------------------------------------------------------------------
    // DISEÑO: bytes: [ Z ] --> TramaIBeacon() -->
    // Qué hace: copia los bytes crudos y desempaqueta, por posiciones
    //           fijas, la estructura de un iBeacon.
    // -------------------------------------------------------------------------------
    public TramaIBeacon(byte[] bytes ) {
        if ( bytes == null ) {
            throw new IllegalArgumentException( "TramaIBeacon: bytes == null" );
        }
        if ( bytes.length < LONGITUD_MINIMA ) {
            throw new IllegalArgumentException(
                "TramaIBeacon: longitud insuficiente (" + bytes.length +
                " < " + LONGITUD_MINIMA + "). No es un iBeacon." );
        }

        this.losBytes = bytes;

        prefijo = Arrays.copyOfRange(losBytes, 0, 8+1 ); // 9 bytes
        uuid = Arrays.copyOfRange(losBytes, 9, 24+1 ); // 16 bytes
        major = Arrays.copyOfRange(losBytes, 25, 26+1 ); // 2 bytes
        minor = Arrays.copyOfRange(losBytes, 27, 28+1 ); // 2 bytes
        txPower = losBytes[ 29 ]; // 1 byte

        advFlags = Arrays.copyOfRange( prefijo, 0, 2+1 ); // 3 bytes
        advHeader = Arrays.copyOfRange( prefijo, 3, 4+1 ); // 2 bytes
        companyID = Arrays.copyOfRange( prefijo, 5, 6+1 ); // 2 bytes
        iBeaconType = prefijo[ 7 ]; // 1 byte
        iBeaconLength = prefijo[ 8 ]; // 1 byte

    } // ()
} // class
