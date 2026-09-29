package org.jordi.holamundoservicio;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

// ==========================================================
// DISEÑO LÓGICO: clase ServicioEscuharBeacons (IntentService)
// ----------------------------------------------------------
//   tiempoDeEspera: N, seguir: B
//   ServicioEscuharBeacons() -->
//       parar() -->
//       onDestroy() -->
//       onHandleIntent(intent) -->
//
// Servicio que, en un hilo worker, se queda "escuchando" (bucle de
// espera) hasta que se le manda parar
// ==========================================================
// -------------------------------------------------------------------------------------------------
// -------------------------------------------------------------------------------------------------
public class ServicioEscuharBeacons  extends IntentService {

    // ---------------------------------------------------------------------------------------------
    // Etiqueta de Log.
    // ---------------------------------------------------------------------------------------------
    private static final String ETIQUETA_LOG = ">>>>";

    // Milisegundos entre iteraciones del bucle de escucha.
    private long tiempoDeEspera = 10000;

    // Bandera de control del bucle
    private volatile boolean seguir = true;

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> ServicioEscuharBeacons() -->
    // Qué hace: constructor; pasa el nombre del worker thread a la
    //           clase base.
    // ---------------------------------------------------------------------------------------------
    public ServicioEscuharBeacons(  ) {
        super("HelloIntentService");

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.constructor: termina");
    }

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> parar() -->
    // Qué hace: pone la bandera a false y detiene el servicio.
    // ---------------------------------------------------------------------------------------------
    public void parar () {

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.parar() " );

        if ( this.seguir == false ) {
            return;
        }

        this.seguir = false;
        this.stopSelf();

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.parar() : acaba " );

    }

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> onDestroy() -->
    // Qué hace: al destruirse el servicio, se asegura de parar el bucle.
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onDestroy() {

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onDestroy() " );

        this.parar(); // posiblemente no haga falta, si stopService() ya se carga el servicio y su worker thread
    }

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: intent: Intent --> onHandleIntent() -->
    // Qué hace: tarea del servicio (en un worker thread): lee el
    //           tiempo de espera del Intent y se queda en el bucle
    //           hasta que "seguir" sea false.
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onHandleIntent(Intent intent) {

        this.tiempoDeEspera = intent.getLongExtra("tiempoDeEspera", /* default */ 50000);
        this.seguir = true;

        // esto lo ejecuta un WORKER THREAD !

        long contador = 1;

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onHandleIntent: empieza : thread=" + Thread.currentThread().getId() );

        try {

            while ( this.seguir ) {
                Thread.sleep(tiempoDeEspera);
                Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onHandleIntent: tras la espera:  " + contador );
                contador++;
            }

            Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onHandleIntent : tarea terminada ( tras while(true) )" );


        } catch (InterruptedException e) {
            // Restore interrupt status.
            Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onHandleItent: problema con el thread");

            Thread.currentThread().interrupt();
        }

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onHandleItent: termina");

    }
} // class
