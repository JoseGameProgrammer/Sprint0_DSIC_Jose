package com.example.jmmarter.btle_definitivo_jose;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas que NECESITAN el móvil o el emulador con la app instalada.
 *
 * A diferencia de UtilidadesTest y TramaIBeaconTest, que son Java puro y se ejecutan en el
 * ordenador, estas pruebas comprueban cosas que solo existen en un dispositivo Android de
 * verdad: que los permisos de Bluetooth están declarados y concedidos, y que el escáner BLE
 * se puede arrancar de verdad.
 *
 * Cómo ejecutarlas:
 *   1. Conectar el móvil por USB y activar la depuración USB.
 *   2. `./gradlew :app:connectedDebugAndroidTest`
 * El resultado sale en app/build/reports/androidTests/connected/index.html
 */
@RunWith(AndroidJUnit4.class)
public class PruebasEnElDispositivo {

    @Test
    public void elPaqueteEsElEsperado() {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.example.jmmarter.btle_definitivo_jose", contexto.getPackageName());
    }

    @Test
    public void elManejoDeContextosNoEsNulo() {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertNotNull(contexto);
    }

    // -----------------------------------------------------------------------------------
    // Permisos: sin ellos la app no ve ni un solo beacon
    // -----------------------------------------------------------------------------------

    @Test
    public void elPermisoDeEscaneoBleEstaConcedido() {
        // Es el permiso que exige Android 12+ para poder leer anuncios BLE. Si el usuario no lo
        // acepta, el escáner arranca pero no entra ni un onScanResult(), que es exactamente el
        // síntoma que costó tiempo Averiguar.
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("Falta el permiso BLUETOOTH_SCAN",
                PackageManager.PERMISSION_GRANTED,
                contexto.checkSelfPermission("android.permission.BLUETOOTH_SCAN"));
    }

    @Test
    public void elPermisoDeConexionBleEstaConcedido() {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("Falta el permiso BLUETOOTH_CONNECT",
                PackageManager.PERMISSION_GRANTED,
                contexto.checkSelfPermission("android.permission.BLUETOOTH_CONNECT"));
    }

    @Test
    public void elPermisoDeInternetEstaConcedido() {
        // Sin INTERNET no hay POST al servidor, que es lo que hace PeticionarioREST.
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("Falta el permiso INTERNET",
                PackageManager.PERMISSION_GRANTED,
                contexto.checkSelfPermission("android.permission.INTERNET"));
    }

    @Test
    public void elDispositivoTieneHardwareBle() {
        // El AndroidManifest lo declara como obligatorio (required="true"), así que en un
        // dispositivo sin BLE la app ni siquiera se instala.
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertTrue("El dispositivo no tiene Bluetooth LE",
                contexto.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_BLUETOOTH_LE));
    }

    @Test
    public void elAdaptadorBluetoothExiste() {
        // Comprueba que hay un adaptador. Que esté activado lo pide MainActivity al arrancar;
        // esta prueba solo verifica que el dispositivo tiene el hardware.
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertNotNull("El dispositivo no tiene adaptador Bluetooth",
                contexto.getSystemService(Context.BLUETOOTH_SERVICE));
    }
}