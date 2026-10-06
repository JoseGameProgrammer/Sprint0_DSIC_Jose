package com.example.jmmarter.btle_definitivo_jose;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Pruebas del troceado de un anuncio iBeacon de 30 bytes.
 *
 * TramaIBeacon es Java puro, así que estas pruebas se ejecutan en el ordenador con
 * `./gradlew :app:testDebugUnitTest`, sin necesidad del móvil.
 *
 * La trama que se usa aquí es exactamente la que monta EmisoraBLE en la placa Arduino
 * (ver EmisoraBLE.h, "Ejemplo de Beacon"), así que si el troceado se mueve en un sitio y
 * no en el otro, estas pruebas lo detectan.
 */
public class TramaIBeaconTest {

    /**
     * Monta un anuncio iBeacon de 30 bytes con la misma disposición que usa la placa.
     *
     *   prefijo  9 bytes = advFlags(3) + advHeader(2) + companyID(2) + type(1) + length(1)
     *   uuid    16 bytes
     *   major    2 bytes
     *   minor    2 bytes
     *   txPower  1 byte
     *
     * @param major valor del campo major, sin signo.
     * @param minor valor del campo minor, ya en complemento a dos si es negativo.
     */
    private static byte[] tramaDeEjemplo(int major, int minor) {
        return new byte[]{
        // prefijo
        0x02, 0x01, 0x06,       // advFlags
        0x1a, (byte) 0xff,       // advHeader
        0x4c, 0x00,             // companyID de Apple
        0x02,                   // iBeacon type
        0x15,                   // iBeacon length (21)
        // uuid
        'E', 'P', 'S', 'G', '-', 'G', 'T', 'I',
        '-', 'P', 'R', 'O', 'Y', '-', '3', 'A',
        // major
        (byte) ((major >> 8) & 0xFF), (byte) (major & 0xFF),
        // minor
        (byte) ((minor >> 8) & 0xFF), (byte) (minor & 0xFF),
        // txPower
        (byte) 0xca
    };
    }

    // -----------------------------------------------------------------------------------
    // El constructor
    // -----------------------------------------------------------------------------------

    @Test
    public void seConstruyeConUnaTramaDe30Bytes() {
        TramaIBeacon trama = new TramaIBeacon(tramaDeEjemplo(2817, 234));

        assertEquals(30, trama.getLosBytes().length);
    }

    @Test
    public void aceptaUnaTramaMasLargaDe30Bytes() {
        // El hardware puede añadir bytes de más. Si hay 30 o más, se admite.
        byte[] conRelleno = new byte[31];
        System.arraycopy(tramaDeEjemplo(2817, 234), 0, conRelleno, 0, 30);
        conRelleno[30] = 0x01;

        assertEquals(31, new TramaIBeacon(conRelleno).getLosBytes().length);
    }

    @Test
    public void rechazaNull() {
        try {
            new TramaIBeacon(null);
            fail("Debería lanzar IllegalArgumentException con null");
        } catch (IllegalArgumentException e) {
            assertEquals("TramaIBeacon: bytes == null", e.getMessage());
        }
    }

    @Test
    public void rechazaUnaTramaDemasiadoCorta() {
        // Menos de 30 bytes no puede ser un iBeacon: no hay ni uuid completo.
        try {
            new TramaIBeacon(new byte[29]);
            fail("Debería lanzar IllegalArgumentException con menos de 30 bytes");
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "TramaIBeacon: longitud insuficiente (29 < 30). No es un iBeacon.",
                    e.getMessage());
        }
    }

    @Test
    public void rechazaUnaTramaVacia() {
        try {
            new TramaIBeacon(new byte[0]);
            fail("Debería lanzar IllegalArgumentException con 0 bytes");
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "TramaIBeacon: longitud insuficiente (0 < 30). No es un iBeacon.",
                    e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------
    // Los trozos del prefijo
    // -----------------------------------------------------------------------------------

    @Test
    public void elPrefijoSonLosNuevePrimerosBytes() {
        byte[] expected = {0x02, 0x01, 0x06, 0x1a, (byte) 0xff, 0x4c, 0x00, 0x02, 0x15};

        assertArrayEquals(expected, new TramaIBeacon(tramaDeEjemplo(2817, 234)).getPrefijo());
    }

    @Test
    public void losAdvFlagsSonLosTresPrimerosDelPrefijo() {
        byte[] expected = {0x02, 0x01, 0x06};

        assertArrayEquals(expected,
                new TramaIBeacon(tramaDeEjemplo(2817, 234)).getAdvFlags());
    }

    @Test
    public void elAdvHeaderEmpiezaEnLaPosicion3DelPrefijo() {
        byte[] expected = {0x1a, (byte) 0xff};

        assertArrayEquals(expected,
                new TramaIBeacon(tramaDeEjemplo(2817, 234)).getAdvHeader());
    }

    @Test
    public void elCompanyIdEmpiezaEnLaPosicion5DelPrefijo() {
        byte[] expected = {0x4c, 0x00};

        assertArrayEquals(expected,
                new TramaIBeacon(tramaDeEjemplo(2817, 234)).getCompanyID());
    }

    @Test
    public void elCompanyIdEsElDeApple() {
        // 0x4C es el identificador de Bluetooth SIG de Apple, la razón de que los iPhone
        // reconozcan el anuncio. Si este byte se mueve, el móvil deja de tratarlo como iBeacon.
        byte[] companyId = new TramaIBeacon(tramaDeEjemplo(2817, 234)).getCompanyID();

        assertEquals(0x4c, companyId[0] & 0xFF);
        assertEquals(0x00, companyId[1] & 0xFF);
    }

    @Test
    public void elIBeaconTypeEsLaPosicion7DelPrefijo() {
        assertEquals(0x02, new TramaIBeacon(tramaDeEjemplo(2817, 234)).getiBeaconType() & 0xFF);
    }

    @Test
    public void elIBeaconLengthEsLaPosicion8DelPrefijo() {
        // 0x15 = 21, que es lo que viene detrás: 16 del uuid + 2 del major + 2 del minor
        // + 1 del txPower.
        assertEquals(0x15, new TramaIBeacon(tramaDeEjemplo(2817, 234)).getiBeaconLength() & 0xFF);
    }

    @Test
    public void elIBeaconLengthCoincideConLoQueVieneDetras() {
        TramaIBeacon trama = new TramaIBeacon(tramaDeEjemplo(2817, 234));

        assertEquals(trama.getUUID().length + trama.getMajor().length
                        + trama.getMinor().length + 1,
                trama.getiBeaconLength() & 0xFF);
    }

    // -----------------------------------------------------------------------------------
    // Los campos de la medición
    // -----------------------------------------------------------------------------------

    @Test
    public void elUuidSonLos16BytesDelMedio() {
        byte[] expected = {
                'E', 'P', 'S', 'G', '-', 'G', 'T', 'I',
                '-', 'P', 'R', 'O', 'Y', '-', '3', 'A'
        };

        assertArrayEquals(expected, new TramaIBeacon(tramaDeEjemplo(2817, 234)).getUUID());
    }

    @Test
    public void elMajorEmpiezaEnLaPosicion25() {
        byte[] expected = {0x0b, 0x01};

        assertArrayEquals(expected, new TramaIBeacon(tramaDeEjemplo(2817, 234)).getMajor());
    }

    @Test
    public void elMinorEmpiezaEnLaPosicion27() {
        byte[] expected = {0x00, (byte) 0xea};  // 234

        assertArrayEquals(expected, new TramaIBeacon(tramaDeEjemplo(2817, 234)).getMinor());
    }

    @Test
    public void elMinorNegativoSeGuardaEnComplementoADos() {
        // -13 grados se transmiten como 0xFFF3. La trama guarda los bytes tal cual; el signo
        // lo pone Utilidades.bytesToInt() al leerlos.
        byte[] expected = {(byte) 0xff, (byte) 0xf3};

        assertArrayEquals(expected,
                new TramaIBeacon(tramaDeEjemplo(3073, 0xfff3)).getMinor());
    }

    @Test
    public void elTxPowerEsElUltimoByte() {
        assertEquals(0xca, new TramaIBeacon(tramaDeEjemplo(2817, 234)).getTxPower() & 0xFF);
    }

    // -----------------------------------------------------------------------------------
    // El contrato entero entre la placa y el móvil
    // -----------------------------------------------------------------------------------

    @Test
    public void elMajorDaElTipoDeMedicionYElNumeroDeMuestra() {
        TramaIBeacon trama = new TramaIBeacon(tramaDeEjemplo(2817, 234));

        int major = Utilidades.bytesToInt(trama.getMajor());

        assertEquals(11, major / 256);   // CO2
        assertEquals(1, major % 256);    // primera muestra
    }

    @Test
    public void laTemperaturaSaleConSignoDelMinor() {
        // Esta es la prueba que protege el valor que acaba en la base de datos.
        TramaIBeacon trama = new TramaIBeacon(tramaDeEjemplo(3073, 0xfff3));

        int major = Utilidades.bytesToInt(trama.getMajor());
        int minor = Utilidades.bytesToInt(trama.getMinor());

        assertEquals(12, major / 256);   // TEMPERATURA
        assertEquals(-13, minor);
    }
}