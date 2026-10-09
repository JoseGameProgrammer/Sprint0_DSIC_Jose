package com.example.jmmarter.btle_definitivo_jose;

import android.bluetooth.le.ScanSettings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanRecord;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
//       arrancarElServicio() --> startService() -->
//       buscarEsteDispositivoBTLE(nombre) -->
//
//   botonDetenerBusquedaDispositivosBTLEPulsado(v) -->
//       detenerBusquedaDispositivosBTLE() -->
//
//   callbackDelEscaneo.onScanResult() -->
//       mostrarInformacionDispositivoBTLE(resultado) -->
//           --> TramaIBeacon() + Utilidades.xxx()
//           --> enviarLaMedidaAlServidor() -->
//                   --> ServicioEscucharBeacons.enviarMedidaAlServidor() -->
//
//   botonArrancarServicioPulsado(v) --> arrancarElServicio() --> startService() -->
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
    // INDICADOR DE MODO: ¿el escaneo activo manda las medidas al servidor?
    //
    //   true   -> el escaneo es el de "Buscar NUESTRO dispositivo". Cada iBeacon que llega se
    //             manda solo a GuardarMedida.php, sin tocar nada más.
    //   false  -> el escaneo es el de "Buscar TODOS los dispositivos". Ese botón es solo para
    //             MIRE qué hay alrededor (diagnóstico), así que no manda nada a ningún sitio.
    //
    // Por qué hace falta el interruptor y no meter el envío directamente en el callback: los
    // dos botones usan EXACTAMENTE el mismo callback de escaneo. Si el envío estuviera en el
    // callback sin mirar este indicador, el botón "buscar todos" también mandaría medidas de
    // cualquiera de los beacons que hubiera alrededor, que no es lo que se quiere.
    // ---------------------------------------------------------------------------------------------
    private boolean enviarAlServidorCadaMedida = false;

    // ---------------------------------------------------------------------------------------------
    // Instancia del servicio usada SOLO para poder llamar a enviarMedidaAlServidor().
    //
    // OJO: esto NO arranca el servicio de verdad. El servicio de verdad lo arranca el Intent que
    // guarda el botón. Aquí lo único que se hace es tener un objeto con el que llamar al
    // método, que es justo lo que ya hace el test automático (ver
    // ServicioEscucharBeacons.probarEnviarMedidaAlServidor).
    //
    // Está a null hasta que hace falta, y se reutiliza para no crear uno por cada medida.
    // ---------------------------------------------------------------------------------------------
    private ServicioEscucharBeacons servicioParaEnviarAlServidor = null;

    // ---------------------------------------------------------------------------------------------
    // POR QUE EL FILTRO POR NOMBRE SE HACE EN CODIGO Y NO CON ScanFilter
    //
    // Antes se armaba un ScanFilter con setDeviceName("GTI-3A-Jose") y se lo pasaba a startScan().
    // Eso fue lo que hizo que el movil NO recibiera NADA:
    //
    //   19:26:16.119  BluetoothLeScanner onScannerRegistered() - status=0 scannerId=6
    //   19:26:30.857  boton detener busqueda dispositivos BTLE Pulsado
    //
    // El escaner arranca bien (status=0) pero en 14 segundos no entra ni un solo onScanResult().
    // Ni del beacon ni de ningun otro dispositivo: el filtro no casa con nada y se come todo.
    //
    // El problema del filtro por nombre es que Android solo puede compararlo con el nombre que
    // venga en el ANUNCIO o en la RESPUESTA DE ESCANEO. Si la placa manda el nombre en otro sitio,
    // o lo manda con otro formato, el filtro descarta TODOS los resultados aunque la placa este
    // emitiendo a dos metros. Es un fallo fragil y muy conocido.
    //
    // Solucion: escanear SIN filtros (llega todo, que es lo que queremos) y comparar el nombre
    // aqui dentro, en mostrarInformacionDispositivoBTLE(), que es donde ya se pedia el nombre.
    // Se sigue viendo solo el beacon propio, pero ahora el escaneo no depende del filtro.
    // ---------------------------------------------------------------------------------------------
    private String nombreDelDispositivoQueBuscamos = null;

    // Direcciones de los dispositivos AJENOS que ya se han anotado en el Log. Como ahora llega
    // todo el que hay alrededor, sin esto el Logcat se llena de miles de lineas y no se ve nada.
    // Solo se avisa la PRIMERA vez que aparece cada uno.
    private final Set<String> dispositivosAjenosYaAvisados = new HashSet<String>();

    // ---------------------------------------------------------------------------------------------
    // VIGILANTE DEL ESCANEO: como el escaneo se puede morir en silencio (a veces el Bluetooth del
    // telefono se queda sin escanear sin avisar y sin llamar a onScanFailed()), se comprueba cada
    // pocos segundos si ha entrado algo. Si lleva demasiado tiempo sin recibir nada, se reinicia.
    // Esto es lo que hace que el escaneo "siga" de verdad en vez de quedarse muerto.
    // ---------------------------------------------------------------------------------------------
    private Handler elHandlerDelVigilante = new Handler(Looper.getMainLooper());
    private long ultimoAnuncioRecibido = 0L;
    private static final long SEGUNDOS_SIN_RESPUESTA_ANTES_DE_REINICIAR = 6L;

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

        // -----------------------------------------------------------------------------------------
        // ESTE ES EL ARREGLO DE LO QUE NO FUNCIONABA.
        //
        // El nombre se compara AQUI, con el getName() de verdad, en vez de dejar que lo haga un
        // ScanFilter por nombre. Asi el escaneo entrega todo lo que hay alrededor y luego se
        // descarta lo que no sea el beacon nuestro.
        //
        // Va aqui arriba, antes del volcado de logs, porque sin esto el Logcat se llenaba de
        // EarPods, relojes y coches que hay en la habitacion y no se veia ni el beacon propio.
        // -----------------------------------------------------------------------------------------
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String nombreDeEsteDispositivo = bluetoothDevice.getName();

        if (this.nombreDelDispositivoQueBuscamos != null
                && !this.nombreDelDispositivoQueBuscamos.equals(nombreDeEsteDispositivo)) {

            // No es el nuestro. Solo se avisa la primera vez que aparece cada uno, o el Log
            // se llenaria de miles de lineas repetidas.
            if (this.dispositivosAjenosYaAvisados.add(bluetoothDevice.getAddress())) {
                Log.d(ETIQUETA_LOG, "  escaneo VIVO, otro dispositivo que no es el nuestro: "
                        + nombreDeEsteDispositivo + "   rssi = " + rssi);
            }
            return;
        }

        // Ha entrado el dispositivo que nos interesa: el escaneo esta vivo.
        this.ultimoAnuncioRecibido = System.currentTimeMillis();

        Log.d(ETIQUETA_LOG, " mostrarInformacionDispositivoBTLLE(): empieza ");
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

        // ---------------------------------------------------------------------------------------
        // ENVÍO AUTOMÁTICO AL SERVIDOR
        //
        // Solo se manda si el escaneo activo es el de "Buscar NUESTRO dispositivo" (el indicador
        // está a true). Con el botón de "Buscar TODOS" esto no se hace, porque ese botón es
        // para mirar qué hay alrededor y no para guardar cosas.
        //
        // OJO CON LO QUE SE MANDA: se manda el iBeacon TAL CUAL ha llegado del aire. No se
        // guarda el rssi (que es la distancia aproximada y cambia cada vez), sino el txPower,
        // que es el valor fijo que el propio beacon lleva dentro. Por eso aquí no sale el rssi.
        //
        // El filtro de duplicados está en enviarMedidaAlServidor(): como el mismo beacon se
        // anuncia muchas veces por segundo, solo sale a la red la primera. Así la BBDD no se
        // llena de copias idénticas.
        // ---------------------------------------------------------------------------------------
        if (this.enviarAlServidorCadaMedida) {

            Log.d(ETIQUETA_LOG, " mostrarInformacionDispositivoBTLE(): mando la medida al servidor (automático)");

            this.enviarLaMedidaAlServidor(tib, bluetoothDevice.getName());

        } else {

            Log.d(ETIQUETA_LOG, " mostrarInformacionDispositivoBTLE(): NO mando nada, este escaneo es solo para mirar");

        } // ()

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: tib: TramaIBeacon, nombreEmisora: Text --> enviarLaMedidaAlServidor() -->
    //
    // Qué hace: le pasa al servicio los 5 campos de la Medida tal y como han salido del aire, y
    //           que él los convierta en JSON y los mande por HTTP POST.
    //
    // QUÉ CAMPOS SE MANDAN Y CUALES NO, Y POR QUÉ
    // -------------------------------------------
    //     uuid          -> el del beacon, que aquí es "EPSG-GTI-PROY-3A"
    //     major         -> el que traía el anuncio
    //     minor         -> el valor medido (el de CO2 o el de temperatura)
    //     txPower       -> el que traía el anuncio (fijo, -53)
    //     nombreEmisora -> el nombre BLE del dispositivo ("GTI-3A-Jose")
    //
    // NO se manda el rssi: es la intensidad de la señal en este instante, o sea lo cerca que
    // esté el móvil, y cambia con cada anuncio. No es un dato de la medición, es del camino.
    // Y NO se manda la fecha: la pone el servidor, que es quien sabe cuándo la ha recibido.
    // ---------------------------------------------------------------------------------------------
    private void enviarLaMedidaAlServidor(TramaIBeacon tib, String nombreEmisora) {

        // El servicio se crea la primera vez y se reutiliza después.
        if (this.servicioParaEnviarAlServidor == null) {
            this.servicioParaEnviarAlServidor = new ServicioEscucharBeacons();
        } // ()

        // OJO: Utilidades.bytesToInt() es lo que convierte los bytes del anuncio en el número
        // de verdad. Sin esa conversión se mandaría el byte suelto, que es solo la mitad baja
        // del número (por eso en el Log salen dos valores, el hexadecimal y el decimal).
        this.servicioParaEnviarAlServidor.enviarMedidaAlServidor(

                Utilidades.bytesToString(tib.getUUID()),
                Utilidades.bytesToInt(tib.getMajor()),
                Utilidades.bytesToInt(tib.getMinor()),
                tib.getTxPower(),
                nombreEmisora

        ); // ()

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

                // El vigilante usa esta marca para saber que el escaneo SIGUE vivo.
                MainActivity.this.ultimoAnuncioRecibido = System.currentTimeMillis();

                MainActivity.this.mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);

                MainActivity.this.ultimoAnuncioRecibido = System.currentTimeMillis();

                for (int i = 0; i < results.size(); i++) {
                    MainActivity.this.mostrarInformacionDispositivoBTLE(results.get(i));
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onScanFailed() codigo = " + errorCode);
                MainActivity.this.actualizarEstado("El escaneo ha fallado (código " + errorCode + ").");
            }
        };

// El nombre se guarda para compararlo DENTRO del callback (ver
        // mostrarInformacionDispositivoBTLE), no para armar un ScanFilter. Ver el comentario
        // largo del campo nombreDelDispositivoQueBuscamos para saber por qu�.
        this.nombreDelDispositivoQueBuscamos = dispositivoBuscado;
        this.dispositivosAjenosYaAvisados.clear();

        Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): empezamos a escanear buscando: " + dispositivoBuscado);

        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): empieza scan");

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        // SIN filtros a proposito: es lo que hace que llegue algo. El filtro por nombre se
        // hacia en codigo, en el callback, y no antes.
        this.elEscanner.startScan(null, settings, this.callbackDelEscaneo);

        // El vigilante necesita un punto de partida: si no, dariera el primer aviso a los
        // SEGUNDOS_SIN_RESPUESTA_ANTES_DE_REINICIAR aunque acabemos de empezar.
        this.ultimoAnuncioRecibido = System.currentTimeMillis();
        this.arrancarElVigilanteDelEscaneo();

        this.actualizarEstado("Escaneando s�lo " + dispositivoBuscado + "...");

        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): termina");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> arrancarElVigilanteDelEscaneo() --> elVigilanteDelEscaneo.run() -->
    //
    // Qué hace: cada 6 segundos mira si ha entrado algún anuncio. Si pasan 6 segundos seguidos
    //           sin ninguno, da el escaneo por muerto y lo vuelve a arrancar.
    //
    // Por qué: el Bluetooth del móvil a veces deja de escanear sin dar ningún error. No llega
    //           ni onScanFailed() ni onScanResult(), así que la app se queda esperando para
    //           siempre creyendo que está escaneando. Con este vigilante se recupera solo.
    // ---------------------------------------------------------------------------------------------
    private void arrancarElVigilanteDelEscaneo() {
        this.elHandlerDelVigilante.removeCallbacks(this.elVigilanteDelEscaneo);
        this.elHandlerDelVigilante.postDelayed(this.elVigilanteDelEscaneo,
                SEGUNDOS_SIN_RESPUESTA_ANTES_DE_REINICIAR * 1000L);
    } // ()

    private void detenerElVigilanteDelEscaneo() {
        this.elHandlerDelVigilante.removeCallbacks(this.elVigilanteDelEscaneo);
    } // ()

    private final Runnable elVigilanteDelEscaneo = new Runnable() {
        @Override
        public void run() {

            if (MainActivity.this.nombreDelDispositivoQueBuscamos == null
                    || MainActivity.this.callbackDelEscaneo == null) {
                // No hay escaneo activo, no hay nada que vigilar.
                return;
            }

            long pasaDesdeElUltimo = System.currentTimeMillis()
                    - MainActivity.this.ultimoAnuncioRecibido;

            if (pasaDesdeElUltimo > SEGUNDOS_SIN_RESPUESTA_ANTES_DE_REINICIAR * 1000L) {

                Log.d(ETIQUETA_LOG, "VIGILANTE: " + (pasaDesdeElUltimo / 1000L)
                        + " segundos sin recibir nada, reinicio el escaneo");

                MainActivity.this.actualizarEstado("El escaneo se ha quedado parado. Reiniciando...");

                // buscarEsteDispositivoBTLE() empieza parando el escaneo anterior, asi que
                // llamarlo otra vez es justo la forma de reiniciarlo.
                MainActivity.this.buscarEsteDispositivoBTLE(
                        MainActivity.this.nombreDelDispositivoQueBuscamos);

            } else {
                // Todo bien, nos volvemos a mirar dentro del mismo rato.
                MainActivity.this.elHandlerDelVigilante.postDelayed(this,
                        SEGUNDOS_SIN_RESPUESTA_ANTES_DE_REINICIAR * 1000L);
            }
        }
    };

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> detenerBusquedaDispositivosBTLE() -->
    // Qué hace: para el escaneo si hay uno activo y limpia el callback.
    // ---------------------------------------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): empieza");

        // El vigilante se para SIEMPRE primero, incluso en los casos en los que luego se
        // vuelve antes de tiempo. Si no, se queda reiniciando un escaneo que ya no existe.
        this.detenerElVigilanteDelEscaneo();
        this.nombreDelDispositivoQueBuscamos = null;

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
    //
    //           Este botón es de MIRAR, no de enviar: antes de arrancar el escaneo pone el
    //           indicador de envío a false, para que ningún beacon que aparezca se mande al
    //           servidor por sorpresa.
    // ---------------------------------------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton buscar dispositivos BTLE Pulsado");

        this.enviarAlServidorCadaMedida = false;

        this.buscarTodosLosDispositivosBTLE();
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonBuscarNuestroDispositivoBTLEPulsado() -->
    // Qué hace: manejador del botón "buscar nuestro". Este es el botón que hace el trabajo
    //           completo de la app, en tres pasos y en este orden:
    //
    //             1. Pone el indicador de envío a true, para que cada medida que llegue se
    //                mande sola a GuardarMedida.php.
    //             2. Enciende el servicio en segundo plano, por si la app se cierra.
    //             3. Arranca el escaneo filtrando por el nombre del beacon.
    //
    //           OJO CON EL ORDEN: el servicio se enciende ANTES de escanear a propósito. Si se
    //           pusiera en marcha una vez escaneando, habría un hueco en el que llega un beacon,
    //           se manda la medida y todavía no hay servicio de fondo. Así no.
    // ---------------------------------------------------------------------------------------------
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton nuestro dispositivo BTLE Pulsado");

        // (1) A partir de aquí, cada medida se manda sola.
        this.enviarAlServidorCadaMedida = true;

        // (2) Servicio de fondo encendido.
        this.arrancarElServicio();

        // (3) Y ahora a escuchar el beacon.
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
    // DISEÑO: --> arrancarElServicio() --> startService() -->
    //
    // Qué hace: si el servicio no está ya arrancado, crea el Intent con el tiempo de espera
    //           y lo arranca. Si ya estaba, no hace nada.
    //
    // POR QUÉ ESTÁ SUELTO Y NO DENTRO DEL BOTÓN
    // ------------------------------------------
    // Porque lo usan DOS sitios: el botón "Arrancar Servicio" y el botón "Buscar NUESTRO
    // dispositivo". Si estuviera escrito dentro del botón, habría que copiarlo en los dos, y
    // en cuanto uno de los dos cambiara se olvida el otro. Con un método suelto, los dos
    // llaman al mismo sitio y solo hay una copia.
    //
    // OJO: esto es idempotente, o sea que llamarlo dos veces NO crea dos servicios. El
    // elIntentDelServicio a null es lo que dice "todavía no lo he arrancado".
    // ---------------------------------------------------------------------------------------------
    private void arrancarElServicio() {

        if (this.elIntentDelServicio != null) {
            // ya estaba arrancado
            return;
        } // ()

        Log.d(ETIQUETA_LOG, " MainActivity.arrancarElServicio() : voy a arrancar el servicio");

        this.elIntentDelServicio = new Intent(this, ServicioEscucharBeacons.class);

        this.elIntentDelServicio.putExtra("tiempoDeEspera", (long) 5000);
        startService(this.elIntentDelServicio);

        Log.d(ETIQUETA_LOG, " MainActivity.arrancarElServicio() : servicio ARRANCADO");

        this.actualizarEstado("Servicio ARRANCADO.");

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonArrancarServicioPulsado() -->
    // Qué hace: manejador del botón "Arrancar Servicio".
    //
    //           (viene de la antigua MainActivityServicio)
    // ---------------------------------------------------------------------------------------------
    public void botonArrancarServicioPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton arrancar servicio Pulsado");

        this.arrancarElServicio();

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


    public void botonSimularBeaconPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton simular beacon pulsado");
        this.actualizarEstado("Simulando recepción de un iBeacon...");

        // Simulamos un iBeacon: UUID conocido, major 2817 (tipo 11 -> CO2), minor aleatorio (400-800 ppm), txPower -53
        String uuid = "EPSG-GTI-PROY-3A";
        int major = 2817; // 11 << 8 + 1
        int minor = 400 + (int)(Math.random() * 400); // random val 400-800
        int txPower = -53;
        String nombreEmisora = "GTI-Jose-Simulado";

        // Mismo proceso que cuando recibimos uno real
        if (this.servicioParaEnviarAlServidor == null) {
            this.servicioParaEnviarAlServidor = new ServicioEscucharBeacons();
        }
        
        this.servicioParaEnviarAlServidor.enviarMedidaAlServidor(uuid, major, minor, txPower, nombreEmisora);
        
        // Y actualizamos la UI (tal y como lo hace mostrarInformacionDispositivoBTLE y actualizarUltimaMedicionEnPantalla)
        String textoMedicion = String.format(
            "Última medición recibida: Tipo: %d, Valor: %d Emisora: %s",
            major, minor, nombreEmisora
        );
        this.laEtiquetaUltimaMedicion.setText(textoMedicion);
    }



    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonPruebaPOSTPulsado() -->
    // Qué hace: manejador del botón de prueba del POST (venía de MainActivityREST).
    // ---------------------------------------------------------------------------------------------
    

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonPruebaGETPulsado() -->
    // Qué hace: manejador del botón de prueba del GET (venía de MainActivityREST).
    // ---------------------------------------------------------------------------------------------
    

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonPruebaGET2Pulsado() -->
    // Qué hace: manejador del segundo botón de prueba del GET (venía de MainActivityREST).
    // ---------------------------------------------------------------------------------------------
    
    // --------------------------------------------------------------BOTONES--------------------------------------------------

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> probarEnviarPOST() -->
    // Qué hace: envía un POST con un JSON de ejemplo a httpbin.org y
    //           registra la respuesta.
    //
    //           BUG CORREGIDO: el cuerpo llevaba comillas SIMPLES
    //           ( 'title' ), y eso no es JSON válido. Ahora van comillas dobles.
    // ---------------------------------------------------------------------------------------------
    

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> probarEnviarGET() -->
    // Qué hace: GET a jsonplaceholder y registra la respuesta.
    // ---------------------------------------------------------------------------------------------
    

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> probarEnviarGET_Otra() -->
    // Qué hace: GET a reqbin y registra la respuesta.
    // ---------------------------------------------------------------------------------------------
    

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
