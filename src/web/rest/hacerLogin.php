<?php

require_once('../logica/hacerLogin.php');

// ==========================================================
// DISEÑO LÓGICO: endpoint REST hacerLogin
// ----------------------------------------------------------
//   nombre:Texto, password:Texto --> hacerLogin() -->
//   { resultado:B, usuario:Texto } | { resultado:B }
//
// Comprueba las credenciales con la lógica y, si son correctas,
// guarda el usuario en la sesión y devuelve JSON.
//
// MEJORAS / CORRECCIONES:
//  - Se reciben los datos por POST (antes por GET, con lo que las
//    credenciales quedaban en la URL, el historial y los logs).
//  - Un único session_start() (antes se llamaba dos veces y PHP
//    avisaba de "session already started").
//  - Comprobación isset() de los parámetros (antes daban notice si
//    faltaban y se comparaba con valores nulos).
//  - session_regenerate_id(true) al autenticar: evita fijación de
//    sesión.
//  - Se devuelve el Content-Type JSON correcto.
//
// SEGURIDAD PENDIENTE: la comprobación real del password (hash) la
// hace la lógica; ver logica/hacerLogin.php.
// ==========================================================

$objetoResultado = new stdClass;

// cabecera de respuesta
header('Content-Type: application/json; charset=utf-8');

// creo (o reanudo) la sesión UNA sola vez
session_start();

// obtengo valores de los parámetros (por POST)
$nombre   = isset($_POST["nombre"])   ? trim($_POST["nombre"])   : "";
$password = isset($_POST["password"]) ? $_POST["password"]       : "";

//
// llamada a la verdadera función.
//
if ( hacerLogin( $nombre, $password ) == true ) {

  // defensa contra fijación de sesión
  session_regenerate_id(true);

  $objetoResultado->resultado = true;
  $objetoResultado->usuario = $nombre;

  // guardo en la sesión el nombre del usuario
  $_SESSION["usuario"] = $nombre;

} else {
  session_destroy();
  $objetoResultado->resultado = false;
}

// echo == devolver
echo json_encode( $objetoResultado );
?>
