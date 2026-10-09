<?php

require_once('../web_business_logic/diHola.php');

// ==========================================================
// DISEÑO LÓGICO: endpoint REST diHola
// ----------------------------------------------------------
//   (usuario en la sesión) --> diHola() -->
//   (nombre:Texto, saludo:Texto, error:Z) | (error:Texto)
//
// Exige que el usuario esté acreditado (sesión) y responde JSON.
// ==========================================================

// cabecera de respuesta JSON
header('Content-Type: application/json; charset=utf-8');

session_start();

// creo el objeto resultado
$objetoResultado = new stdClass;

// compruebo si esto lo pide un usuario
// antes acreditado mediante login
if ( ! isset( $_SESSION["usuario"]) ) {
  // no es un usuario acreditado
  $objetoResultado->error = "usuario no acreditado";
  // $objetoResultado->nombre = "";
  // $objetoResultado->saludo = "";
  // echo == devolver
  echo json_encode( $objetoResultado );
  return;
}

// Sí que es un usuario acreditado:
$usuario = $_SESSION["usuario"];

//
// llamada a la verdadera función.
//
$objetoResultado = diHola( $usuario );

$objetoResultado->error = 0;

// echo == devolver
echo json_encode( $objetoResultado );
?>
