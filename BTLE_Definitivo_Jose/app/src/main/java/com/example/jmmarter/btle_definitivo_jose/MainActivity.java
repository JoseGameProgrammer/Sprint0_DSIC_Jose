package com.example.jmmarter.btle_definitivo_jose;

import android.bluetooth.le.ScanSettings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanRecord;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

// ==================================================================================================
// DISEÑO LÓGICO: MainActivity (detector BTLE + servicio + pruebas REST)
// --------------------------------------------------------------------------------------------------
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
//   botonArrancarServicioPulsado(v) --> startService() -->
//   botonDetenerServicioPulsado(v)  --> stopService() -->
//   botonTestEnviarMedidaPulsado(v)  --> probarEnviarMedidaAlServidor() -->
//   botonPruebaPOSTPulsado(v)        --> probarEnviarPOST() -->
//   botonPruebaGETPulsado(v)         --> probarEnviarGET() -->
//   botonPruebaGET2Pulsado(v)        --> probarEnviarGET_Otra() -->
//
// ÚNICA Activity de la aplicación. Antes esto estaba repartido en tres clases distintas
// que se llamaban todas "MainActivity" (MainActivity, MainActivityREST y
// MainActivityServicio), en tres paquetes distintos. Aquí están TODAS juntas:
//
//   - Escanear beacons BTLE y desempaquetarlos si son iBeacon.
//   - Arrancar / parar el ServicioEscucharBeacons.
//   - Lanzar el test automático de envío de medidas al servidor REST.
//   - Las pruebas del cliente REST (MainActivityREST).
//
// Escanea anuncios BLE, los muestra por Log y desempaqueta los que son iBeacon.
// ==================================================================================================
public class MainActivity extends AppCompatActivity {

    private static final String ETIQUETA_LOG = ">>>>"; // Etiqueta Log

    private static final String ETIQUETA_LOG_REST = "pruebasPeticionario"; // Etiqueta Log de las pruebas REST

    private static final int CODIGO_PETICION_PERMISOS = 11223344; // Código con el que se identifica la petición de permisos

    // =============================================================================================
    //  TODO(Jose) - NOMBRE DEL DISPOSITIVO QUE BUSCAMOS
    // =============================================================================================
    //  Este es el nombre BLE (el que va en el "advertising name" del Arduino nRF52) que
    //  pone en Publicador.h al arrancar la emisora. OJO: antes aquí ponía "GTI3A-2025",
    //  que NO era el mismo que emitía el Arduino ("GTI-3A-Jose"), así que el botón
    //  "buscar nuestro dispositivo" no encontraba nunca nada.
    // =============================================================================================
    private static final String NOMBRE_NUESTRO_DISPOSITIVO_BTLE = "GTI-3A-Jose";

    // ---------------------------------------------------------------------------------------------
    // Escáner BTLE del sistema
    // ---------------------------------------------------------------------------------------------
    private BluetoothLeScanner elEscanner;
    // ---------------------------------------------------------------------------------------------
    // callback activo (null si no se está escaneando)
    // ---------------------------------------------------------------------------------------------
    private ScanCallback callbackDelEscaneo = null;

    // ---------------------------------------------------------------------------------------------
    // Intent con el que se arrancó el servicio (null si el servicio está parado).
    // ---------------------------------------------------------------------------------------------
    private Intent elIntentDelServicio = null;

    // ---------------------------------------------------------------------------------------------
    // Vistas de la interfaz.
    // ---------------------------------------------------------------------------------------------
    private TextView laEtiquetaEstado;
    private TextView laEtiquetaUltimaMedicion;

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> buscarTodosLosDispositivosBTLE() -->
    // Qué hace: callback que reporta TODOS los anuncios
    //           BTLE y arranca el escaneo (previa comprobación de
    //           permisos)
    //
    // ORDEN IMPORTANTE: primero se para el escaneo ANTERIOR y LUEGO se crea el
    // callback nuevo. Si se hace al revés, detenerBusquedaDispositivosBTLE()
    // pondría callbackDelEscaneo a null y startScan() reventaría con
    // "IllegalArgumentException: callback is null".
    // ---------------------------------------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empieza ");

        // Por si ya estaba escaneando que no se acumule y pete.
        // Esto para el escaneo VIEJO (con su callback viejo) y pone el campo a null.
        this.detenerBusquedaDispositivosBTLE();

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empezamos a escanear ");

        // Comprobamos permisos y si no tenemos los pedimos
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): no tengo permiso, lo pido");
            this.actualizarEstado("Falta el permiso de escaneo BTLE.");
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT},
                    CODIGO_PETICION_PERMISOS);
            return;
        }

        // Si no hay escáner es que el Bluetooth está apagado o faltan permisos
        // (inicializarBlueTooth() lo deja a null en ese caso). No se puede escanear.
        if (this.elEscanner == null) {
            Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): NO hay escaner BTLE, no puedo escanear");
            this.actualizarEstado("No hay escáner BTLE: enciende el Bluetooth y acepta los permisos.");
            return;
        }

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): instalamos scan callback ");

        // Aquí el callback que recibirá los resultados del escaneo.
        // Se crea DESPUÉS de parar el anterior, para que no se lo lleve por delante
        // detenerBusquedaDispositivosBTLE().
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

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): startScan ");
        this.elEscanner.startScan(this.callbackDelEscaneo);

        this.actualizarEstado("Escaneando TODOS los dispositivos BTLE...");
        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): termina ");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: resultado: ScanResult --> mostrarInformacionDispositivoBTLE() -->
    // Qué hace: vuelca por Log la info del dispositivo y, si el
    //           anuncio es un iBeacon, desempaqueta sus campos.
    //
    // se captura IllegalArgumentException de TramaIBeacon
    // para que un anuncio BLE que no sea iBeacon no tumbe la app.
    // ---------------------------------------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {

        Log.d(ETIQUETA_LOG, " mostrarInformacionDispositivoBTLLE(): empieza ");

        BluetoothDevice bluetoothDevice = resultado.getDevice();

        // BUG CORREGIDO: getScanRecord() puede devolver null (hay anuncios sin
        // record de escaneo), y getBytes() sobre null es un NullPointerException.
        ScanRecord elRecord = resultado.getScanRecord();
        if (elRecord == null) {
            Log.d(ETIQUETA_LOG, " mostrarInformacionDispositivoBTLLE(): el anuncio NO tiene ScanRecord, me lo salto");
            return;
        }

        byte[] bytes = elRecord.getBytes();
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

        // Muestra en pantalla la medida que acaba de llegar, que antes sólo iba al Log.
        String textoMedida = "última medida: " + bluetoothDevice.getName()
                + " major=" + Utilidades.bytesToInt(tib.getMajor())
                + " minor=" + Utilidades.bytesToInt(tib.getMinor())
                + " txPower=" + tib.getTxPower()
                + " rssi=" + rssi;

        this.actualizarUltimaMedicion(textoMedida);

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: dispositivoBuscado: Text --> buscarEsteDispositivoBTLE() -->
    // Qué hace: un callback, arma un filtro por nombre de
    //           dispositivo y arranca el escaneo con máxima latencia.
    //
    // BUG CORREGIDO (1): se llamaba a startScan() DOS veces (una sin
    // filtros y otra con filtros), dejando dos escaneos a la vez.
    // Ahora solo se hace la llamada con filtros y ajustes.
    //
    // BUG CORREGIDO (2): se creaba el callback nuevo, luego se llamaba a
    // detenerBusquedaDispositivosBTLE() (que pone callbackDelEscaneo a null)
    // y después se pasaba ese null a startScan():
    //   java.lang.IllegalArgumentException: callback is null
    // Ahora se para el escaneo anterior PRIMERO y el callback nuevo se crea después.
    // ---------------------------------------------------------------------------------------------
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): empieza ");
        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): buscando " + dispositivoBuscado);

        // Evita tener dos escaneos activos a la vez. Va ANTES de crear el callback
        // nuevo, porque esto lo pone a null.
        this.detenerBusquedaDispositivosBTLE();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): no tengo permiso, lo pido");
            this.actualizarEstado("Falta el permiso de escaneo BTLE.");
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT},
                    CODIGO_PETICION_PERMISOS);
            return;
        }

        // Si no hay escáner es que el Bluetooth está apagado o faltan permisos
        // (inicializarBlueTooth() lo deja a null en ese caso). No se puede escanear.
        if (this.elEscanner == null) {
            Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): NO hay escaner BTLE, no puedo escanear");
            this.actualizarEstado("No hay escáner BTLE: enciende el Bluetooth y acepta los permisos.");
            return;
        }

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

        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): empieza scan");

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        ArrayList<ScanFilter> filtros = new ArrayList<ScanFilter>();
        filtros.add(sf);

        this.elEscanner.startScan(filtros, settings, this.callbackDelEscaneo);

        this.actualizarEstado("Escaneando sólo " + dispositivoBuscado + "...");

        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): termina");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> detenerBusquedaDispositivosBTLE() -->
    // Qué hace: para el escaneo si hay uno activo y limpia el callback.
    // ---------------------------------------------------------------------------------------------
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

        // Si el escáner es null no hay nada que parar (Bluetooth apagado o sin permiso).
        if (this.elEscanner == null) {
            Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): no hay escaner BTLE, no hay nada que parar");
            this.callbackDelEscaneo = null;
            return;
        }

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): parando scan");
        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null;

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): termina");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: texto: Text --> actualizarEstado() -->
    // Qué hace: pone un texto en la etiqueta de estado de la pantalla.
    //
    //         (--x en el diagrama: no depende del estado... aquí sí, del MainActivity,
    //          pero la firma lógica es "un texto entra, la pantalla queda como estaba".
    //          Se documenta con la misma notación que el resto.)
    // ---------------------------------------------------------------------------------------------
    private void actualizarEstado(String texto) {
        if (this.laEtiquetaEstado != null) {
            this.laEtiquetaEstado.setText(texto);
        }
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: texto: Text --> actualizarUltimaMedicion() -->
    // Qué hace: pone un texto en la etiqueta de última medida.
    // ---------------------------------------------------------------------------------------------
    private void actualizarUltimaMedicion(String texto) {
        if (this.laEtiquetaUltimaMedicion != null) {
            this.laEtiquetaUltimaMedicion.setText(texto);
        }
    } // ()

    // --------------------------------------------------------------BOTONES--------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonBuscarDispositivosBTLEPulsado() -->
    // Qué hace: manejador del botón "buscar todos".
    // ---------------------------------------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton buscar dispositivos BTLE Pulsado");
        this.buscarTodosLosDispositivosBTLE();
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonBuscarNuestroDispositivoBTLEPulsado() -->
    // Qué hace: manejador del botón "buscar nuestro".
    // ---------------------------------------------------------------------------------------------
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton nuestro dispositivo BTLE Pulsado");

        this.buscarEsteDispositivoBTLE(NOMBRE_NUESTRO_DISPOSITIVO_BTLE);

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonDetenerBusquedaDispositivosBTLEPulsado() -->
    // Qué hace: manejador del botón "detener".
    // ---------------------------------------------------------------------------------------------
    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton detener busqueda dispositivos BTLE Pulsado");
        this.detenerBusquedaDispositivosBTLE();
        this.actualizarEstado("Escaneo detenido.");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonArrancarServicioPulsado() -->
    // Qué hace: si no está arrancado, crea el Intent con el tiempo de
    //           espera y arranca el servicio.
    //
    //           (viene de la antigua MainActivityServicio)
    // ---------------------------------------------------------------------------------------------
    public void botonArrancarServicioPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton arrancar servicio Pulsado");

        if (this.elIntentDelServicio != null) {
            // ya estaba arrancado
            return;
        }

        Log.d(ETIQUETA_LOG, " MainActivity.botonArrancarServicioPulsado : voy a arrancar el servicio");

        this.elIntentDelServicio = new Intent(this, ServicioEscucharBeacons.class);

        this.elIntentDelServicio.putExtra("tiempoDeEspera", (long) 5000);
        startService(this.elIntentDelServicio);

        this.actualizarEstado("Servicio ARRANCADO.");

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonDetenerServicioPulsado() -->
    // Qué hace: si estaba arrancado, lo detiene.
    //
    //           (viene de la antigua MainActivityServicio)
    // ---------------------------------------------------------------------------------------------
    public void botonDetenerServicioPulsado(View v) {

        if (this.elIntentDelServicio == null) {
            // no estaba arrancado
            return;
        }

        stopService(this.elIntentDelServicio);

        this.elIntentDelServicio = null;

        Log.d(ETIQUETA_LOG, " boton detener servicio Pulsado");

        this.actualizarEstado("Servicio parado.");

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonTestEnviarMedidaPulsado() -->
    // Qué hace: manejador del botón "Test automático". Lanza el test por
    //           consola que vive en ServicioEscucharBeacons. El veredicto se
    //           lee en el Log con la etiqueta ">>>>", no en pantalla.
    // ---------------------------------------------------------------------------------------------
    public void botonTestEnviarMedidaPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton test enviar medida al servidor Pulsado");

        this.actualizarEstado("Ejecutando el test automático: mira el Log...");

        ServicioEscucharBeacons.probarEnviarMedidaAlServidor();
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonPruebaPOSTPulsado() -->
    // Qué hace: manejador del botón de prueba del POST (venía de MainActivityREST).
    // ---------------------------------------------------------------------------------------------
    public void botonPruebaPOSTPulsado(View v) {
        Log.d(ETIQUETA_LOG_REST, " botonPruebaPOSTPulsado(): empieza");

        probarEnviarPOST();

        Log.d(ETIQUETA_LOG_REST, " botonPruebaPOSTPulsado(): termina");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonPruebaGETPulsado() -->
    // Qué hace: manejador del botón de prueba del GET (venía de MainActivityREST).
    // ---------------------------------------------------------------------------------------------
    public void botonPruebaGETPulsado(View v) {
        Log.d(ETIQUETA_LOG_REST, " botonPruebaGETPulsado(): empieza");

        probarEnviarGET();

        Log.d(ETIQUETA_LOG_REST, " botonPruebaGETPulsado(): termina");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonPruebaGET2Pulsado() -->
    // Qué hace: manejador del segundo botón de prueba del GET (venía de MainActivityREST).
    // ---------------------------------------------------------------------------------------------
    public void botonPruebaGET2Pulsado(View v) {
        Log.d(ETIQUETA_LOG_REST, " botonPruebaGET2Pulsado(): empieza");

        probarEnviarGET_Otra();

        Log.d(ETIQUETA_LOG_REST, " botonPruebaGET2Pulsado(): termina");
    } // ()
    // --------------------------------------------------------------BOTONES--------------------------------------------------

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> probarEnviarPOST() -->
    // Qué hace: envía un POST con un JSON de ejemplo a httpbin.org y
    //           registra la respuesta.
    //
    //           BUG CORREGIDO: el cuerpo llevaba comillas SIMPLES
    //           ( 'title' ), y eso no es JSON válido. Ahora van comillas dobles.
    // ---------------------------------------------------------------------------------------------
    private void probarEnviarPOST() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("POST", "https://httpbin.org/post",

                "{ \"title\": \"El Conde de Montecristo\", \"body\": \"Puros Habanos\", \"userId\": 1234}",
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        Log.d(ETIQUETA_LOG_REST, "TENGO RESPUESTA:\ncodigo = " + codigo + "\ncuerpo: \n" + cuerpo);

                    }
                }
        );

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> probarEnviarGET() -->
    // Qué hace: GET a jsonplaceholder y registra la respuesta.
    // ---------------------------------------------------------------------------------------------
    private void probarEnviarGET() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("GET", "https://jsonplaceholder.typicode.com/users/1234/posts",
                null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        Log.d(ETIQUETA_LOG_REST, "codigo = " + codigo + "\n" + cuerpo);
                    }
                }
        );

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> probarEnviarGET_Otra() -->
    // Qué hace: GET a reqbin y registra la respuesta.
    // ---------------------------------------------------------------------------------------------
    private void probarEnviarGET_Otra() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("GET", "https://reqbin.com/echo", null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        Log.d(ETIQUETA_LOG_REST, "codigo = " + codigo + "\n" + cuerpo);
                    }
                }
        );

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> inicializarBlueTooth() -->
    // Qué hace: obtiene el adaptador BT, lo enciende, obtiene el
    //           escáner BTLE y pide los permisos necesarios.
    // ---------------------------------------------------------------------------------------------
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
        } else if (bta != null && bta.isEnabled() == false) {
            // BUG CORREGIDO: enable() está obsoleto desde la API 33 y en versiones
            // nuevas de Android es una excepción. Lo que se puede es ofrecer al
            // usuario que lo active con la tarjeta de ajustes.
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): el BT está apagado, pido que lo enciendas a mano");
            this.actualizarEstado("Por favor, enciende el Bluetooth.");
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): pidiendo permisos si faltan");

        // solo se pide si no están concedidos ya.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    CODIGO_PETICION_PERMISOS);

            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): permisos pedidos");
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): ya tengo los permisos necesarios");
        }

        // BUG CORREGIDO: antes se pedía el escáner y se llamaba a isEnabled(),
        // getState() y getBluetoothLeScanner() TODO SEGUIDO, y sin el permiso
        // BLUETOOTH_CONNECT eso es una SecurityException y la app peta al arrancar.
        // Ahora sólo se toca el adaptador si de verdad tenemos el permiso.
        if (bta != null
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): habilitado =  " + bta.isEnabled());
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): estado =  " + bta.getState());
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos escaner btle ");

            this.elEscanner = bta.getBluetoothLeScanner();

            if (this.elEscanner == null) {
                Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): Socorro: NO hemos obtenido escaner btle  !!!!");
            }
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): sin permiso BLUETOOTH_CONNECT, no toco el adaptador");
            this.elEscanner = null;
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): TERMINA ");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: savedInstanceState: Bundle --> onCreate() -->
    // Qué hace: punto de entrada de la Activity; guarda las vistas del
    //           layout, monta la interfaz e inicializa Bluetooth.
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, " onCreate(): empieza ");

        // Agarro las vistas del layout (estaban declaradas en el XML pero no las
        // usaba nadie, así que el usuario nunca veía cambiar nada en pantalla).
        this.laEtiquetaEstado = findViewById(R.id.estado);
        this.laEtiquetaUltimaMedicion = findViewById(R.id.ultimaMedicion);

        inicializarBlueTooth();

        Log.d(ETIQUETA_LOG, " onCreate(): termina ");

    } // onCreate()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: requestCode: N, permissions: [Text], grantResults: [Z]
    //                                  --> onRequestPermissionsResult() -->
    // Qué hace: recoge el resultado de la petición de permisos.
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): EMPIEZA ");

        switch (requestCode) {
            case CODIGO_PETICION_PERMISOS:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permisos concedidos  !!!!");

                    // BUG CORREGIDO: al conceder los permisos hay que volver a
                    // iniciar BT, porque antes de esto el escáner se quedaba a null
                    // para siempre si el usuario aceptaba el diálogo.
                    inicializarBlueTooth();
                } else {

                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): Socorro: permisos NO concedidos  !!!!");
                    this.actualizarEstado("Faltan permisos: no se puede escanear BTLE.");

                }
                return;
        }
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> onDestroy() -->
    // Qué hace: al salir de la Activity, para el escaneo para no dejar el
    //           BTLE encendido, y para el servicio si estaba arrancado.
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        Log.d(ETIQUETA_LOG, " onDestroy(): para el escaneo y el servicio ");

        this.detenerBusquedaDispositivosBTLE();

        if (this.elIntentDelServicio != null) {
            stopService(this.elIntentDelServicio);
            this.elIntentDelServicio = null;
        }

        super.onDestroy();
    } // ()

} // class
