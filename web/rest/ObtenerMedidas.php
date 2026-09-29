<?php

// ==================================================================================================
// DISEÑO LÓGICO: endpoint REST ObtenerMedidas.php
// --------------------------------------------------------------------------------------------------
//
//   Método y ruta:  GET  ../rest/ObtenerMedidas.php?cuantasComoMaximo=n
//
//   ENTRADA (parámetro de la URL):
//   cuantasComoMaximo: N
//
//   LLAMADA A LA LÓGICA (esta es la única función de negocio a la que llama):
//   cuantasComoMaximo: N --> obtenerMedidas() --> [ Medida ]
//
//   SALIDA (cuerpo JSON de la respuesta):
//   {
//       "medidas": [ Medida ]
//   }
//
//   donde Medida = ( id: N, uuid: Text, major: N, minor: Z,
//                    txPower: Z, nombreEmisora: Text, fechaLectura: Text )
//
// --------------------------------------------------------------------------------------------------
// QUÉ ES ESTE FICHERO
//
// El hermano gemelo de GuardarMedida.php, pero en la otra dirección: este NO
// guarda nada, solo LEE.
//
//   GuardarMedida.php    -->  guardarMedida()    -->  B            (escribe)
//   ObtenerMedidas.php   <--  obtenerMedidas()  <--  [ Medida ]    (solo lee)
//
// La flecha hacia la izquierda en el diagrama significa que obtenerMedidas()
// NO modifica el estado de la base de datos. Esta página es de consulta pura:
// se puede llamar las veces que se quiera sin miedo, no ensucia la base de
// datos ni un solo byte.
//
// OJO: aquí no hay campo "error" en la respuesta, y no es un descuido. El
// diseño del enunciado para esta ruta es { "medidas": [ Medida ] } a secas.
// El único motivo por el que puede salir vacía es que no haya medidas, así que
// una lista vacía es una respuesta HONESTA y el cliente decide qué hacer con
// ella. Si algún día hiciera falta avisar de un error, se añadiría el campo
// "error" como en GuardarMedida.php, pero de momento el diseño no lo pide y no
// se inventa.
//
// OJO con la flecha: el enunciado la dibuja --> hacia obtenerMedidas() y <-- de
// vuelta. Se lee así: a la lógica le ENTRAN las entradas por la derecha
// (cuantasComoMaximo: N) y le SALE un [ Medida ] por la izquierda. Es el mismo
// convenio que en guardarMedida(): entradas por la derecha, resultados por la
// izquierda.
//
// --------------------------------------------------------------------------------------------------
// POR QUÉ "cuantasComoMaximo" Y NO UN NÚMERO FIJO
//
// Es un tope, no un valor exacto. Si se pide 2 y hay 57 medidas,
// salen 2. Si se pide 100 y hay 3, salen 3. Y si se pide 0, o "abc", o un
// número negativo, la lógica devuelve una lista vacía en vez de reventar la
// conexión: un parámetro raro no debe tumbar el servidor. Eso ya está resuelto
// en obtenerMedidas(), y este endpoint no lo repite: se lo pasa tal cual y
// devuelve lo que le den.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

require_once '../logica/LogicaMedidas.php';

// Cabecera de respuesta: todo lo que devuelva este endpoint es JSON en UTF-8.
header('Content-Type: application/json; charset=utf-8');

/*
 * Imprime la respuesta y termina el script.
 *
 * @param array $lasMedidas Lista de Medida que devuelve obtenerMedidas().
 */
function responderConMedidas( $lasMedidas ) {

  // El diseño pide { "medidas": [ ... ] }, así que la lista va dentro de una
  // clave llamada "medidas". Este array (PHP) se convierte en un objeto (JSON).
  $respuesta = array(
    'medidas' => array_values( $lasMedidas ),
  );

  echo json_encode( $respuesta, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES );

  exit;

} // ()

// -------------------------------------------------------------------------------------------------
// 1) LECTURA DE LA PETICIÓN
// -------------------------------------------------------------------------------------------------
// Los datos de esta ruta NO vienen en el cuerpo, como en GuardarMedida.php,
// sino en la URL, en el Query String:  ?cuantasComoMaximo=2
//
// OJO: un GET con cuerpo es raro, así que aquí no se toca php://input para nada.
if ( ! isset( $_SERVER['REQUEST_METHOD'] ) || $_SERVER['REQUEST_METHOD'] !== 'GET' ) {

  error_log( 'ObtenerMedidas.php: método ' . ( isset($_SERVER['REQUEST_METHOD']) ? $_SERVER['REQUEST_METHOD'] : '(ninguno)' ) . ' en vez de GET' );

  responderConMedidas( array() );

} // ()

// -------------------------------------------------------------------------------------------------
// 2) VALIDACIÓN DE PARÁMETROS
// -------------------------------------------------------------------------------------------------
// El parámetro es opcional: si no viene en la URL se usa 100, que es el valor
// por defecto que dice el diseño.
//
// OJO con isset() aquí sí vale, y además es MEJOR que en el otro endpoint: si
// alguien pide ?cuantasComoMaximo= vacío, isset() da false y salta al 100 por
// defecto, que es justo lo que se quiere. Comprobar "no vacío" con
// $_GET['cuantasComoMaximo'] a pelo daría un aviso de PHP por usar una clave
// que igual no existe.
//
// Y OJO con lo que NO se hace aquí: NO se comprueba que el número sea
// positivo ni que sea un entero. Ese no es el trabajo de este fichero. Se le
// pasa el valor a obtenerMedidas() tal cual, y es la lógica la que decide si
// vale (le devolverá una lista vacía si no). Si este endpoint se pusiera a
// validar por su cuenta, la misma decisión estaría escrita en dos sitios, y en
// cuanto las reglas cambiasen en uno de los dos, dejarían de estar de acuerdo.
if ( isset( $_GET['cuantasComoMaximo'] ) ) {

  $cuantasComoMaximo = $_GET['cuantasComoMaximo'];

} else {

  $cuantasComoMaximo = 100;

} // ()

error_log( 'ObtenerMedidas.php: se piden ' . $cuantasComoMaximo . ' medidas como máximo' );

// -------------------------------------------------------------------------------------------------
// 3) LLAMADA A LA LÓGICA Y GENERACIÓN DEL JSON DE SALIDA
// -------------------------------------------------------------------------------------------------
// La única llamada a la lógica de este endpoint. Devuelve un array de Medida,
// cada una con sus 7 claves (id, uuid, major, minor, txPower, nombreEmisora,
// fechaLectura), ya ordenadas de la más reciente a la más antigua.
$listaMedidas = obtenerMedidas( $cuantasComoMaximo );

responderConMedidas( $listaMedidas );

?>
