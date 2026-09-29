<?php

// ==========================================================
// DISEÑO LÓGICO: función de lógica de login
// ----------------------------------------------------------
//   nombre:Texto, password:Texto --> hacerLogin() --> VoF
//
// Comprueba las credenciales. Versión de práctica: acepta el
// password fijo de demostración.
//
// SEGURIDAD / MEJORA PENDIENTE: aquí debería validarse el password
// contra un hash almacenado en base de datos (p.ej. con
// password_verify()) y hacerse la comparación en tiempo constante.
// Un password en el código ("1234") es solo para el esqueleto de
// prácticas y NO debe usarse en producción.
// ==========================================================

// password de demostración (solo práctica)
define('PASSWORD_DEMO', '1234');

function hacerLogin( $nombre, $password ) {

  if ( $nombre === "" || $password === "" ) {
    return false;
  }

  // comprobación "rigurosa" del password (de práctica)
  if ( hash_equals( PASSWORD_DEMO, $password ) ) {
    return true;
  }

  return false;
}
?>
