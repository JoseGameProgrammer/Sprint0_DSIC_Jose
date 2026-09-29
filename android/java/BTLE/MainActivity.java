package org.jordi.prueba2025;

import android.bluetooth.le.ScanSettings;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;

import android.os.Bundle;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

// ==========================================================
// DISEÑO: MainActivity (detector BTLE)
// ----------------------------------------------------------
//   onCreate() -->
//       inicializarBlueTooth() -->
//
//   botonBuscarDispositivosBTLEPulsado(v) -->
//       buscarTodosLosDispositivosBTLE() -->
//
//   botonBuscarNuestroDispositivoBTLEPulsado(v) -->
//       buscarEsteDispositivoBTLE(nombre) -->
//
//   botonDetenerBusquedaDispositivosBTLEPulsado(v) -->
//       detenerBusquedaDispositivosBTLE() -->
//
//   callbackDelEscaneo.onScanResult() -->
//       mostrarInformacionDispositivoBTLE(resultado) -->
//           --> TramaIBeacon() + Utilidades.xxx()
//
// Escanea anuncios BLE, los muestra
// por Log y desempaqueta los que son iBeacon.
// ==========================================================
public class MainActivity extends AppCompatActivity {

    private static final String ETIQUETA_LOG = ">>>>"; // Etiqueta Log

    private static final int CODIGO_PETICION_PERMISOS = 11223344; // Código con el que se identifica la petición de permisos

    // Escáner BTLE del sistema
    private BluetoothLeScanner elEscanner;
    // callback activo (null si no se está escaneando)
    private ScanCallback callbackDelEscaneo = null;

    // --------------------------------------------------------------
    // DISEÑO: --> buscarTodosLosDispositivosBTLE() -->
    // Qué hace: callback que reporta TODOS los anuncios
    //           BTLE y arranca el escaneo (previa comprobación de
    //           permisos)
    //
    // MEJORA: antes de arrancar un escaneo se detiene el anterior
    // para no dejar escaneos solapados.
    // --------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empieza ");

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): instalamos scan callback ");

        // Aquí el callback que recibirá los resultados del escaneo
        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanResult() ");

                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onBatchScanResults() ");

            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanFailed() ");
            }
        };

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empezamos a escanear ");
        
        // Comprobamos permisos y si no tenemos los pedimos
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): no tengo permiso, lo pido");
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT},
                    CODIGO_PETICION_PERMISOS);
            return;
        }

        // Por si ya estaba escaneando que no se acumule y pete
        // Lo paramos y lo activamos
        detenerBusquedaDispositivosBTLE();

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): startScan ");
        this.elEscanner.startScan(this.callbackDelEscaneo);

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): termina ");
    } // ()

    // --------------------------------------------------------------
    // DISEÑO: resultado: ScanResult --> mostrarInformacionDispositivoBTLE() -->
    // Qué hace: vuelca por Log la info del dispositivo y, si el
    //           anuncio es un iBeacon, desempaqueta sus campos.
    //
    // se captura IllegalArgumentException de TramaIBeacon
    // para que un anuncio BLE que no sea iBeacon no tumbe la app.
    // --------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {

        Log.d(ETIQUETA_LOG, " mostrarInformacionDispositivoBTLLE(): empieza ");

        BluetoothDevice bluetoothDevice = resultado.getDevice();
        byte[] bytes = resultado.getScanRecord().getBytes();
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, "                                                     ");
        Log.d(ETIQUETA_LOG, " ****************************************************");
        Log.d(ETIQUETA_LOG, " ****** DISPOSITIVO DETECTADO BTLE ****************** ");
        Log.d(ETIQUETA_LOG, " ****************************************************");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " mostrarInformacionDispositivoBTLLE(): no tengo el permiso BLUETOOTH_CONNECT ");
            return;
        }
        Log.d(ETIQUETA_LOG, " nombre = " + bluetoothDevice.getName());
        Log.d(ETIQUETA_LOG, " toString = " + bluetoothDevice.toString());
        Log.d(ETIQUETA_LOG, " dirección = " + bluetoothDevice.getAddress());
        Log.d(ETIQUETA_LOG, " rssi = " + rssi);

        Log.d(ETIQUETA_LOG, " bytes (" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));

        // Aqui se previene el error de que no sea un beacon
        TramaIBeacon tib;
        try {
            tib = new TramaIBeacon(bytes);
        } catch (IllegalArgumentException e) {
            Log.d(ETIQUETA_LOG, " no es un iBeacon (" + e.getMessage() + ")");
            return;
        }

        Log.d(ETIQUETA_LOG, " ----------------------------------------------------");
        Log.d(ETIQUETA_LOG, " prefijo  = " + Utilidades.bytesToHexString(tib.getPrefijo()));
        Log.d(ETIQUETA_LOG, "          advFlags = " + Utilidades.bytesToHexString(tib.getAdvFlags()));
        Log.d(ETIQUETA_LOG, "          advHeader = " + Utilidades.bytesToHexString(tib.getAdvHeader()));
        Log.d(ETIQUETA_LOG, "          companyID = " + Utilidades.bytesToHexString(tib.getCompanyID()));
        Log.d(ETIQUETA_LOG, "          iBeacon type = " + Integer.toHexString(tib.getiBeaconType()));
        Log.d(ETIQUETA_LOG, "          iBeacon length 0x = " + Integer.toHexString(tib.getiBeaconLength()) + " ( "
                + tib.getiBeaconLength() + " ) ");
        Log.d(ETIQUETA_LOG, " uuid  = " + Utilidades.bytesToHexString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, " uuid  = " + Utilidades.bytesToString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, " major  = " + Utilidades.bytesToHexString(tib.getMajor()) + "( "
                + Utilidades.bytesToInt(tib.getMajor()) + " ) ");
        Log.d(ETIQUETA_LOG, " minor  = " + Utilidades.bytesToHexString(tib.getMinor()) + "( "
                + Utilidades.bytesToInt(tib.getMinor()) + " ) ");
        Log.d(ETIQUETA_LOG, " txPower  = " + Integer.toHexString(tib.getTxPower()) + " ( " + tib.getTxPower() + " )");
        Log.d(ETIQUETA_LOG, " ****************************************************");

    } // ()

    // --------------------------------------------------------------
    // DISEÑO: dispositivoBuscado: Text --> buscarEsteDispositivoBTLE() -->
    // Qué hace: un callback, arma un filtro por nombre de
    //           dispositivo y arranca el escaneo con máxima latencia.
    //
    // BUG CORREGIDO: se llamaba a startScan() DOS veces (una sin
    // filtros y otra con filtros), dejando dos escaneos a la vez.
    // Ahora solo se hace la llamada con filtros y ajustes.
    // --------------------------------------------------------------
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): empieza ");
        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): buscando " + dispositivoBuscado);
        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): instalamos scan callback ");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onScanResult() ");

                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onBatchScanResults() ");

            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onScanFailed() ");
            }
        };

        ScanFilter sf = new ScanFilter.Builder().setDeviceName(dispositivoBuscado).build();

        Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): empezamos a escanear buscando: " + dispositivoBuscado);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): no tengo permiso, lo pido");
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT},
                    CODIGO_PETICION_PERMISOS);
            return;
        }

        // evita tener dos escaneos activos a la vez.
        detenerBusquedaDispositivosBTLE();

        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): empieza scan");

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        ArrayList<ScanFilter> filtros = new ArrayList<ScanFilter>();
        filtros.add( sf );

        this.elEscanner.startScan( filtros, settings, this.callbackDelEscaneo);

        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): termina");
    } // ()

    // --------------------------------------------------------------
    // DISEÑO: --> detenerBusquedaDispositivosBTLE() -->
    // Qué hace: para el escaneo si hay uno activo y limpia el callback.
    // --------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): empieza");
        //COMPROBAMOS que haya un escaner activo
        if (this.callbackDelEscaneo == null) {
            Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): termina (no había escaneo)");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): no tengo permiso BLUETOOTH_SCAN");
            return;
        }

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): parando scan");
        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null;

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): termina");
    } // ()







    // --------------------------------------------------------------BOTONES--------------------------------------------------
    // --------------------------------------------------------------
    // DISEÑO: v: View --> botonBuscarDispositivosBTLEPulsado() -->
    // Qué hace: manejador del botón "buscar todos".
    // --------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton buscar dispositivos BTLE Pulsado");
        this.buscarTodosLosDispositivosBTLE();
    } // ()

    // --------------------------------------------------------------
    // DISEÑO: v: View --> botonBuscarNuestroDispositivoBTLEPulsado() -->
    // Qué hace: manejador del botón "buscar nuestro";
    // --------------------------------------------------------------
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton nuestro dispositivo BTLE Pulsado");

        this.buscarEsteDispositivoBTLE("GTI3A-2025" );

    } // ()

    // --------------------------------------------------------------
    // DISEÑO: v: View --> botonDetenerBusquedaDispositivosBTLEPulsado() -->
    // Qué hace: manejador del botón "detener".
    // --------------------------------------------------------------
    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton detener busqueda dispositivos BTLE Pulsado");
        this.detenerBusquedaDispositivosBTLE();
    } // ()
    // --------------------------------------------------------------BOTONES--------------------------------------------------








    // --------------------------------------------------------------
    // DISEÑO: --> inicializarBlueTooth() -->
    // Qué hace: obtiene el adaptador BT, lo enciende, obtiene el
    //           escáner BTLE y pide los permisos necesarios.
    // --------------------------------------------------------------
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): empieza ");
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos adaptador BT ");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): habilitamos adaptador BT ");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): no tengo permiso BLUETOOTH_CONNECT, lo pido");
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    CODIGO_PETICION_PERMISOS);
        } else {
            bta.enable();
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): habilitado =  " + bta.isEnabled() );
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): estado =  " + bta.getState() );
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos escaner btle ");

        this.elEscanner = bta.getBluetoothLeScanner();

        if ( this.elEscanner == null ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): Socorro: NO hemos obtenido escaner btle  !!!!");

        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): pidiendo permisos si faltan" );

        // solo se pide si no están concedidos ya.
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {

            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    CODIGO_PETICION_PERMISOS);

            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): permisos pedidos" );
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): ya tengo los permisos necesarios");
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): TERMINA " );
    } // ()

    // --------------------------------------------------------------
    // DISEÑO: savedInstanceState: Bundle --> onCreate() -->
    // Qué hace: punto de entrada de la Activity; monta el layout e
    //           inicializa Bluetooth.
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, " onCreate(): empieza ");

        inicializarBlueTooth();

        Log.d(ETIQUETA_LOG, " onCreate(): termina ");

    } // onCreate()

    // --------------------------------------------------------------
    // DISEÑO: requestCode: N, permissions: [Text], grantResults: [Z]
    //                                  --> onRequestPermissionsResult() -->
    // Qué hace: recoge el resultado de la petición de permisos.
    // --------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults);

        Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): EMPIEZA " );

        switch (requestCode) {
            case CODIGO_PETICION_PERMISOS:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permisos concedidos  !!!!");
                }  else {

                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): Socorro: permisos NO concedidos  !!!!");

                }
                return;
        }
    } // ()



} // class
