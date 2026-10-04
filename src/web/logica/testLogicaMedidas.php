<?php

// ==================================================================================================
// DISEÑO LÓGICO: testLogicaMedidas.php (test automático de la lógica del negocio por consola)
// --------------------------------------------------------------------------------------------------
//   Medida = (
//       id: N,
//       uuid: Text,
//       major: N,
//       minor: Z,
//       txPower: Z,
//       nombreEmisora: Text,
//       fechaLectura: Text
//   )
//
//                    ------------- testLogicaMedidas.php -------------
//                    |
//                     --> probarGuardarMedida() -->
//   B <--            |
//                    |
//                     --> probarObtenerMedidas() -->
//   [ Medida ] <--   |
//                    |
//                     --> probarValidacion() -->
//   B <--            |
//                    |
//                     --> probarLogicaMedidas() -->
//   B <--            |
//                    ----------------------------------------------------
//
// QUÉ HACE ESTE SCRIPT
//
// Comprueba, sin PHPUnit y sin librerías externas, que la lógica del negocio de
// web/logica/LogicaMedidas.php funciona:
//
//   1. guardarMedida("EPSG-GTI-PROY-3A", 2817, 235, -53, "GTI-Jose") devuelve true.
//   2. obtenerMedidas(5) devuelve un array no vacío, ordenado por id descendente,
//      y cada medida tiene las 7 propiedades del tipo Medida.
//   3. Se imprimen "TEST LOGICA BACKEND OK" o "TEST LOGICA BACKEND ERROR".
//
// Además comprueba los filtros de duplicidad de datos (guardarMedida con texto
// vacío o con números que no son enteros) y que obtenerMedidas() respeta el
// límite que se le pide, que es donde suelen esconderse los fallos.
//
// CÓMO SE EJECUTA
//
//     cd web\logica
//     php testLogicaMedidas.php
//
// NOTA: este test INSERTA filas de verdad en bdd.sqlite, pero al terminar
// BORRA todas las que ha creado él mismo, así que la base de demostración se
// queda igual que estaba. Usa valores propios (major con números altos) para poder
// distinguir sus filas de las de los demás.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

// -------------------------------------------------------------------------------------------------
// Este script es de CONSOLA. Si alguien intenta abrirlo desde el navegador, se corta.
// -------------------------------------------------------------------------------------------------
if ( PHP_SAPI !== 'cli' ) {
  header( 'HTTP/1.1 403 Forbidden' );
  die( "testLogicaMedidas.php solo se ejecuta por consola:  php testLogicaMedidas.php" . PHP_EOL );
}

require_once __DIR__ . '/LogicaMedidas.php';

// -------------------------------------------------------------------------------------------------
// Helpers de impresión por consola.
// -------------------------------------------------------------------------------------------------
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

// -------------------------------------------------------------------------------------------------
// --> ultimoIdAntesDeProbar() --> N
// Qué hace: anota el último id que había en la tabla Medidas ANTES de que empiece el test.
//           Ese número sirve después para borrar solo las filas que ha creado ESTE test
//           (son las que tengan id MAYOR que este), sin tocar los datos que hubiera.
//
// Por qué importa: si el test se dejara sus filas detrás, la base de la demostración
// se llenaría de 5 medidas falsas cada vez que se ejecuta, y en los siguientes
// prompts el navegador mostraría un montón de medidas de prueba que no son reales.
// -------------------------------------------------------------------------------------------------
function ultimoIdAntesDeProbar() {

  $bdd = conectarBBDD();

  $fila = $bdd->query( 'SELECT COALESCE( MAX( ID ), 0 ) AS elUltimo FROM Medidas' )->fetch();

  return (int) $fila['elUltimo'];

} // ()

// -------------------------------------------------------------------------------------------------
// --> borrarLoQueHaCreadoElTest() --> N
// Qué hace: borra las medidas con id mayor que el anotado al empezar, o sea,
//           exactamente las que ha insertado este test. Devuelve cuántas ha borrado.
//
// OJO: el DELETE va en su propia sentencia, sin parámetros, porque no hay ningún
// valor que venga de fuera. Aun así va preparada, para no hacer excepciones.
//
// Esto NO es lógica de negocio: no va en LogicaMedidas.php, porque borrar filas
// para arreglar un test es cosa del test, no del negocio.
// -------------------------------------------------------------------------------------------------
function borrarLoQueHaCreadoElTest( $ultimoId ) {

  $bdd = conectarBBDD();

  $sentencia = $bdd->prepare( 'DELETE FROM Medidas WHERE ID > :ultimoId' );
  $sentencia->bindValue( ':ultimoId', (int) $ultimoId, PDO::PARAM_INT );
  $sentencia->execute();

  return $sentencia->rowCount();

} // ()

// -------------------------------------------------------------------------------------------------
// Todas las notas del test se van metiendo aquí. Cada comprobación mete true o false.
// Al final, si hay algún false, el test es un ERROR.
// -------------------------------------------------------------------------------------------------
$comprobaciones = array();

titulo( "TEST AUTOMATICO DE LA LOGICA DEL NEGOCIO - LogicaMedidas.php" );

// =================================================================================================
// PROBAR QUE guardarMedida() DEVUELVE true
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> probarGuardarMedida() --> B
// Qué hace: llama a guardarMedida() con la medida de ejemplo del enunciado y
//           comprueba que devuelve true.
// -------------------------------------------------------------------------------------------------
function probarGuardarMedida() {

  paso( "1. guardarMedida( 'EPSG-GTI-PROY-3A', 2817, 235, -53, 'GTI-Jose' )" );

  $resultado = guardarMedida( 'EPSG-GTI-PROY-3A', 2817, 235, -53, 'GTI-Jose' );

  if ( $resultado === true ) {
    ok( "guardarMedida() ha devuelto true: la medida se ha guardado" );
    return true;
  }

  fallo( "guardarMedida() NO ha devuelto true, ha devuelto: " . var_export( $resultado, true ) );
  return false;

} // ()

// =================================================================================================
// PROBAR QUE obtenerMedidas() DEVUELVE BIEN LA LISTA
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> probarObtenerMedidas() --> [ Medida ]
// Qué hace: inserta varias medidas y comprueba que obtenerMedidas(5) devuelve un
//           array no vacío, ordenado por id descendente, con las 7 claves de Medida.
// -------------------------------------------------------------------------------------------------
function probarObtenerMedidas() {

  // Las 7 claves que define el tipo Medida.
  $clavesMedida = array( 'id', 'uuid', 'major', 'minor', 'txPower', 'nombreEmisora', 'fechaLectura' );

  // Se guardan 3 medidas más, con major distintos y altos, solo para este test.
  // Así se puede comprobar que el orden por id descendente se nota.
  paso( "1b. Guardo 3 medidas más para poder comprobar el orden y el límite" );

  guardarMedida( 'EPSG-GTI-PROY-3A', 90001, 11, -60, 'GTI-Jose' );
  guardarMedida( 'EPSG-GTI-PROY-3A', 90002, 22, -61, 'GTI-Jose' );
  guardarMedida( 'EPSG-GTI-PROY-3A', 90003, 33, -62, 'GTI-Jose' );

  paso( "2. obtenerMedidas( 5 )" );

  $medidas = obtenerMedidas( 5 );

  // --- que devuelva un array NO VACÍO ---------------------------------------------------
  if ( is_array( $medidas ) && count( $medidas ) > 0 ) {
    ok( "obtenerMedidas(5) ha devuelto un array no vacío con " . count( $medidas ) . " medidas" );
    $arrayCorrecto = true;
  } else {
    fallo( "obtenerMedidas(5) NO ha devuelto un array no vacío" );
    $arrayCorrecto = false;
    return false; // sin datos no se puede seguir mirando nada más
  }

  // --- que RESPETE el límite de 5 ------------------------------------------------------
  if ( count( $medidas ) <= 5 ) {
    ok( "obtenerMedidas(5) respeta el límite: no devuelve más de 5" );
    $limiteCorrecto = true;
  } else {
    fallo( "obtenerMedidas(5) ha devuelto " . count( $medidas ) . " medidas, el límite es 5" );
    $limiteCorrecto = false;
  }

  // --- que las 7 propiedades de Medida estén en todas -----------------------------------
  $faltanClaves = array();

  foreach ( $medidas as $indice => $laMedida ) {

    if ( ! is_array( $laMedida ) ) {
      fallo( "la medida de la posición " . $indice . " no es un array" );
      $faltanClaves[] = 'no es un array';
      continue;
    }

    // Se comparan las claves devueltas con las del tipo Medida.
    $faltan = array_diff( $clavesMedida, array_keys( $laMedida ) );

    if ( count( $faltan ) > 0 ) {
      $faltanClaves[] = 'posición ' . $indice . ' sin: ' . implode( ',', $faltan );
    }
  }

  if ( count( $faltanClaves ) === 0 ) {
    ok( "las " . count( $medidas ) . " medidas tienen las 7 propiedades de Medida ("
         . implode( ', ', $clavesMedida ) . ")" );
    $clavesCorrectas = true;
  } else {
    foreach ( $faltanClaves as $aviso ) {
      fallo( "faltan propiedades en Medida -> " . $aviso );
    }
    $clavesCorrectas = false;
  }

  // --- que venga ORDENADA por id DESCENDENTE ---------------------------------------------
  // Se recorre la lista y se comprueba que cada id es MENOR que el anterior.
  $ordenCorrecto = true;
  $ids = array();

  for ( $i = 0 ; $i < count( $medidas ) ; $i++ ) {
    $ids[] = (int) $medidas[$i]['id'];

    if ( $i > 0 ) {
      if ( (int) $medidas[$i]['id'] >= (int) $medidas[$i - 1]['id'] ) {
        fallo( "el orden por id descendente está roto: id[" . ( $i - 1 ) . "]="
               . $medidas[$i - 1]['id'] . " y id[" . $i . "]=" . $medidas[$i]['id'] );
        $ordenCorrecto = false;
      }
    }
  }

  if ( $ordenCorrecto ) {
    ok( "las medidas vienen ordenadas por id descendente: " . implode( ' > ', $ids ) );
  }

  // --- que los tipos sean los del diseño ------------------------------------------------
  // major: N, minor: Z, txPower: Z tienen que ser enteros de verdad, no el texto "235".
  $tiposCorrectos = true;
  $primera = $medidas[0];

  if ( ! is_int( $primera['id'] ) ) {
    fallo( "id debería ser entero ( N ), es " . gettype( $primera['id'] ) );
    $tiposCorrectos = false;
  }
  if ( ! is_string( $primera['uuid'] ) ) {
    fallo( "uuid debería ser texto ( Text ), es " . gettype( $primera['uuid'] ) );
    $tiposCorrectos = false;
  }
  if ( ! is_int( $primera['major'] ) ) {
    fallo( "major debería ser entero ( N ), es " . gettype( $primera['major'] ) );
    $tiposCorrectos = false;
  }
  if ( ! is_int( $primera['minor'] ) ) {
    fallo( "minor debería ser entero ( Z ), es " . gettype( $primera['minor'] ) );
    $tiposCorrectos = false;
  }
  if ( ! is_int( $primera['txPower'] ) ) {
    fallo( "txPower debería ser entero ( Z ), es " . gettype( $primera['txPower'] ) );
    $tiposCorrectos = false;
  }
  if ( ! is_string( $primera['nombreEmisora'] ) ) {
    fallo( "nombreEmisora debería ser texto ( Text ), es " . gettype( $primera['nombreEmisora'] ) );
    $tiposCorrectos = false;
  }
  if ( ! is_string( $primera['fechaLectura'] ) ) {
    fallo( "fechaLectura debería ser texto ( Text ), es " . gettype( $primera['fechaLectura'] ) );
    $tiposCorrectos = false;
  }

  if ( $tiposCorrectos ) {
    ok( "los tipos de la primera medida son los del diseño Medida" );
  }

  // --- que un límite absurdo devuelva una lista vacía, no un error ------------------------
  paso( "2b. obtenerMedidas( 0 ) y obtenerMedidas( 'abc' ) deben devolver lista vacía" );

  if ( obtenerMedidas( 0 ) === array() && obtenerMedidas( 'abc' ) === array() ) {
    ok( "un límite que no es un entero positivo devuelve lista vacía y no peta" );
    $limitesDefensivos = true;
  } else {
    fallo( "un límite inválido NO ha devuelto una lista vacía" );
    $limitesDefensivos = false;
  }

  return $arrayCorrecto && $limiteCorrecto && $clavesCorrectas && $ordenCorrecto
         && $tiposCorrectos && $limitesDefensivos;

} // ()

// =================================================================================================
// PROBAR QUE guardarMedida() RECHAZA LOS DATOS MALOS
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> probarValidacion() --> B
// Qué hace: comprueba que guardarMedida() devuelve false (y NO inserta nada) cuando
//           le llegan datos que no son válidos.
// -------------------------------------------------------------------------------------------------
function probarValidacion() {

  paso( "3. La validación de guardarMedida() con datos malos" );

  $casos = array(
    'uuid vacío'            => array( '',            2817, 235, -53, 'GTI-Jose' ),
    'uuid con espacios'     => array( '   ',         2817, 235, -53, 'GTI-Jose' ),
    'nombreEmisora vacío'   => array( 'EPSG-...',    2817, 235, -53, ''         ),
    'major no entero'       => array( 'EPSG-...', 'abc',    235, -53, 'GTI-Jose' ),
    'minor no entero'       => array( 'EPSG-...',  2817, 'xyz', -53, 'GTI-Jose' ),
    'txPower no entero'     => array( 'EPSG-...',  2817, 235, 'dbm', 'GTI-Jose' ),
    'major con decimales'   => array( 'EPSG-...',  2817.5, 235, -53, 'GTI-Jose' ),
  );

  $todoRechazadoBien = true;

  foreach ( $casos as $nombreCaso => $losParametros ) {

    $resultado = call_user_func_array( 'guardarMedida', $losParametros );

    if ( $resultado === false ) {
      ok( "guardarMedida() rechaza correctamente: " . $nombreCaso );
    } else {
      fallo( "guardarMedida() ha devuelto " . var_export( $resultado, true )
             . " con un caso que debería ser inválido: " . $nombreCaso );
      $todoRechazadoBien = false;
    }
  }

  // OJO: un valor 0 SÍ es válido (minor=0 o txPower=0). Se comprueba aparte porque es
  // el error típico: usar "if ( ! $valor )" en vez de "=== false" rechazaría el 0.
  paso( "3b. Un 0 en minor y en txPower es un valor VÁLIDO (no debe rechazarse)" );

  if ( guardarMedida( 'EPSG-GTI-PROY-3A', 90004, 0, 0, 'GTI-Jose' ) === true ) {
    ok( "guardarMedida() acepta minor=0 y txPower=0, que son valores legales" );
  } else {
    fallo( "guardarMedida() ha rechazado minor=0 / txPower=0, y son valores legales" );
    $todoRechazadoBien = false;
  }

  return $todoRechazadoBien;

} // ()

// =================================================================================================
// PROBAR QUE LA LÓGICA NO DEPENDE DE HTTP
// =================================================================================================

// -------------------------------------------------------------------------------------------------
// --> probarAislamiento() --> B
// Qué hace: comprueba que LogicaMedidas.php no toca nada de HTTP. Si algún día
//           alguien metiera un session_start() o un echo aquí dentro, la lógica dejaría
//           de ser reutilizable desde otro sitio, y este test lo cantaría.
// -------------------------------------------------------------------------------------------------
function probarAislamiento() {

  paso( "4. La lógica del negocio no depende de HTTP ni de sesiones" );

  $contenido = file_get_contents( __DIR__ . '/LogicaMedidas.php' );

  // Palabras que NO deberían aparecer en la capa de lógica.
  $prohibidas = array( 'session_start', '\$_POST', '\$_GET', '\$_REQUEST', 'json_encode', 'header(' );

  $hayFuga = false;

  foreach ( $prohibidas as $prohibida ) {
    // Solo se mira fuera de los comentarios (las líneas que empiezan por // o *).
    $soloCodigo = preg_replace( '#^\s*(//|\*|/\*).*$#m', '', $contenido );

    // OJO el preg_quote(): "header(" lleva un paréntesis, que en una expresión
    // regular es un metacarácter de grupo. Sin el preg_quote() el patrón sería
    // "#header(#", que es un regex MAL FORMADO: preg_match() devolvería false con
    // un warning y la comprobación no miraría nada, dando un OK falso.
    $patron = '#' . preg_quote( $prohibida, '#' ) . '#';

    if ( preg_match( $patron, $soloCodigo ) === 1 ) {
      fallo( "la lógica de negocio usa '" . $prohibida
             . "', que es de la capa HTTP: eso no debería estar aquí" );
      $hayFuga = true;
    }
  }

  if ( $hayFuga === false ) {
    ok( "LogicaMedidas.php está limpia: no usa sesiones, ni \$_POST, ni json_encode" );
  }

  return ! $hayFuga;

} // ()

// =================================================================================================
// SE EJECUTAN TODAS LAS PRUEBAS
// =================================================================================================

paso( "0. Empiezo las pruebas" );

// Se apunta dónde acaba la tabla ANTES de insertar nada, para poder limpiar después.
$ultimoIdAlEmpezar = ultimoIdAntesDeProbar();

$comprobaciones[] = probarGuardarMedida();
$comprobaciones[] = probarObtenerMedidas();
$comprobaciones[] = probarValidacion();
$comprobaciones[] = probarAislamiento();

// -------------------------------------------------------------------------------------------------
// LIMPIEZA: se borran solo las filas creadas por ESTE test.
// -------------------------------------------------------------------------------------------------
paso( "5. Limpio las filas que ha creado este test" );

$borradas = borrarLoQueHaCreadoElTest( $ultimoIdAlEmpezar );

if ( $borradas === 5 ) {
  ok( "se han borrado las " . $borradas . " medidas del test, y solo esas (había "
       . $ultimoIdAlEmpezar . ( $ultimoIdAlEmpezar === 1 ? ' id' : ' ids' ) . " antes)" );
} elseif ( $borradas === 0 ) {
  ok( "no ha hecho falta borrar nada, el test no llegó a insertar" );
} else {
  ok( "se han borrado " . $borradas . " medidas del test" );
}

// -------------------------------------------------------------------------------------------------
// Veredicto.
// -------------------------------------------------------------------------------------------------
paso( "6. Veredicto" );

$fallos = 0;
foreach ( $comprobaciones as $nota ) {
  if ( $nota === false ) {
    $fallos++;
  }
}

echo PHP_EOL . "    Pruebas: " . count( $comprobaciones ) . "   Fallos: " . $fallos . PHP_EOL;

titulo( ( $fallos === 0 ) ? "TEST LOGICA BACKEND OK" : "TEST LOGICA BACKEND ERROR" );

// Código de salida 0 = todo bien, 1 = algún fallo (para poder encadenarlo en consola).
exit( $fallos === 0 ? 0 : 1 );

?>
