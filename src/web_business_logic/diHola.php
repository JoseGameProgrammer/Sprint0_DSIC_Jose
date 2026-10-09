<?php
// ==========================================================
// DISEÑO LÓGICO: función de lógica diHola
// ----------------------------------------------------------
//   usuario:Texto --> diHola() --> (nombre:Texto, saludo:Texto)
//
// Devuelve un objeto con el nombre recibido y un saludo.
// ==========================================================

function diHola( $usuario ) {
  $objetoResultado = new stdClass;
  $objetoResultado->nombre = $usuario;
  $objetoResultado->saludo = "That's all folks";

  return $objetoResultado;
}

?>
