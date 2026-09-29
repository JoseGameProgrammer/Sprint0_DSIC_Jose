package org.jordi.holamundoservicio;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

// ==========================================================
// DISEÑO LÓGICO: MainActivity (arranca/para el servicio)
// ----------------------------------------------------------
//   elIntentDelServicio: Intent
//   botonArrancarServicioPulsado(v) --> startService() -->
//   botonDetenerServicioPulsado(v)  --> stopService() -->
//   onCreate() -->
//
// UI mínima que arranca y detiene ServicioEscuharBeacons,
// guardando el Intent para no arrancarlo dos veces.
// ==========================================================
// -------------------------------------------------------------------------------------------------
// -------------------------------------------------------------------------------------------------
public class MainActivity extends AppCompatActivity {

    // ---------------------------------------------------------------------------------------------
    // Etiqueta de Log y Intent con el que se arrancó el servicio
    // ---------------------------------------------------------------------------------------------
    private static final String ETIQUETA_LOG = ">>>>";

    private Intent elIntentDelServicio = null;

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonArrancarServicioPulsado() -->
    // Qué hace: si no está arrancado, crea el Intent con el tiempo de
    //           espera y arranca el servicio.
    // ---------------------------------------------------------------------------------------------
    public void botonArrancarServicioPulsado( View v ) {
        Log.d(ETIQUETA_LOG, " boton arrancar servicio Pulsado" );

        if ( this.elIntentDelServicio != null ) {
            // ya estaba arrancado
            return;
        }

        Log.d(ETIQUETA_LOG, " MainActivity.constructor : voy a arrancar el servicio");

        this.elIntentDelServicio = new Intent(this, ServicioEscuharBeacons.class);

        this.elIntentDelServicio.putExtra("tiempoDeEspera", (long) 5000);
        startService( this.elIntentDelServicio );

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> botonDetenerServicioPulsado() -->
    // Qué hace: si estaba arrancado, lo detiene.
    // ---------------------------------------------------------------------------------------------
    public void botonDetenerServicioPulsado( View v ) {

        if ( this.elIntentDelServicio == null ) {
            // no estaba arrancado
            return;
        }

        stopService( this.elIntentDelServicio );

        this.elIntentDelServicio = null;

        Log.d(ETIQUETA_LOG, " boton detener servicio Pulsado" );


    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: savedInstanceState: Bundle --> onCreate() -->
    // Qué hace: monta la UI.
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, " MainActivity.constructor : empieza");



        Log.d(ETIQUETA_LOG, " MainActivity.constructor : acaba");

    }
} // class
