package com.example.jmmarter.btle_definitivo_jose;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

// ==================================================================================================
// DISEÑO LÓGICO: clase ServicioEscucharBeacons (IntentService)
// --------------------------------------------------------------------------------------------------
//   ------------------------ ServicioEscucharBeacons ------------------------
//   |
//   | ETIQUETA_LOG: Text
//   | tiempoDeEspera: N
//   | seguir: B
//   |
//   |
//                     ServicioEscucharBeacons() -->
//   |
//   |
//   intent: Intent --> onHandleIntent() -->
//   |
//   |
//                     parar() -->
//   |
//   |
//                     onDestroy() -->
//   |
//   |
//   uuid: Text, major: N, minor: Z,
//   txPower: Z, nombreEmisora: Text
//                  --> enviarMedidaAlServidor() -->
//   |
//   |
//   uuid: Text, major: N, minor: Z, txPower: Z,
//   nombreEmisora: Text --> construirJSONMedida() --x
//   |
//   |
//                     probarEnviarMedidaAlServidor() --x
//   |
//   ------------------------------------------------------------------------------
//
// Servicio en segundo plano que se queda "escuchando" (bucle de espera en un
// worker thread) hasta que se le manda parar, y que sabe enviar al servidor
// REST la medida (beacon) que ha recibido o simulado.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------
public class ServicioEscucharBeacons extends IntentService {

    // ---------------------------------------------------------------------------------------------
    // Etiqueta de Log.
    // ---------------------------------------------------------------------------------------------
    private static final String ETIQUETA_LOG = ">>>>";

    // =============================================================================================
    //  #  TODO(Jose) - CONFIGURACIÓN DEL SERVIDOR REST : CÁMBIALO ANTES DE DAR EL TEST EN VERDE
    // =============================================================================================
    //  #  El enunciado pedía "../rest/GuardarMedida.php", pero esa es una RUTA RELATIVA de PHP:
    //  #  desde el móvil no existe tal cosa. PeticionarioREST necesita una URL ABSOLUTA, es decir
    //  #  "http://<host>:<puerto><ruta>". Por eso está partida en dos trozos:
    //  #
    //  #    URL_BASE_SERVIDOR  ->  http://<host>:<puerto>/rest/
    //  #    RUTA_GUARDAR_MEDIDA ->  GuardarMedida.php
    //  #
    //  #  QUÉ PONER EN URL_BASE_SERVIDOR según dónde pruebes:
    //  #
    //  #   * EMULADOR de Android Studio ->  http://10.0.2.2/rest/
    //  #     El emulador NO puede ver "localhost": 10.0.2.2 es el alias que el emulador da a
    //  #     tu propio PC (la máquina anfitriona). Es el valor que hay ahora.
    //  #
    //  #   * TELÉFONO REAL (cable o wifi) ->  http://<IP-DE-TU-PC>:8080/rest/
    //  #     Ejemplo: http://192.168.1.50:8080/rest/
    //  #     La IP es la que sale en CMD con  ipconfig  (IPv4 Address, p.ej. 192.168.1.50).
    //  #     OJO: el PC y el móvil TIENEN que estar en la MISMA red wifi, y el firewall de
    //  #     Windows tiene que dejar pasar el puerto 80 de PHP.
    //  #
    //  #   * PUERTO: XAMPP/WAMP sirven por el 80, y el 80 no se escribe (es el que se asume).
    //  #     Si tu servidor va en otro puerto, ponlo detrás de la IP:  http://192.168.1.50:8080/
    //  #
    //  #  OJO CON LAS BARRAS: URL_BASE_SERVIDOR acaba en "/" y la ruta NO lo lleva, o si no
    //  #  queda una doble barra y el PHP no lo encuentra.
    //  #
    //  #  ---------------------------------------------------------------------------------------
    //  #  EL SERVIDOR YA EXISTE (prompt 3, 29/09/2026):
    //  #  web\rest\GuardarMedida.php está escrito y probado por web\rest\testServidorREST.php,
    //  #  que da "TEST SERVIDOR REST OK". El endpoint responde SIEMPRE con HTTP 200 y dentro
    //  #  del JSON un campo "error" a 0 o a 1, que es justo lo que comprueba este test:
    //  #
    //  #      HTTP 200 + {"error":0,"mensaje":"Medida guardada correctamente"}
    //  #
    //  #  OJO: por eso NO se devuelve un 400 cuando hay error. Si el endpoint contestara 400,
    //  #  PeticionarioREST lo trataría como fallo de red, leería getErrorStream() en vez de
    //  #  getInputStream(), y este test daría ERROR aunque el mensaje fuera correcto.
    //  #
    //  #  ALGO QUE SÍ QUEDA PENDIENTE: si pruebas con un TELÉFONO REAL en vez de con el
    //  #  emulador, hay que cambiar 10.0.2.2 por la IP de este PC (ver más arriba), y el
    //  #  firewall de Windows tiene que dejar entrar el puerto del servidor.
    //  #  ---------------------------------------------------------------------------------------
    // =============================================================================================
    public static final String URL_BASE_SERVIDOR = "http://10.0.2.2/rest/";
    public static final String RUTA_GUARDAR_MEDIDA = "GuardarMedida.php";

    // URL completa a la que se hace el POST de la medida.
    private static final String URL_GUARDAR_MEDIDA = URL_BASE_SERVIDOR + RUTA_GUARDAR_MEDIDA;

    // ---------------------------------------------------------------------------------------------
    // Milisegundos entre iteraciones del bucle de escucha.
    // ---------------------------------------------------------------------------------------------
    private long tiempoDeEspera = 10000;

    // ---------------------------------------------------------------------------------------------
    // Bandera de control del bucle
    // ---------------------------------------------------------------------------------------------
    private volatile boolean seguir = true;

    // ---------------------------------------------------------------------------------------------
    // Memoria de la última medida enviada, para el filtro de duplicados.
    //
    // Son ESTÁTICOS a propósito: "no mandar dos veces seguidas la misma medida" es una
    // regla de toda la aplicación, no de una instancia del servicio. Así, si Android
    // destruyera y recreara el servicio, no se perdería la remembered última medida.
    //
    // primeraMedida vale true mientras todavía no se ha enviado NINGUNA, para poder
    // distinguir "no he enviado nada todavía" de "la última fue 0 y 0".
    // ---------------------------------------------------------------------------------------------
    private static boolean primeraMedida = true;
    private static int ultimoMajorEnviado = 0;
    private static int ultimoMinorEnviado = 0;

    // ---------------------------------------------------------------------------------------------
    // Contador de peticiones LANZADAS al servidor. Lo usa el test automático para poder
    // comprobar, sin esperar a la red, que la segunda medida repetida NO sale a la red.
    // ---------------------------------------------------------------------------------------------
    private static int peticionesLanzadas = 0;

    // ---------------------------------------------------------------------------------------------
    // Observador opcional de las respuestas del servidor. Lo usa el test automático para
    // enterarse del (codigo, cuerpo) sin tener que cambiar la firma de enviarMedidaAlServidor().
    // Si es null (uso normal) no se avisa a nadie.
    // ---------------------------------------------------------------------------------------------
    private static PeticionarioREST.RespuestaREST observadorDePruebas = null;

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> ServicioEscucharBeacons() -->
    // Qué hace: constructor; pasa el nombre del worker thread a la clase base.
    // ---------------------------------------------------------------------------------------------
    public ServicioEscucharBeacons() {
        super("ServicioEscucharBeacons");

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.constructor: termina");
    }

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> parar() -->
    // Qué hace: pone la bandera a false y detiene el servicio.
    // ---------------------------------------------------------------------------------------------
    public void parar() {

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.parar() ");

        if (this.seguir == false) {
            return;
        }

        this.seguir = false;
        this.stopSelf();

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.parar() : acaba ");

    }

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> onDestroy() -->
    // Qué hace: al destruirse el servicio, se asegura de parar el bucle.
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onDestroy() {

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onDestroy() ");

        super.onDestroy(); // la clase base también debe ejecutarse

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

            while (this.seguir) {
                Thread.sleep(tiempoDeEspera);
                Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onHandleIntent: tras la espera:  " + contador);
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

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
    //                                                  --> construirJSONMedida() --x
    //                Text <--
    //
    // Qué hace: mete los 5 campos del beacon en un String con formato JSON VÁLIDO,
    //           usando exactamente las claves "uuid", "major", "minor", "txPower"
    //           y "nombreEmisora".
    //
    //           OJO: JSON obliga a comillas DOBLES. Con comillas simples no hay JSON
    //           válido y el PHP no lo puede leer.
    //
    //           Es ESTÁTICA (--x) porque no toca ningún estado: depende sólo de sus
    //           parámetros, que es justo lo que significa una función matemática.
    //
    //           Nota sobre los tipos: en Java no hay enteros sin signo, así que N
    //           (major) y Z (minor, txPower) se implementan todos como int.
    // ---------------------------------------------------------------------------------------------
    public static String construirJSONMedida(String uuid, int major, int minor, int txPower, String nombreEmisora) {

        // aquí se CONSTRUYE el JSON con las 5 claves pedidas
        String cuerpoJSON = "{"
                + "\"uuid\": \"" + uuid + "\","
                + "\"major\": " + major + ","
                + "\"minor\": " + minor + ","
                + "\"txPower\": " + txPower + ","
                + "\"nombreEmisora\": \"" + nombreEmisora + "\""
                + "}";

        return cuerpoJSON;
    }

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
    //                                      --> enviarMedidaAlServidor() -->
    //
    // Qué hace: manda la medida al servidor REST con un HTTP POST a GuardarMedida.php:
    //
    //   1. COMPRUEBA si la medida es REPETIDA. Si el major y el minor son los mismos
    //      que los de la última vez que salió, no manda nada: el mismo beacon se
    //      anuncia muchas veces por segundo y no tiene sentido machacarlo en el
    //      servidor con copias idénticas.
    //   2. CONSTRUYE el JSON con los 5 campos (construirJSONMedida()).
    //   3. ENVÍA con PeticionarioREST ("POST" + JSON + callback) y, cuando conteste,
    //      escribe por Log el código HTTP y el cuerpo de la respuesta.
    // ---------------------------------------------------------------------------------------------
    public void enviarMedidaAlServidor(String uuid, int major, int minor, int txPower, String nombreEmisora) {

        // (1) AQUÍ SE COMPRUEBA SI LA MEDIDA ESTÁ REPETIDA.
        //     Con "primeraMedida" se distingue "todavía no he enviado nada" de
        //     "la última vez mandé un 0 y un 0", que es un caso legítimo.
        if (primeraMedida == false
                && ultimoMajorEnviado == major
                && ultimoMinorEnviado == minor) {

            Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.enviarMedidaAlServidor(): MEDIDA REPETIDA, no se envía ( major="
                    + major + ", minor=" + minor + " )");
            return;
        }

        // La medida es nueva: me acuerdo de ella para poder filtrar la siguiente.
        this.ultimoMajorEnviado = major;
        this.ultimoMinorEnviado = minor;
        this.primeraMedida = false;

        // (2) AQUÍ SE CONSTRUYE EL JSON.
        String cuerpoJSON = construirJSONMedida(uuid, major, minor, txPower, nombreEmisora);

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.enviarMedidaAlServidor(): JSON = " + cuerpoJSON);

        // Contador para el test automático: se incrementa en el mismo instante en que
        // se decide lanzar la petición, para que el test pueda comprobarlo sin esperar.
        this.peticionesLanzadas++;

        // (3) AQUÍ SE ENVÍA CON PeticionarioREST (POST a GuardarMedida.php).
        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("POST", URL_GUARDAR_MEDIDA, cuerpoJSON,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {

                        // aquí se ve la respuesta del servidor
                        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.enviarMedidaAlServidor(): codigo = " + codigo);
                        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.enviarMedidaAlServidor(): cuerpo = " + cuerpo);

                        // si el test automático está escuchando, le paso la respuesta.
                        if (observadorDePruebas != null) {
                            observadorDePruebas.callback(codigo, cuerpo);
                        }
                    }
                }
        );

    } // ()

    // ---------------------------------------------------------------------------------------------
    // DISEÑO: --> probarEnviarMedidaAlServidor() --x
    //
    // Qué hace: TEST AUTOMÁTICO por consola, sin JUnit ni librerías externas. Encadena
    //           tres comprobaciones con "if" y, cuando llega la respuesta del servidor
    //           (que es asíncrona), decide e imprime el veredicto por Log:
    //
    //             TEST AUTOMÁTICO TELÉFONO OK     -> todas las comprobaciones pasaron
    //             TEST AUTOMÁTICO TELÉFONO ERROR  -> falló al menos una
    //
    //           1. Se manda la medida de prueba y se comprueba que el JSON lleva los
    //              5 campos esperados.
    //           2. Se manda la MISMA medida otra vez y se comprueba que el filtro de
    //              duplicados la paran (el contador de peticiones NO sube).
    //           3. Cuando llega la respuesta del servidor, se comprueba que el código
    //              HTTP es 200 y que el cuerpo trae "error": 0.
    //
    //           Es ESTÁTICA (--x) para poder llamarla desde un botón sin tener que
    //           arrancar antes el servicio.
    //
    //           OJO: pone a cero la memoria del filtro de duplicados para que la
    //           prueba sea siempre igual de repetible. Después de la prueba, la
    //           siguiente medida real volverá a salir normalmente.
    // ---------------------------------------------------------------------------------------------
    public static void probarEnviarMedidaAlServidor() {

        // Datos de la medida de prueba.
        final String uuidPrueba = "EPSG-GTI-PROY-3A";
        final int majorPrueba = 2817;
        final int minorPrueba = 235;
        final int txPowerPrueba = -53;
        final String nombreEmisoraPrueba = "GTI-Jose";

        Log.d(ETIQUETA_LOG, " ----------------------------------------------");
        Log.d(ETIQUETA_LOG, " TEST AUTOMÁTICO TELÉFONO : empieza");
        Log.d(ETIQUETA_LOG, "   mandando a : " + URL_GUARDAR_MEDIDA);
        Log.d(ETIQUETA_LOG, "   uuid=" + uuidPrueba + " major=" + majorPrueba + " minor=" + minorPrueba
                + " txPower=" + txPowerPrueba + " nombreEmisora=" + nombreEmisoraPrueba);

        // Dejo el estado como si no se hubiera enviado nada, para que la prueba
        // sea repetible y no dependa de lo que se haya enviado antes.
        primeraMedida = true;
        ultimoMajorEnviado = 0;
        ultimoMinorEnviado = 0;
        peticionesLanzadas = 0;

        // ------------------------------------------------ COMPROBACIÓN 1: el JSON
        // Se construye el mismo JSON que va a salir y se mira que tenga los 5 campos.
        boolean jsonCorrecto = true;

        String json = construirJSONMedida(uuidPrueba, majorPrueba, minorPrueba, txPowerPrueba, nombreEmisoraPrueba);

        Log.d(ETIQUETA_LOG, "   comprobación 1: JSON = " + json);

        if (json.contains("\"uuid\"")) {
            Log.d(ETIQUETA_LOG, "     OK  el JSON tiene el campo \"uuid\"");
        } else {
            Log.d(ETIQUETA_LOG, "     FALLO el JSON NO tiene el campo \"uuid\"");
            jsonCorrecto = false;
        }

        if (json.contains("\"major\"")) {
            Log.d(ETIQUETA_LOG, "     OK  el JSON tiene el campo \"major\"");
        } else {
            Log.d(ETIQUETA_LOG, "     FALLO el JSON NO tiene el campo \"major\"");
            jsonCorrecto = false;
        }

        if (json.contains("\"minor\"")) {
            Log.d(ETIQUETA_LOG, "     OK  el JSON tiene el campo \"minor\"");
        } else {
            Log.d(ETIQUETA_LOG, "     FALLO el JSON NO tiene el campo \"minor\"");
            jsonCorrecto = false;
        }

        if (json.contains("\"txPower\"")) {
            Log.d(ETIQUETA_LOG, "     OK  el JSON tiene el campo \"txPower\"");
        } else {
            Log.d(ETIQUETA_LOG, "     FALLO el JSON NO tiene el campo \"txPower\"");
            jsonCorrecto = false;
        }

        if (json.contains("\"nombreEmisora\"")) {
            Log.d(ETIQUETA_LOG, "     OK  el JSON tiene el campo \"nombreEmisora\"");
        } else {
            Log.d(ETIQUETA_LOG, "     FALLO el JSON NO tiene el campo \"nombreEmisora\"");
            jsonCorrecto = false;
        }

        // ------------------------------------------------ COMPROBACIÓN 2: sale a la red
        // Pongo un observador que recibirá la respuesta del servidor (es asíncrona).
        // Los otros booleanos se rellenan antes de que llegue.
        //
        // OJO con el orden de los 4 huecos: es el mismo que el número de comprobación
        // que se escribe en cada mensaje del Log. Antes iban descuadrados (la 1ª
        // medida escribía en el hueco 0, que era el del JSON), y por eso un fallo de
        // la petición se camuflaba como un fallo del JSON.
        final boolean[] comprobaciones = new boolean[4];
        comprobaciones[0] = jsonCorrecto;   // comprobación 1: el JSON
        comprobaciones[1] = false;          // comprobación 2: la 1ª medida sale a la red
        comprobaciones[2] = false;          // comprobación 3: la 2ª repetida se filtra
        comprobaciones[3] = false;          // comprobación 4: llega la respuesta del servidor

        observadorDePruebas = new PeticionarioREST.RespuestaREST() {
            @Override
            public void callback(int codigo, String cuerpo) {

                // ------------------------------------------------ COMPROBACIÓN 4: la respuesta
                Log.d(ETIQUETA_LOG, "   comprobación 4: llega la respuesta del servidor");
                Log.d(ETIQUETA_LOG, "     codigo HTTP = " + codigo);
                Log.d(ETIQUETA_LOG, "     cuerpo     = " + cuerpo);

                // Un solo booleano para las dos cosas que hay que mirar de la respuesta
                // (el código HTTP y el "error":0 del cuerpo). Antes se escribía dos
                // veces en el hueco 3, la segunda con un "&& true" que no cambiaba nada
                // y que ademas tapaba el fallo anterior si venía mal el código.
                boolean laRespuestaEsBuena = true;

                if (codigo == 200) {
                    Log.d(ETIQUETA_LOG, "     OK  el servidor ha contestado codigo == 200");
                } else {
                    Log.d(ETIQUETA_LOG, "     FALLO el servidor NO ha contestado codigo == 200 ( ha dado " + codigo + " )");
                    Log.d(ETIQUETA_LOG, "     PISTAS: (1) ¿está levantado el servidor PHP?  (2) ¿has cambiado");
                    Log.d(ETIQUETA_LOG, "             URL_BASE_SERVIDOR por la IP correcta?  (3) ¿el firewall");
                    Log.d(ETIQUETA_LOG, "             de Windows deja pasar el puerto?");
                    laRespuestaEsBuena = false;
                }

                // El PHP responde algo como {"error":0,...}. Se aceptan las dos
                // formas de escribirlo (con y sin espacio detrás de la coma).
                if (cuerpo != null
                        && (cuerpo.contains("\"error\":0") || cuerpo.contains("\"error\": 0"))) {
                    Log.d(ETIQUETA_LOG, "     OK  la respuesta trae \"error\": 0");
                } else {
                    Log.d(ETIQUETA_LOG, "     FALLO la respuesta NO trae \"error\": 0");
                    laRespuestaEsBuena = false;
                }

                comprobaciones[3] = laRespuestaEsBuena;

                // ------------------------------------------------ VEREDICTO
                boolean todoBien = comprobaciones[0] && comprobaciones[1]
                        && comprobaciones[2] && comprobaciones[3];

                if (todoBien) {
                    Log.d(ETIQUETA_LOG, " TEST AUTOMÁTICO TELÉFONO OK");
                } else {
                    Log.d(ETIQUETA_LOG, " TEST AUTOMÁTICO TELÉFONO ERROR");
                }

                // ya no hace falta avisar a nadie más
                observadorDePruebas = null;
            }
        };

        // Se necesita UNA instancia del servicio para poder llamar al método de instancia.
        // No se arranca: sólo se usa para enviar la medida.
        ServicioEscucharBeacons elServicio = new ServicioEscucharBeacons();

        // ------------------------------------------------ 1ª LLAMADA: la medida buena
        elServicio.enviarMedidaAlServidor(uuidPrueba, majorPrueba, minorPrueba, txPowerPrueba, nombreEmisoraPrueba);

        if (peticionesLanzadas == 1) {
            Log.d(ETIQUETA_LOG, "   comprobación 2: OK  la 1ª medida SÍ ha salido a la red (peticiones=" + peticionesLanzadas + ")");
            comprobaciones[1] = true;
        } else {
            Log.d(ETIQUETA_LOG, "   comprobación 2: FALLO la 1ª medida NO ha salido a la red (peticiones=" + peticionesLanzadas + ")");
            comprobaciones[1] = false;
        }

        // ------------------------------------------------ 2ª LLAMADA: la MISMA medida
        // Debe ser filtrada por el filtro de duplicados: el contador NO debe subir.
        elServicio.enviarMedidaAlServidor(uuidPrueba, majorPrueba, minorPrueba, txPowerPrueba, nombreEmisoraPrueba);

        if (peticionesLanzadas == 1) {
            Log.d(ETIQUETA_LOG, "   comprobación 3: OK  la 2ª medida REPETIDA ha sido filtrada (peticiones sigue en " + peticionesLanzadas + ")");
            comprobaciones[2] = true;
        } else {
            Log.d(ETIQUETA_LOG, "   comprobación 3: FALLO la 2ª medida REPETIDA ha salido a la red (peticiones=" + peticionesLanzadas + ")");
            comprobaciones[2] = false;
        }

        // OJO: aquí todavía NO se puede imprimir el veredicto, porque la respuesta del
        // servidor llega más tarde (es asíncrona). El veredicto se imprime dentro del
        // observador de arriba, en el callback.

    } // ()

} // class
