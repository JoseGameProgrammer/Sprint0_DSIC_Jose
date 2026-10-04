<?php

// ==================================================================================================
// DISEÑO LÓGICO: testServidorREST.php (test automático de los endpoints REST por consola)
// --------------------------------------------------------------------------------------------------
//                    --------------- testServidorREST.php ---------------
//                    |
//                     --> arrancarServidorDePruebas() --> [ proceso ]
//                    |
//                     --> peticionHTTP( metodo, ruta, cuerpo ) --> ( codigo, cabeceras, cuerpo )
//                    |
//                     --> probarGuardarMedida() --> B
//                    |
//                     --> probarObtenerMedidas() --> B
//                    |
//                     --> probarErroresDeGuardarMedida() --> B
//                    |
//                     --> probarObtenerMedidasSinParametro() --> B
//                    |
//                     --> probarTiposDeJson() --> B
//                    |
//                     --> probarLosEndpointsNoTienenSql() --> B
//                    |
//                     --> pararServidorDePruebas() -->
//                    |
//                     --> probarServidorREST() --> B
//                    ----------------------------------------------------
//
// QUÉ HACE ESTE SCRIPT
//
// Comprueba, sin PHPUnit ni librerías externas, que los endpoints REST de
// web/rest/ funcionan de verdad, enviando peticiones HTTP DE VERDAD:
//
//   1. POST a /rest/GuardarMedida.php con un JSON de prueba y se comprueba con
//      un "if" que la respuesta trae "error": 0.
//   2. GET a /rest/ObtenerMedidas.php?cuantasComoMaximo=2 y se comprueba que el
//      JSON trae el array "medidas" con los datos esperados.
//   3. Se imprimen "TEST SERVIDOR REST OK" o "TEST SERVIDOR REST ERROR".
//
// CÓMO SE HACEN LAS PETICIONES HTTP DE VERDAD
//
// Ojo con esto, que es lo importante y lo que no se puede hacer de otra forma:
// NO vale con llamar a guardarMedida() directamente, porque entonces se estaría
// probando la lógica, que ya tiene su propio test (web/logica/testLogicaMedidas.php).
// Aquí lo que se prueba es el ENDPOINT, y para probarlo tiene que pasar por
// HTTP de verdad: que llegue una cabecera Content-Type, que php://input traiga
// el cuerpo, que el $_GET se rellene, y que lo que sale sea JSON.
//
// Para eso hay un servidor HTTP de mentira, pero de las de verdad: se arranca
// con "php -S" (el servidor que trae PHP, no hace falta Apache ni XAMPP), que
// escucha en un puerto libre que se busca solo. Al terminar el test se para.
//
// Por qué "php -S" y no un fichero suelto: en consola, php://input SIEMPRE
// devuelve una cadena vacía (está comprobado), porque no hay ninguna petición
// HTTP detrás. Sin un servidor de verdad no se puede probar php://input.
//
// CÓMO SE EJECUTA
//
//     cd web\rest
//     php testServidorREST.php
//
// Y si ya tienes XAMPP levantado y quieres probar contra él:
//
//     php testServidorREST.php --servidor=http://localhost
//
// QUÉ TOCA Y QUÉ NO TOCA
//
// Este test INSERTA medidas de verdad en bdd.sqlite (a través del endpoint, que
// es la gracia), pero al terminar BORRA todas las que ha creado él mismo, así que
// la base de datos de demostración se queda como estaba. Para no liar a nadie,
// las medidas del test llevan un major con números altos (90000 o más) que no
// puede usar ninguna medida real.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

// -------------------------------------------------------------------------------------------------
// Este script es de CONSOLA. Si alguien intenta abrirlo desde el navegador, se corta.
// (Si se pudiera abrir, arrancaría un servidor y dejaría el del test a medias).
// -------------------------------------------------------------------------------------------------
if ( PHP_SAPI !== 'cli' ) {
  header( 'HTTP/1.1 403 Forbidden' );
  die( "testServidorREST.php solo se ejecuta por consola:  php testServidorREST.php" . PHP_EOL );
}

require_once __DIR__ . '/../logica/LogicaMedidas.php';

// =================================================================================================
// UTILIDADES DE IMPRESIÓN POR CONSOLA
// =================================================================================================

function titulo( $texto ) {
  echo PHP_EOL . "======================================================================" . PHP_EOL;
  echo "  " . $texto . PHP_EOL;
  echo "======================================================================" . PHP_EOL;
}

function paso( $texto ) {
  echo PHP_EOL . "--> " . $texto . PHP_EOL;
}

function ok( $texto ) {
  echo "    OK    " . $texto . PHP_EOL;
}

function fallo( $texto ) {
  echo "    FALLO " . $texto . PHP_EOL;
}

// =================================================================================================
// EL SERVIDOR HTTP DE PRUEBAS
// =================================================================================================

// Estas dos variables globales guardan el servidor mientras dure el test.
// Se guardan globales y no como argumentos porque las usan muchas funciones.
$SERVIDOR_PROCESO = null;   // el proceso php -S
$SERVIDOR_PUERTO  = 0;      // el puerto en el que escucha
$SERVIDOR_BASE    = '';     // http://127.0.0.1:<puerto>

// -------------------------------------------------------------------------------------------------
// --> buscarPuertoLibre() --> N
// Qué hace: pide un puerto libre al sistema operativo.
//
// La forma de hacerlo es abrir un socket en el puerto 0 y mirar cuál nos ha
// dado: el sistema elige uno que esté libre. Luego se cierra y ya se sabe qué
// puerto usar.
//
// OJO con la carrera: entre que se cierra el socket y el servidor arranca en
// ese puerto, en teoría otro programa podría cogerlo. En un ordenador normal
// no pasa, y si pasara el test lo diría con un mensaje claro en vez de
// inventarse un "OK".
// -------------------------------------------------------------------------------------------------
function buscarPuertoLibre() {

  $elSocket = @stream_socket_server( 'tcp://127.0.0.1:0', $numeroError, $textoError );

  if ( $elSocket === false ) {
    return 0;
  }

  // Con stream_socket_get_name() se pregunta al socket cuál es su dirección.
  // Devuelve algo como "127.0.0.1:52713", y de ahí solo hace falta el número.
  $laDireccion = stream_socket_get_name( $elSocket, false );

  fclose( $elSocket );

  $laParte = substr( strrchr( $laDireccion, ':' ), 1 );

  return (int) $laParte;

} // ()

// -------------------------------------------------------------------------------------------------
// --> arrancarServidorDePruebas() --> B
// Qué hace: arranca "php -S" sirviendo la carpeta web/ y espera a que responda.
//           Devuelve false si no se ha podido.
//
// OJO: se sirve la carpeta web/ COMPLETA, no solo web/rest/, porque los endpoints
// hacen "require_once '../logica/LogicaMedidas.php'" y, si el servidor no sirve esa
// carpeta por encima, el include no se encuentra y el endpoint peta.
// -------------------------------------------------------------------------------------------------
function arrancarServidorDePruebas() {

  global $SERVIDOR_PROCESO, $SERVIDOR_PUERTO, $SERVIDOR_BASE;

  $puerto = buscarPuertoLibre();

  if ( $puerto === 0 ) {
    fallo( "no se ha podido encontrar un puerto libre" );
    return false;
  }

  $carpetaWeb = realpath( __DIR__ . '/..' );
  $ficheroLog = sys_get_temp_dir() . DIRECTORY_SEPARATOR . 'servidorTestREST.log';

  $mandos = array( PHP_BINARY, '-S', '127.0.0.1:' . $puerto, '-t', $carpetaWeb );

  // Los descriptores 0, 1 y 2 son la entrada, la salida y la salida de error.
  // La 1 y la 2 se mandan a un fichero, y no a la consola, porque si el servidor
  // escribiera por pantalla se mezclaría con el informe del test. Para mirar el
  // log si algo va mal:   notepad %TEMP%\servidorTestREST.log
  $descriptores = array(
    0 => array( 'pipe', 'r' ),
    1 => array( 'file', $ficheroLog, 'a' ),
    2 => array( 'file', $ficheroLog, 'a' ),
  );

  $lasTuberias = array();

  // El array de segunda posición no se usa, pero proc_open() lo exige por firma.
  $elProceso = @proc_open( $mandos, $descriptores, $lasTuberias );

  if ( ! is_resource( $elProceso ) ) {
    fallo( "no se ha podido arrancar 'php -S' (revisa el log " . $ficheroLog . ")" );
    return false;
  }

  $SERVIDOR_PROCESO = $elProceso;
  $SERVIDOR_PUERTO  = $puerto;
  $SERVIDOR_BASE    = 'http://127.0.0.1:' . $puerto;

  // Se espera a que el servidor esté levantado. No se puede suponer que lo esté
  // solo porque proc_open() no ha fallado: el proceso puede tardar un momento en
  // abrir el puerto. Se va probando a_socket a este socket hasta que responda o
  // se agoten los intentos (unos 4 segundos).
  for ( $intento = 0 ; $intento < 40 ; $intento++ ) {

    usleep( 100000 ); // 0,1 segundos

    $elSocket = @fsockopen( '127.0.0.1', $puerto, $numeroError, $textoError, 0.3 );

    if ( $elSocket ) {
      fclose( $elSocket );
      ok( "servidor de pruebas levantado en " . $SERVIDOR_BASE . " (sirviendo " . $carpetaWeb . ")" );
      return true;
    }

  } // ()

  pararServidorDePruebas();

  fallo( "el servidor arrancó pero no llegó a abrir el puerto " . $puerto
         . ". Puede que otro programa lo haya cogido. Revisa " . $ficheroLog );

  return false;

} // ()

// -------------------------------------------------------------------------------------------------
// --> pararServidorDePruebas() -->
// Qué hace: apaga el servidor de pruebas. Se llama SIEMPRE al final, aunque el
//           test haya fallado, o al acabar el script se quedaría un php -S
//          -colgando en segundo plano molestando al resto del tiempo.
// -------------------------------------------------------------------------------------------------
function pararServidorDePruebas() {

  global $SERVIDOR_PROCESO;

  if ( is_resource( $SERVIDOR_PROCESO ) ) {

    proc_terminate( $SERVIDOR_PROCESO );
    proc_close( $SERVIDOR_PROCESO );

    $SERVIDOR_PROCESO = null;

  } // ()

} // ()

// -------------------------------------------------------------------------------------------------
// --> peticionHTTP( metodo: Text, ruta: Text, cuerpo: Text ) --> ( N, [ Text ], Text )
// Qué hace: envía una petición HTTP de verdad al servidor de pruebas y devuelve
//           el triple (codigo, cabeceras, cuerpo).
//
// metodo: "GET" o "POST".
// ruta:   lo que va detrás de http://127.0.0.1:<puerto>, con la barra inicial.
// cuerpo: el texto que se manda en el cuerpo (solo si no es GET). Puede ser
//         un JSON válido o basura a propósito, que también hay que probar.
//
// Se usa file_get_contents() con un contexto de flujo, que es la forma que trae
// PHP de hacerse una petición HTTP sin instalar nada. OJO con el
// "ignore_errors => true": sin eso, si el servidor contesta 400 o 500,
// file_get_contents() devuelve false en vez de dejar leer el cuerpo, y el test
// no podría mirar qué dijo el endpoint.
//
// El cuerpo se devuelve TAL CUAL, sin json_decode, porque una de las
// comprobaciones del test es precisamente ver que lo que sale es JSON de verdad.
// -------------------------------------------------------------------------------------------------
function peticionHTTP( $metodo, $ruta, $cuerpo = null ) {

  global $SERVIDOR_BASE;

  $opciones = array(
    'http' => array(
      'method'        => $metodo,
      'timeout'       => 10,
      'ignore_errors' => true,
    ),
  );

  if ( $cuerpo !== null ) {
    $opciones['http']['header']  = "Content-Type: application/json; charset=utf-8\r\n";
    $opciones['http']['content'] = $cuerpo;
  }

  // Con "false" en el segundo parámetro, file_get_contents() NO convierte los
  // bytes a una array de líneas, los deja como una cadena entera.
  $laRespuesta = @file_get_contents( $SERVIDOR_BASE . $ruta, false,
                                     stream_context_create( $opciones ) );

  if ( $laRespuesta === false ) {

    $elUltimoError = error_get_last();

    return array(
      0,
      array(),
      'NO SE HA PODIDO CONECTAR CON EL SERVIDOR'
          . ( isset( $elUltimoError['message'] ) ? ' (' . $elUltimoError['message'] . ')' : '' ),
    );

  } // ()

  // $http_response_header es una variable MÁGICA que PHP rellena sola con las
  // cabeceras de la última respuesta, siempre que allow_url_fopen esté activo.
  //
  // OJO con el "global" que NO lleva, y es lo importante: esta variable se crea
  // en el ÁMBITO LOCAL de la función donde se ha llamado a file_get_contents(),
  // no en el global. Si se leyera con "global $http_response_header" se leería
  // una variable global VACÍA y el código HTTP saldría siempre 0, que es
  // justo el fallo que hubo la primera vez. Aquí se lee tal cual, en local.
  $lasCabeceras = isset( $http_response_header ) ? $http_response_header : array();

  // En las cabeceras HTTP cada una es una línea "Clave: valor". El código HTTP
  // es la primera, con este formato: "HTTP/1.1 200 OK".
  $elCodigo = 0;

  if ( count( $lasCabeceras ) > 0
       && preg_match( '#^HTTP/\S+\s+(\d{3})#', $lasCabeceras[0], $loEncontrado ) === 1 ) {
    $elCodigo = (int) $loEncontrado[1];
  } // ()

  return array( $elCodigo, $lasCabeceras, $laRespuesta );

} // ()

// -------------------------------------------------------------------------------------------------
// --> cabecera( cabeceras: [ Text ], nombre: Text ) --> Text
// Qué hace: busca el valor de una cabecera en la respuesta HTTP.
//
// Se comparan los nombres SIN importar las mayúsculas, porque HTTP no
// distingue entre "Content-Type" y "content-type", y cada servidor lo escribe
// a su manera.
// -------------------------------------------------------------------------------------------------
function cabecera( $lasCabeceras, $nombreBuscado ) {

  foreach ( $lasCabeceras as $unaCabecera ) {

    $losDosLados = explode( ':', $unaCabecera, 2 );

    if ( count( $losDosLados ) === 2
         && strtolower( trim( $losDosLados[0] ) ) === strtolower( $nombreBuscado ) ) {
      return trim( $losDosLados[1] );
    } // ()

  } // ()

  return '';

} // ()

// =================================================================================================
// COMPROBACIÓN 1: EL POST A GuardarMedida.php
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> probarGuardarMedida() --> B
// Qué hace: manda un POST con un JSON de prueba y comprueba con un "if" que la
//           respuesta trae "error": 0, tal y como pide el enunciado.
// -------------------------------------------------------------------------------------------------
function probarGuardarMedida() {

  // La medida de prueba. El major va en 90000 para que el test pueda reconocer
  // después sus propias filas y borrarlas, sin tocar los datos de los demás.
  $medida = array(
    'uuid'          => 'EPSG-GTI-PROY-3A',
    'major'         => 90001,
    'minor'         => 235,
    'txPower'       => -53,
    'nombreEmisora' => 'GTI-Jose',
  );

  paso( "1. POST /rest/GuardarMedida.php con un JSON de prueba" );

  echo "       envío: " . json_encode( $medida, JSON_UNESCAPED_UNICODE ) . PHP_EOL;

  list( $codigo, $cabeceras, $cuerpo ) = peticionHTTP(
          'POST', '/rest/GuardarMedida.php', json_encode( $medida, JSON_UNESCAPED_UNICODE ) );

  echo "       respuesta: HTTP " . $codigo . "  " . trim( $cuerpo ) . PHP_EOL;

  $todoBien = true;
  // --- el código HTTP debe ser 200 -------------------------------------------------
  // Ojo: el endpoint responde 200 SIEMPRE, y el error va dentro del JSON. El
  // cliente Android comprueba el 200, así que tiene que serlo.
  if ( $codigo === 200 ) {
    ok( "el endpoint ha contestado HTTP 200" );
  } else {
    fallo( "el endpoint NO ha contestado HTTP 200, ha contestado " . $codigo );
    $todoBien = false;
  } // ()

  // --- la cabecera Content-Type debe decir que es JSON ----------------------------
  $laCabecera = cabecera( $cabeceras, 'Content-Type' );

  if ( stripos( $laCabecera, 'application/json' ) !== false ) {
    ok( "la cabecera Content-Type es '" . $laCabecera . "'" );
  } else {
    fallo( "la cabecera Content-Type NO es application/json, es '" . $laCabecera . "'" );
    $todoBien = false;
  } // ()

  // --- el cuerpo tiene que ser un JSON que se pueda decodificar --------------------
  $decodificado = json_decode( $cuerpo, true );

  if ( json_last_error() === JSON_ERROR_NONE && is_array( $decodificado ) ) {
    ok( "el cuerpo de la respuesta es un JSON válido" );
  } else {
    fallo( "el cuerpo de la respuesta NO es un JSON válido: " . json_last_error_msg() );
    return false; // sin JSON no tiene sentido seguir mirando
  } // ()

  // --- AQUÍ ESTÁ EL "if" QUE PIDE EL ENUNCIADO: error = 0 -------------------------
  if ( isset( $decodificado['error'] ) && $decodificado['error'] === 0 ) {
    ok( "el JSON trae \"error\": 0" );
  } else {
    fallo( "el JSON NO trae \"error\": 0, trae: "
           . ( isset( $decodificado['error'] ) ? var_export( $decodificado['error'], true ) : '(nada)' ) );
    $todoBien = false;
  } // ()

  // --- y el mensaje, para que el móvil lo pueda enseñar -----------------------------
  if ( isset( $decodificado['mensaje'] ) && trim( $decodificado['mensaje'] ) !== '' ) {
    ok( "el JSON trae un \"mensaje\": \"" . $decodificado['mensaje'] . "\"" );
  } else {
    fallo( "el JSON NO trae un \"mensaje\" con texto" );
    $todoBien = false;
  } // ()

  // ---------------------------------------------------------------------------------------------
  // 1b) SE GUARDAN 3 MEDIDAS MÁS
  // ---------------------------------------------------------------------------------------------
  // OJO con por qué: la comprobación 2 va a pedir 2 medidas y a comprobar que
  // salen MENOS de las que hay. Si la base de datos tuviera solo 1 o 2 medidas,
  // esa comprobación pasaría igual, porque pedir 2 y devolver 2
  // también encaja con "el endpoint se ha comido el límite". Con 4 medidas
  // guardadas, pedir 2 y devolver 4 demuestra que el límite funciona de verdad.
  paso( "1b. Guardo 3 medidas más, para tener 4 y poder comprobar el límite" );

  $medidasDeMas = array(
    array( 'major' => 90002, 'minor' => 22 ),
    array( 'major' => 90003, 'minor' => 33 ),
    array( 'major' => 90004, 'minor' => 44 ),
  );

  foreach ( $medidasDeMas as $unaDeMas ) {

    $laMedida = array(
      'uuid'          => 'EPSG-GTI-PROY-3A',
      'major'         => $unaDeMas['major'],
      'minor'         => $unaDeMas['minor'],
      'txPower'       => -53,
      'nombreEmisora' => 'GTI-Jose',
    );

    list( $codigo, $cabeceras, $cuerpo ) = peticionHTTP(
            'POST', '/rest/GuardarMedida.php', json_encode( $laMedida, JSON_UNESCAPED_UNICODE ) );

    $decodificado = json_decode( $cuerpo, true );

    if ( $codigo === 200 && isset( $decodificado['error'] ) && $decodificado['error'] === 0 ) {
      ok( "guardada la medida major=" . $laMedida['major'] . " -> {\"error\":0}" );
    } else {
      fallo( "no se ha podido guardar la medida major=" . $laMedida['major']
             . ": HTTP " . $codigo . "  " . trim( $cuerpo ) );
      $todoBien = false;
    } // ()

  } // ()

  return $todoBien;

} // ()

// -------------------------------------------------------------------------------------------------
// --> probarObtenerMedidas() --> B
// Qué hace: llama a GET /rest/ObtenerMedidas.php?cuantasComoMaximo=2 y comprueba
//           que el JSON trae el array "medidas" con los datos esperados.
// -------------------------------------------------------------------------------------------------
function probarObtenerMedidas() {

  paso( "2. GET /rest/ObtenerMedidas.php?cuantasComoMaximo=2" );

  list( $codigo, $cabeceras, $cuerpo ) = peticionHTTP(
          'GET', '/rest/ObtenerMedidas.php?cuantasComoMaximo=2' );

  echo "       respuesta: HTTP " . $codigo . "  " . substr( trim( $cuerpo ), 0, 200 )
       . ( strlen( $cuerpo ) > 200 ? '...' : '' ) . PHP_EOL;

  $todoBien = true;

  if ( $codigo === 200 ) {
    ok( "el endpoint ha contestado HTTP 200" );
  } else {
    fallo( "el endpoint NO ha contestado HTTP 200, ha contestado " . $codigo );
    $todoBien = false;
  } // ()

  $decodificado = json_decode( $cuerpo, true );

  if ( json_last_error() === JSON_ERROR_NONE && is_array( $decodificado ) ) {
    ok( "el cuerpo de la respuesta es un JSON válido" );
  } else {
    fallo( "el cuerpo de la respuesta NO es un JSON válido: " . json_last_error_msg() );
    return false;
  } // ()

  // --- tiene que existir la clave "medidas" y ser un array -------------------------
  if ( ! isset( $decodificado['medidas'] ) || ! is_array( $decodificado['medidas'] ) ) {
    fallo( "el JSON NO trae un array \"medidas\": " . trim( $cuerpo ) );
    return false;
  } // ()

  ok( "el JSON trae la clave \"medidas\", que es un array de "
       . count( $decodificado['medidas'] ) . " medidas" );

  // --- se han pedido 2, y hay 4 guardadas, así que TIENEN que salir 2 --------------
  // OJO: la comprobación es de igualdad exacta, no de "menos o igual que". Con un
  // "<= 2" la comprobación pasaría por los pelos aunque el endpoint se hubiera
  // comido el límite y devuelto las 4... no, si devolviera 4 no pasaría. Lo que
  // no detectaría un "<= 2" es que devolviera 0 o 1 por un fallo suyo. Con la
  // igualdad exacta se comprueba las dos cosas de golpe: que no devuelve de más
  // y que no devuelve de menos.
  $lasMedidas = $decodificado['medidas'];

  if ( count( $lasMedidas ) === 2 ) {
    ok( "se han pedido 2, hay 4 guardadas y han salido exactamente 2, el límite se respeta" );
  } else {
    fallo( "se han pedido 2 medidas, hay 4 guardadas y han salido "
           . count( $lasMedidas ) . ". Deberían salir exactamente 2" );
    $todoBien = false;
  } // ()

  if ( count( $lasMedidas ) === 0 ) {
    fallo( "no ha salido ninguna medida, y la comprobación 1 acaba de guardar cuatro" );
    return false;
  } // ()

  // --- cada elemento tiene que ser una Medida con sus 7 claves -------------------
  $clavesMedida = array( 'id', 'uuid', 'major', 'minor', 'txPower', 'nombreEmisora', 'fechaLectura' );
  $faltan = array();

  foreach ( $lasMedidas as $laMedida ) {

    if ( ! is_array( $laMedida ) ) {
      $faltan[] = 'un elemento que no es un objeto';
      continue;
    } // ()

    $faltan = array_merge( $faltan, array_diff( $clavesMedida, array_keys( $laMedida ) ) );

  } // ()

  if ( count( $faltan ) === 0 ) {
    ok( "las medidas traídas tienen las 7 claves de Medida ("
         . implode( ', ', $clavesMedida ) . ")" );
  } else {
    fallo( "faltan claves en las medidas: " . implode( ', ', array_unique( $faltan ) ) );
    $todoBien = false;
  } // ()

  // --- los DATOS deben ser los que se guardaron, Y EN EL ORDEN CORRECTO ----------
  // Esta es la comprobación de verdad: no basta con que el JSON tenga la forma
  // correcta, tiene que traer las medidas que se acaban de guardar.
  //
  // Se guardaron, en este orden, los majors 90001, 90002, 90003 y 90004. Al pedir
  // las 2 últimas tiene que salir la MÁS NUEVA primero, o sea 90004 y luego
  // 90003. Si salieran al revés, el endpoint estaría devolviendo el orden
  // equivocado aunque el JSON pareciese correcto.
  $esperadas = array(
    array( 'uuid' => 'EPSG-GTI-PROY-3A', 'major' => 90004, 'minor' => 44, 'txPower' => -53, 'nombreEmisora' => 'GTI-Jose' ),
    array( 'uuid' => 'EPSG-GTI-PROY-3A', 'major' => 90003, 'minor' => 33, 'txPower' => -53, 'nombreEmisora' => 'GTI-Jose' ),
  );

  $datosCoinciden = true;

  foreach ( $esperadas as $posicion => $esperado ) {

    if ( ! isset( $lasMedidas[$posicion] ) ) {
      fallo( "no ha salido la medida que debería ir en la posición " . $posicion );
      $datosCoinciden = false;
      continue;
    } // ()

    $laMedida = $lasMedidas[$posicion];

    foreach ( $esperado as $campo => $valorEsperado ) {

      if ( ! array_key_exists( $campo, $laMedida ) ) {
        fallo( "en la medida de la posición " . $posicion . " falta el campo \"" . $campo . "\"" );
        $datosCoinciden = false;
        continue;
      } // ()

      if ( $laMedida[$campo] !== $valorEsperado ) {

        // OJO: los números del JSON llegan como int y los de $valorEsperado
        // también, así que el !== es honesto. Si algún día el endpoint devolviera
        // "major":"90004" (con comillas), este if lo cantaría, que es exactamente
        // lo que hay que vigilar: que un tipo N no viaje como texto.
        fallo( "en la posición " . $posicion . ", el campo \"" . $campo . "\" vale "
               . var_export( $laMedida[$campo], true ) . " y se esperaba "
               . var_export( $valorEsperado, true ) );
        $datosCoinciden = false;

      } // ()

    } // ()

  } // ()

  if ( $datosCoinciden ) {
    ok( "las 2 medidas son las que se guardaron y vienen en el orden correcto"
         . " (la más nueva, major=" . $lasMedidas[0]['major']
         . ", y después major=" . $lasMedidas[1]['major'] . ")" );
  } // ()

  $laPrimera = $lasMedidas[0];

  // --- el id tiene que ser un número, no texto ------------------------------------
  if ( ! is_int( $laPrimera['id'] ) ) {
    fallo( "el campo \"id\" debería ser un número, es " . gettype( $laPrimera['id'] ) );
    $todoBien = false;
  } // ()

  // OJO con el $datosCoinciden al final del return: si se olvidara aquí, el test
  // imprimiría sus FALLO de "el campo X vale Y y se esperaba Z" y aun así daría
  // el veredicto de OK, porque $todoBien seguiría siendo true. Ya pasó una vez
  // al escribir este test, por eso está escrito y comentado.
  return $todoBien && $datosCoinciden;

} // ()

// =================================================================================================
// COMPROBACIONES EXTRA: LOS ERRORES QUE TIENEN QUE RESPONDER error: 1
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> probarErroresDeGuardarMedida() --> B
// Qué hace: manda cuatro peticiones MAL FORMADAS a propósito y comprueba que en
//           los cuatro casos el endpoint contesta "error": 1 y no revienta.
//
// Esta comprobación es la que de verdad demuestra que la validación del endpoint
// está bien: un endpoint que contesta "error": 0 a cualquier cosa, aunque el
// enunciado no lo pida, es un endpoint que no valida nada.
// -------------------------------------------------------------------------------------------------
function probarErroresDeGuardarMedida() {

  paso( "3. Las peticiones que TIENEN que responder \"error\": 1" );

  // Los casos: un título para el informe, el cuerpo que se manda, y si es un POST.
  $casos = array(
    array( 'sin ningún campo',                  '{}' ),
    array( 'faltando "txPower"',                '{"uuid":"A","major":1,"minor":2,"nombreEmisora":"B"}' ),
    array( 'faltando "uuid" y "nombreEmisora"', '{"major":1,"minor":2,"txPower":-3}' ),
    array( 'JSON roto de verdad',               '{"uuid":"A", "major":' ),
    array( 'texto que no es JSON',              'esto no es un JSON' ),
    array( 'cuerpo vacío',                      '' ),
    array( 'un número suelto en vez de objeto', '5' ),
    array( 'uuid vacío (lo rechaza la lógica)', '{"uuid":"","major":1,"minor":2,"txPower":-3,"nombreEmisora":"B"}' ),
    array( 'major con letras (lo rechaza la lógica)', '{"uuid":"A","major":"abc","minor":2,"txPower":-3,"nombreEmisora":"B"}' ),
  );

  $todoBien = true;

  foreach ( $casos as $elCaso ) {

    list( $tituloCaso, $cuerpoCaso ) = $elCaso;

    list( $codigo, $cabeceras, $cuerpo ) = peticionHTTP( 'POST', '/rest/GuardarMedida.php', $cuerpoCaso );

    $decodificado = json_decode( $cuerpo, true );

    if ( $codigo !== 200 ) {
      fallo( $tituloCaso . ": el endpoint ha contestado HTTP " . $codigo . ", debería contestar 200" );
      $todoBien = false;
      continue;
    } // ()

    if ( json_last_error() !== JSON_ERROR_NONE || ! is_array( $decodificado ) ) {
      fallo( $tituloCaso . ": la respuesta no es un JSON válido: " . trim( $cuerpo ) );
      $todoBien = false;
      continue;
    } // ()

    if ( isset( $decodificado['error'] ) && $decodificado['error'] === 1 ) {
      ok( $tituloCaso . " -> {\"error\":1}" );
    } else {
      fallo( $tituloCaso . ": esperaba \"error\": 1 y ha venido "
             . ( isset( $decodificado['error'] ) ? var_export( $decodificado['error'], true ) : '(nada)' )
             . "  cuerpo: " . trim( $cuerpo ) );
      $todoBien = false;
    } // ()

  } // ()

  // Un GET al endpoint de guardar también tiene que ser un error controlado.
  paso( "3b. Un GET a GuardarMedida.php también debe responder \"error\": 1\"" );

  list( $codigo, $cabeceras, $cuerpo ) = peticionHTTP( 'GET', '/rest/GuardarMedida.php' );

  $decodificado = json_decode( $cuerpo, true );

  if ( $codigo === 200 && isset( $decodificado['error'] ) && $decodificado['error'] === 1 ) {
    ok( "un GET a GuardarMedida.php -> {\"error\":1}, controlado y sin avisos de PHP" );
  } else {
    fallo( "un GET a GuardarMedida.php debería contestar {\"error\":1}, ha contestado: " . trim( $cuerpo ) );
    $todoBien = false;
  } // ()

  return $todoBien;

} // ()

// =================================================================================================
// COMPROBACIONES EXTRA: EL GET SIN PARÁMETRO Y LOS TIPOS DEL JSON
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> probarObtenerMedidasSinParametro() --> B
// Qué hace: llama al GET SIN "cuantasComoMaximo" en la URL y comprueba que
//           funciona y sale con el valor por defecto de 100, es decir, que
//           devuelve TODAS las medidas que haya.
//
// En esta BBDD hay 4 medidas del test y 1 de la demostración, así que tienen
// que salir 5. Si salieran menos, el valor por defecto de 100 no se estaría
// aplicando como dice el diseño.
// -------------------------------------------------------------------------------------------------
function probarObtenerMedidasSinParametro() {

  paso( "4. GET /rest/ObtenerMedidas.php SIN el parámetro cuantasComoMaximo" );

  list( $codigo, $cabeceras, $cuerpo ) = peticionHTTP( 'GET', '/rest/ObtenerMedidas.php' );

  $decodificado = json_decode( $cuerpo, true );

  if ( $codigo !== 200
       || json_last_error() !== JSON_ERROR_NONE
       || ! is_array( $decodificado )
       || ! isset( $decodificado['medidas'] )
       || ! is_array( $decodificado['medidas'] ) ) {

    fallo( "sin parámetro debería contestar 200 con un array \"medidas\", ha contestado: " . trim( $cuerpo ) );
    return false;

  } // ()

  $cuantasHanSalido = count( $decodificado['medidas'] );

  // Con el valor por defecto de 100 tienen que salir todas, y en esta BBDD hay 5.
  if ( $cuantasHanSalido === 5 ) {
    ok( "sin parámetro salen las 5 medidas, con el valor por defecto de 100" );
    return true;
  } // ()

  if ( $cuantasHanSalido > 2 ) {
    ok( "sin parámetro salen " . $cuantasHanSalido
         . " medidas, más de 2, así que el valor por defecto de 100 se está aplicando" );
    return true;
  } // ()

  fallo( "sin parámetro solo han salido " . $cuantasHanSalido
         . " medidas, y deberían salir todas (4 del test + 1 de demostración)" );

  return false;

} // ()

// -------------------------------------------------------------------------------------------------
// --> probarParametrosRareos() --> B
// Qué hace: manda GET con valores absurdos en "cuantasComoMaximo" y comprueba que
//           el endpoint NO revienta, ni avise por pantalla, y conteste siempre un
//           array "medidas" (vacío si el valor no sirve).
//
// Un parámetro raro no puede tirar el servidor: eso lo decide la lógica, en
// obtenerMedidas(), que devuelve una lista vacía. El endpoint no revalida.
// -------------------------------------------------------------------------------------------------
function probarParametrosRareos() {

  paso( "5. GET con \"cuantasComoMaximo\" raro no debe reventar el servidor" );

  $casos = array( 'abc', '0', '-5', '3.7', 'mil', '99999999999999999999' );

  $todoBien = true;

  foreach ( $casos as $elCaso ) {

    list( $codigo, $cabeceras, $cuerpo ) = peticionHTTP(
            'GET', '/rest/ObtenerMedidas.php?cuantasComoMaximo=' . rawurlencode( $elCaso ) );

    $decodificado = json_decode( $cuerpo, true );

    if ( $codigo === 200
         && json_last_error() === JSON_ERROR_NONE
         && is_array( $decodificado )
         && isset( $decodificado['medidas'] )
         && is_array( $decodificado['medidas'] ) ) {

      ok( "cuantasComoMaximo=" . $elCaso . " -> HTTP 200 con " . count( $decodificado['medidas'] )
           . " medidas, sin reventar" );

    } else {

      fallo( "cuantasComoMaximo=" . $elCaso . " ha contestado HTTP " . $codigo . " con: " . trim( $cuerpo ) );
      $todoBien = false;

    } // ()

    // Si el PHP soltara un aviso (Warning o Notice) ANTES del JSON, el cuerpo
    // empezaría por texto y el json_decode de arriba fallaría. Que pase la
    // comprobación ya demuestra que no hay avisos delante del JSON.
    if ( stripos( $cuerpo, 'Warning' ) !== false
         || stripos( $cuerpo, 'Notice' ) !== false
         || stripos( $cuerpo, 'Deprecated' ) !== false
         || stripos( $cuerpo, 'Fatal error' ) !== false ) {

      fallo( "cuantasComoMaximo=" . $elCaso . " ha soltado un aviso de PHP dentro de la respuesta" );
      $todoBien = false;

    } // ()

  } // ()

  return $todoBien;

} // ()

// -------------------------------------------------------------------------------------------------
// --> probarLosEndpointsNoTienenSql() --> B
// Qué hace: mira el código de los dos endpoints y comprueba que NO hay SQL
//           dentro. Lo dice el enunciado ("No deben contener sentencias SQL
//           directamente") y es la regla que mantiene la separación de capas.
//
// Ojo con el filtro de comentarios: en los comentarios de los endpoints se
// escriben palabras como "SQL" o "sentencias" al explicar POR QUÉ no hay SQL.
// Si no se quitaran los comentarios antes de mirar, el test se saltaría solo.
// -------------------------------------------------------------------------------------------------
function probarLosEndpointsNoTienenSql() {

  paso( "6. Los endpoints NO deben contener sentencias SQL" );

  $endpoints = array( 'GuardarMedida.php', 'ObtenerMedidas.php' );

  // Palabras que solo pueden aparecer si hay SQL de verdad. "require_once" y
  // "LogicaMedidas" se permiten, porque el endpoint NECESITA llamar a la lógica.
  $prohibidas = array(
    'SELECT',
    'INSERT',
    'UPDATE',
    'DELETE',
    'DROP',
    'ALTER',
    'CREATE TABLE',
    'FROM Medidas',
    'INTO Medidas',
    'new PDO',
    'sqlite:',
  );

  $todoBien = true;

  foreach ( $endpoints as $nombreEndpoint ) {

    $contenido = file_get_contents( __DIR__ . '/' . $nombreEndpoint );

    // Se quitan los comentarios de una línea (// y #) y los de bloque (/* */),
    // para no mirar lo que solo está escrito en la documentación.
    $soloCodigo = preg_replace( '#/\*.*?\*/#s', '', $contenido );
    $soloCodigo = preg_replace( '#^\s*(//|\#).*$#m', '', $soloCodigo );

    $encontrado = array();

    foreach ( $prohibidas as $prohibida ) {

      if ( stripos( $soloCodigo, $prohibida ) !== false ) {
        $encontrado[] = $prohibida;
      } // ()

    } // ()

    if ( count( $encontrado ) === 0 ) {
      ok( $nombreEndpoint . " no tiene SQL: solo traduce HTTP a llamadas de la lógica" );
    } else {
      fallo( $nombreEndpoint . " tiene SQL dentro: " . implode( ', ', $encontrado )
             . ". El SQL solo puede estar en ../logica/LogicaMedidas.php" );
      $todoBien = false;
    } // ()

  } // ()

  return $todoBien;

} // ()

// =================================================================================================
// LIMPIEZA: BORRAR LAS MEDIDAS QUE HA CREADO ESTE TEST
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> borrarLasMedidasDelTest() --> N
// Qué hace: borra de bdd.sqlite las medidas que ha creado este test y devuelve
//           cuántas ha borrado.
//
// Se distinguen por el major: el test usa majors de 90000 para arriba, que es
// un rango que no puede usar ninguna medida real, así que es imposible que se
// lleve por delante datos de otra persona.
//
// OJO: borra por valor de major y no por id, a propósito. Si se guardara por
// id y el test se ejecutara dos veces, la segunda podría quedarse con ids
// altos y acabar borrando la fila buena de la demostración.
// -------------------------------------------------------------------------------------------------
function borrarLasMedidasDelTest() {

  $bdd = conectarBBDD();

  $sentencia = $bdd->prepare( 'DELETE FROM Medidas WHERE Major >= :menor' );
  $sentencia->bindValue( ':menor', 90000, PDO::PARAM_INT );
  $sentencia->execute();

  return $sentencia->rowCount();

} // ()

// =================================================================================================
// SE EJECUTAN TODAS LAS PRUEBAS
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> probarServidorREST() --> B
// Qué hace: el hilo conductor. Arranca el servidor, hace todas las
//           comprobaciones, lo para y devuelve si todo ha ido bien.
//
// Lo metido en una función, y no suelto en el cuerpo del script, es para
// garantizar que el servidor se para SIEMPRE, incluso si alguna comprobación
// peta. Si el "parar" se escribiera al final del script, un fallo intermedio
// dejaría un php -S colgado en segundo plano.
// -------------------------------------------------------------------------------------------------
function probarServidorREST() {

  paso( "0. Arranco el servidor HTTP de pruebas" );

  if ( ! arrancarServidorDePruebas() ) {
    // Sin servidor no se puede probar nada, y no tiene sentido seguir.
    return false;
  } // ()

  $comprobaciones = array();

  $comprobaciones[] = probarGuardarMedida();
  $comprobaciones[] = probarObtenerMedidas();
  $comprobaciones[] = probarErroresDeGuardarMedida();
  $comprobaciones[] = probarObtenerMedidasSinParametro();
  $comprobaciones[] = probarParametrosRareos();
  $comprobaciones[] = probarLosEndpointsNoTienenSql();

  // Se para el servidor ANTES de limpiar la base de datos, para que no se quede
  // ningún proceso de php -S con la base de datos abierta.
  paso( "7. Paro el servidor de pruebas" );
  pararServidorDePruebas();
  ok( "servidor parado" );

  paso( "8. Limpio las medidas que ha creado este test" );

  $borradas = borrarLasMedidasDelTest();

  if ( $borradas > 0 ) {
    ok( "se han borrado " . $borradas . " medidas del test (las de major 90000 o superior)" );
  } else {
    ok( "no ha hecho falta borrar nada" );
  } // ()

  $fallos = 0;

  foreach ( $comprobaciones as $nota ) {
    if ( $nota === false ) {
      $fallos++;
    } // ()
  } // ()

  echo PHP_EOL . "    Comprobaciones: " . count( $comprobaciones ) . "   Fallos: " . $fallos . PHP_EOL;

  return $fallos === 0;

} // ()

// -------------------------------------------------------------------------------------------------
// Programa principal.
// -------------------------------------------------------------------------------------------------
titulo( "TEST AUTOMATICO DEL SERVIDOR REST - web/rest" );

// ¿Se ha pedido probar contra un servidor que ya esté levantado?
//   php testServidorREST.php --servidor=http://localhost
// Con esa opción NO se arranca ningún servidor: se usa el que haya.
// Es lo que hay que usar si la máquina tiene Apache de XAMPP, porque así se
// comprueba el servidor de verdad y no el de php -S.
$unServidorExterno = '';

foreach ( $argv as $unArgumento ) {

  if ( strpos( $unArgumento, '--servidor=' ) === 0 ) {
    $unServidorExterno = substr( $unArgumento, strlen( '--servidor=' ) );
  } // ()

} // ()

$todoBien = false;

if ( $unServidorExterno !== '' ) {

  $SERVIDOR_BASE = rtrim( $unServidorExterno, '/' ) . '/rest';
  $SERVIDOR_PUERTO = 0;

  paso( "0. Uso el servidor que me han pasado por parámetro: " . $SERVIDOR_BASE );

  $todoBien = probarServidorREST();

} else {

  $todoBien = probarServidorREST();

} // ()

titulo( $todoBien ? 'TEST SERVIDOR REST OK' : 'TEST SERVIDOR REST ERROR' );

// Código de salida 0 = todo bien, 1 = algún fallo (para poder encadenarlo).
exit( $todoBien ? 0 : 1 );

?>
