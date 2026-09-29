package com.example.jmmarter.btle_definitivo_jose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import android.os.AsyncTask;
import android.util.Log;

// ==========================================================
// DISEÑO LÓGICO: clase PeticionarioREST (AsyncTask)
// ----------------------------------------------------------
//   elMetodo: Text, urlDestino: Text, elCuerpo: Text, laRespuesta: RespuestaREST
//   PeticionarioREST() -->
//   metodo, url, cuerpo, callback --> hacerPeticionREST() -->
//       doInBackground() -->
//       onPostExecute(comoFue) --> laRespuesta.callback(codigo, cuerpo)
//
// Cliente HTTP que hace una petición REST y devuelve el resultado
// por callback, fuera del hilo principal (AsyncTask).
// ==========================================================
// ------------------------------------------------------------------------
// ------------------------------------------------------------------------
public class PeticionarioREST extends AsyncTask<Void, Void, Boolean> {

    // --------------------------------------------------------------------
    // Tiempos máximos de conexión y lectura (ms).
    // --------------------------------------------------------------------
    private static final int TIMEOUT_CONEXION = 10000;
    private static final int TIMEOUT_LECTURA = 10000;

    // --------------------------------------------------------------------
    // Datos de la petición y de la respuesta.
    // --------------------------------------------------------------------
    private String elMetodo;
    private String urlDestino;
    private String elCuerpo = null;
    private RespuestaREST laRespuesta;
    private int codigoRespuesta;
    private String cuerpoRespuesta = "";

    // --------------------------------------------------------------------
    // DISEÑO: --> PeticionarioREST() -->
    // --------------------------------------------------------------------
    public PeticionarioREST() {
        Log.d("clienterestandroid", "constructor()");
    }

    // --------------------------------------------------------------------
    // DISEÑO: metodo: Text, url: Text, cuerpo: Text, r: RespuestaREST
    //                                  --> hacerPeticionREST() -->
    // Qué hace: guarda los datos y lanza la tarea (execute()).
    // --------------------------------------------------------------------
    public void hacerPeticionREST(String metodo, String urlDestino, String cuerpo, RespuestaREST laRespuesta) {
        this.elMetodo = metodo;
        this.urlDestino = urlDestino;
        this.elCuerpo = cuerpo;
        this.laRespuesta = laRespuesta;

        this.execute(); // otro thread ejecutará doInBackground()
    }

    // --------------------------------------------------------------------
    // DISEÑO: --> doInBackground() <-- B
    // Qué hace: abre la conexión, envía la petición (con cuerpo si no es
    //           GET), lee la respuesta (o el error) y devuelve true/false.
    //
    // MEJORAS:
    //  - Content-Type correcto: charset=utf-8 (antes charset-utf-8, que
    //    no es un parámetro válido).
    //  - El cuerpo se envía en UTF-8 y con el header Content-Type.
    //  - Se añaden timeouts (antes podía colgarse indefinidamente).
    //  - Si la respuesta es de error (>= 400) se lee getErrorStream().
    //  - La conexión se cierra SIEMPRE (finally).
    //  - Se usan recursos try-with-resources.
    // --------------------------------------------------------------------
    @Override
    protected Boolean doInBackground(Void... params) {
        Log.d("clienterestandroid", "doInBackground()");

        HttpURLConnection connection = null;
        try {

            // envio la peticion

            Log.d("clienterestandroid", "doInBackground() me conecto a >" + urlDestino + "<");

            URL url = new URL(urlDestino);

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_CONEXION);
            connection.setReadTimeout(TIMEOUT_LECTURA);
            connection.setRequestMethod(this.elMetodo);
            connection.setDoInput(true);

            if (!this.elMetodo.equals("GET") && this.elCuerpo != null) {
                Log.d("clienterestandroid", "doInBackground(): no es get, pongo cuerpo");
                connection.setDoOutput(true);
                // el cuerpo se escribe codificado en UTF-8.
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytesCuerpo = this.elCuerpo.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytesCuerpo.length);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(bytesCuerpo);
                    os.flush();
                }
            }

            // ya he enviado la peticion
            Log.d("clienterestandroid", "doInBackground(): peticion enviada ");

            // ahora obtengo la respuesta

            int rc = connection.getResponseCode();
            String rm = connection.getResponseMessage();
            String respuesta = "" + rc + " : " + rm;
            Log.d("clienterestandroid", "doInBackground() recibo respuesta = " + respuesta);
            this.codigoRespuesta = rc;

            try {
                // si es error, el cuerpo viene por getErrorStream().
                InputStream is = (rc >= 400) ? connection.getErrorStream() : connection.getInputStream();

                if (is == null) {
                    this.cuerpoRespuesta = "";
                } else {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(is, StandardCharsets.UTF_8))) {

                        Log.d("clienterestandroid", "leyendo cuerpo");
                        StringBuilder acumulador = new StringBuilder();
                        String linea;
                        while ((linea = br.readLine()) != null) {
                            Log.d("clienterestandroid", linea);
                            acumulador.append(linea);
                        }
                        Log.d("clienterestandroid", "FIN leyendo cuerpo");

                        this.cuerpoRespuesta = acumulador.toString();
                        Log.d("clienterestandroid", "cuerpo recibido=" + this.cuerpoRespuesta);
                    }
                }

            } catch (IOException ex) {
                // dispara excepcion cuando la respuesta REST no tiene cuerpo y yo intento getInputStream()
                Log.d("clienterestandroid", "doInBackground() : parece que no hay cuerpo en la respuesta");
            }

            return true; // doInBackground() termina bien

        } catch (Exception ex) {
            Log.d("clienterestandroid", "doInBackground(): ocurrio alguna otra excepcion: " + ex.getMessage());
        } finally {
            // se cierra la conexión pase lo que pase.
            if (connection != null) {
                connection.disconnect();
            }
        }

        return false; // doInBackground() NO termina bien
    } // ()

    // --------------------------------------------------------------------
    // DISEÑO: comoFue: B --> onPostExecute() -->
    // Qué hace: al terminar la tarea en background, invoca el callback
    //           con el código y el cuerpo de la respuesta.
    // --------------------------------------------------------------------
    @Override
    protected void onPostExecute(Boolean comoFue) {
        // llamado tras doInBackground()
        Log.d("clienterestandroid", "onPostExecute() comoFue = " + comoFue);
        this.laRespuesta.callback(this.codigoRespuesta, this.cuerpoRespuesta);
    }

    // --------------------------------------------------------------------
    // Interfaz que implementa quien quiere recibir la respuesta.
    // --------------------------------------------------------------------
    public interface RespuestaREST {
        void callback(int codigo, String cuerpo);
    }

} // class
