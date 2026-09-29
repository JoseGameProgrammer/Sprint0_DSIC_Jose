package org.jordi.pruebarest1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

// ==========================================================
// DISEÑO LÓGICO: MainActivity (pruebas del cliente REST)
// ----------------------------------------------------------
//   onCreate() -->
//   prueba1_pulsado(v) --> probarEnviarPOST() -->
//   probarEnviarGET() -->
//   probarEnviarGET_Otra() -->
//
// UI de pruebas que lanza peticiones REST con PeticionarioREST y
// muestra por Log la respuesta recibida en el callback.
// ==========================================================
// -------------------------------------------------------------------------------------------------
// -------------------------------------------------------------------------------------------------
public class MainActivity extends AppCompatActivity {

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: savedInstanceState: Bundle --> onCreate() -->
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: v: View --> prueba1_pulsado() -->
    // Qué hace: manejador del botón de prueba; lanza el POST.
    // ---------------------------------------------------------------------------------------------
    public void prueba1_pulsado(View v) {
        Log.d( "pruebasPeticionario", "prueba1_pulsado() empieza");

        probarEnviarPOST();

        Log.d( "pruebasPeticionario", "prueba1_pulsado() termina");
    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> probarEnviarPOST() -->
    // Qué hace: envía un POST con un JSON de ejemplo a httpbin.org y
    //           registra la respuesta.
    // ---------------------------------------------------------------------------------------------
    private void probarEnviarPOST() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("POST",  "https://httpbin.org/post",

                "{ 'title': 'El Conde de Montecristo', 'body': 'Puros Habanos', 'userId': 1234}",
                new PeticionarioREST.RespuestaREST () {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        Log.d( "pruebasPeticionario", "TENGO RESPUESTA:\ncodigo = " + codigo + "\ncuerpo: \n" + cuerpo);

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

        elPeticionario.hacerPeticionREST("GET",  "https://jsonplaceholder.typicode.com/users/1234/posts",
                null,
                new PeticionarioREST.RespuestaREST () {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        Log.d( "pruebasPeticionario", "codigo = " + codigo + "\n" + cuerpo);
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

        elPeticionario.hacerPeticionREST("GET",  "https://reqbin.com/echo", null,
                new PeticionarioREST.RespuestaREST () {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        Log.d( "pruebasPeticionario", "codigo = " + codigo + "\n" + cuerpo);
                    }
                }
        );

    } // ()

} // class
