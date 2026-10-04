<?php

// ==================================================================================================
// DISEÑO LÓGICO: endpoint REST GuardarMedida.php
// --------------------------------------------------------------------------------------------------
//
//   Método y ruta:  POST  ../rest/GuardarMedida.php
//
//   ENTRADA (cuerpo JSON de la petición):
//   {
//       uuid: Text,
//       major: N,
//       minor: Z,
//       txPower: Z,
//       nombreEmisora: Text
//   }
//
//   LLAMADA A LA LÓGICA (esta es la única función de negocio a la que llama):
//   uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
//
//   SALIDA (cuerpo JSON de la respuesta):
//   - Si B = true:   { "error": 0, "mensaje": Text }
//   - Si B = false:  { "error": 1, "mensaje": Text }
//
// --------------------------------------------------------------------------------------------------
// QUÉ ES ESTE FICHERO
//
// Esto NO es la lógica del negocio, es el PORTERO. Su único trabajo es:
//
//     1. Leer la petición HTTP (el cuerpo JSON que ha mandado el cliente).
//     2. Sacar de ese JSON los 5 datos y pasárselos a guardarMedida().
//     3. Traducir el true/false que devuelve la lógica a un JSON.
//
// OJO con lo que NO hay aquí dentro: ni una sola sentencia SQL. Este fichero no
// sabe qué columnas tiene la tabla Medidas, ni cómo se llama siquiera. Solo sabe
// traducir HTTP a "llamar a guardarMedida() con estos 5 datos". Toda la base de
// datos vive en ../logica/LogicaMedidas.php. Si algún día el negocio cambia, este
// fichero no se toca.
//
// ESTE ES EL MOTIVO DE QUE LA CAPA LÓGICA ESTÉ SEPARADA. El día que el cliente
// deje de ser este móvil y sea una página web, el endpoint es el MISMO: lo único
// que cambia es quién llama.
//
// --------------------------------------------------------------------------------------------------
// OJO CON LOS "error" DEL DISEÑO
//
// "error: 0" significa "todo bien" y "error: 1" significa "ha ido mal". Es un
// 0/1 al revés, sí, pero es lo que pide el enunciado y es lo que espera el
// cliente Android (busca literalmente la cadena "error":0 en la respuesta).
//
// OJO TAMBIÉN CON EL CÓDIGO HTTP: este endpoint responde SIEMPRE con 200, incluso
// cuando hay error, y el error va DENTRO del JSON en el campo "error". No se
// devuelve 400 ni 500 a propósito, porque el cliente Android comprueba que el
// código sea 200 y luego lee el campo "error" del cuerpo. Si se devolviera un 400,
// el cliente Android lo trataría como fallo de red y ni siquiera miraría el JSON.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

require_once '../logica/LogicaMedidas.php';

// Cabecera de respuesta: todo lo que devuelva este endpoint es JSON en UTF-8.
// Tiene que ser lo PRIMERO que se escriba, antes de ningún echo.
header('Content-Type: application/json; charset=utf-8');

/*
 * Imprime la respuesta y termina el script.
 *
 * Se mete en una función aparte porque hay varios caminos de salida (petición
 * que no es POST, JSON roto, campos que faltan, resultado de la lógica) y todos
 * tienen que terminar de la misma manera: sueltar el JSON y salir. Si se
 * escribiera "echo" suelto en cada caso, un fallo de última hora podría dejar
 * medio JSON impreso.
 *
 * @param int    $codigoError 0 = todo bien, 1 = error.
 * @param string $mensaje     Texto que verá el cliente.
 */
function responder( $codigoError, $mensaje ) {

  // El array se monta en el ORDEN en el que se quiere que salga el JSON:
  // primero "error" y después "mensaje", para que se lea igual que el diseño.
  $respuesta = array(
    'error'   => (int) $codigoError,
    'mensaje' => (string) $mensaje,
  );

  // JSON_UNESCAPED_UNICODE para que "Añón" salga como "Añón" y no como "A\u00f1on".
  // JSON_UNESCAPED_SLASHES para que las "/" no salgan como "\/". Con esto el
  // cuerpo es JSON válido y además se lee tal cual, que es lo que espera un
  // navegador o un móvil.
  echo json_encode( $respuesta, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES );

  exit;

} // ()


// -------------------------------------------------------------------------------------------------
// 1) LECTURA DE LA PETICIÓN
// -------------------------------------------------------------------------------------------------

// El método tiene que ser POST. Si alguien abre la URL en el navegador, el
// método será GET y no habrá cuerpo: sin esta comprobación, json_decode("")
// daría error y el mensaje sería engañoso ("Error al guardar la medida") cuando
// el problema de verdad es que se ha llamado con el método equivocado.
if ( ! isset( $_SERVER['REQUEST_METHOD'] ) || $_SERVER['REQUEST_METHOD'] !== 'POST' ) {

  error_log( 'GuardarMedida.php: método ' . ( isset($_SERVER['REQUEST_METHOD']) ? $_SERVER['REQUEST_METHOD'] : '(ninguno)' ) . ' en vez de POST' );

  responder( 1, 'Error al guardar la medida' );

} // ()

// El cuerpo de la petición se lee de php://input, que es el sitio donde PHP
// guarda lo que el cliente ha mandado en el cuerpo del POST.
//
// OJO: php://input se puede leer UNA SOLA VEZ. Si el endpoint leyera dos veces,
// la segunda devolvería una cadena vacía. Por eso se lee aquí una vez y se
// guarda en una variable.
$cuerpoPeticion = file_get_contents( 'php://input' );

// Un cuerpo vacío (petición sin cuerpo, o alguien que llama al endpoint a pelo)
// no es un JSON válido: se corta aquí para no seguir con un null por delante.
if ( $cuerpoPeticion === false || trim( $cuerpoPeticion ) === '' ) {

  error_log( 'GuardarMedida.php: llega el cuerpo de la petición vacío' );

  responder( 1, 'Error al guardar la medida' );

} // ()

// Se convierte el texto JSON en un array asociativo de PHP. Con el "true" del
// segundo parámetro se pide un array y no un objeto stdClass, que es lo que
// necesitan las funciones de la lógica.
$datos = json_decode( $cuerpoPeticion, true );

// json_decode NO avisa cuando algo va mal: simplemente devuelve null y guarda el
// motivo en json_last_error(). Sin esta comprobación, un JSON mal escrito
// seguiría adelante con $datos = null y saltaría un aviso de PHP.
if ( json_last_error() !== JSON_ERROR_NONE ) {

  error_log( 'GuardarMedida.php: JSON inválido -> ' . json_last_error_msg() );

  responder( 1, 'Error al guardar la medida' );

} // ()

// Un JSON puede ser válido y aun así no ser un objeto: por ejemplo el texto
// "5" o el texto "\"hola\"" son JSON válidos, pero no tienen los 5 campos.
// Hay que comprobar que sea un array antes de buscar claves dentro.
if ( ! is_array( $datos ) ) {

  error_log( 'GuardarMedida.php: el cuerpo es JSON válido pero no es un objeto' );

  responder( 1, 'Error al guardar la medida' );

} // ()

error_log( 'GuardarMedida.php: cuerpo recibido = ' . $cuerpoPeticion );

// -------------------------------------------------------------------------------------------------
// 2) VALIDACIÓN DE PARÁMETROS
// -------------------------------------------------------------------------------------------------
// Se comprueba que estén los 5 campos del diseño.
//
// Se usa array_key_exists() y no isset() a propósito. isset() da la razón por
// dos motivos distintos: "no existe" y "existe pero vale null", así que un
// {"uuid": null, ...} pasaría el filtro de isset() y llegaría a la lógica como
// un null. Con array_key_exists() se sabe exactamente qué campo falta.
//
// OJO: que estén los 5 campos NO significa que sus VALORES sean válidos. Que
// major sea "abc" o que el uuid esté vacío lo decide la lógica, en
// guardarMedida(), que es quien tiene las reglas del negocio. Aquí solo se
// comprueba que la petición tenga la forma que dice el diseño.
$camposObligatorios = array( 'uuid', 'major', 'minor', 'txPower', 'nombreEmisora' );

$faltanCampos = array();

foreach ( $camposObligatorios as $campo ) {

  if ( ! array_key_exists( $campo, $datos ) ) {
    $faltanCampos[] = $campo;
  }

} // ()

if ( count( $faltanCampos ) > 0 ) {

  error_log( 'GuardarMedida.php: faltan campos obligatorios -> ' . implode( ', ', $faltanCampos ) );

  responder( 1, 'Error al guardar la medida' );

} // ()

// -------------------------------------------------------------------------------------------------
// 3) LLAMADA A LA LÓGICA Y GENERACIÓN DEL JSON DE SALIDA
// -------------------------------------------------------------------------------------------------
//
// Aquí es donde se llama a la VERDADERA función de negocio. El endpoint no
// guarda nada por su cuenta: solo le pasa los 5 datos y espera un true/false.
$guardada = guardarMedida(
              $datos['uuid'],
              $datos['major'],
              $datos['minor'],
              $datos['txPower'],
              $datos['nombreEmisora']
            );

// Si la lógica ha devuelto true, la medida está dentro.
if ( $guardada === true ) {

  responder( 0, 'Medida guardada correctamente' );

} // ()

responder( 1, 'Error al guardar la medida' );

?>
