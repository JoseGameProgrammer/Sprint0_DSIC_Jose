<?php

/* =================================================================================================
   DISEÑO LÓGICO: testBD.php (test automático de la BBDD por consola)
   -------------------------------------------------------------------------------------------------
   +--------------------------------------------------+
   |               BBDD: Tabla Medidas                |
   +--------------------------------------------------+
   | ID: N (Primary Key, AutoIncremental)             |
   | FechaLectura: Text                               |
   | Uuid: Text                                       |
   | Major: N                                         |
   | Minor: Z                                         |
   | TxPower: Z                                       |
   | NombreEmisora: Text                              |
   +--------------------------------------------------+

   Medida = (
       id: N,
       uuid: Text,
       major: N,
       minor: Z,
       txPower: Z,
       nombreEmisora: Text,
       fechaLectura: Text
   )

   TablaMedidas = [ Medida ]

   -------------------------------------------------------------------------------------------------
   QUÉ HACE ESTE SCRIPT

   Se ejecuta SOLO por consola (no es un endpoint web) y comprueba, sin usar PHPUnit
   ni ninguna librería externa, que la tabla Medidas se crea bien y que guarda una
   medida correctamente:

     1. Crea la tabla Medidas ejecutando el fichero crearBD.sql.
     2. Inserta un registro de prueba.
     3. Comprueba con "if" que el ID autogenerado es mayor que 0 y que la FechaLectura
        se ha rellenado sola.
     4. Imprime por consola "TEST BD OK" o "TEST BD ERROR".

   CÓMO SE EJECUTA

       cd web\bdd
       php testBD.php

   -------------------------------------------------------------------------------------------------
   TODO(Jose) - BASE DE DATOS DE DEMOSTRACIÓN

   Esto usa SQLite con un fichero de base de datos local (bdd.sqlite) porque es lo
   único que no necesita instalar un servidor. Cuando pases a MySQL you'll tener que
   cambiar tres cosas:

     1. La conexión: en vez de  new PDO('sqlite:bdd.sqlite')  ->
                     new PDO('mysql:host=TU_HOST;dbname=TU_BASE;charset=utf8mb4', 'user', 'clave')
     2. La llamada a $bdd->exec( $sql ), porque MySQL NO admite varias sentencias
        separadas por ";" en un solo exec(). Habrá que leer crearBD.sql, partirlo por
        ";" y ejecutar cada troceado por separado.
     3. Los tipos: en MySQL el autoincremental no es AUTOINCREMENT, sino
        "INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY", y la fecha automática es
        "TIMESTAMP" con "DEFAULT CURRENT_TIMESTAMP".
   =================================================================================================
*/

// -------------------------------------------------------------------------------------------------
// Este script es de CONSOLA. Si alguien intenta abrirlo desde el navegador, se corta.
// -------------------------------------------------------------------------------------------------
if ( PHP_SAPI !== 'cli' ) {
  header('HTTP/1.1 403 Forbidden');
  die( "testBD.php solo se ejecuta por consola:  php testBD.php" . PHP_EOL );
}

// -------------------------------------------------------------------------------------------------
// Rutas y datos de configuración.
// -------------------------------------------------------------------------------------------------
$rutaSQLCrearTabla = __DIR__ . DIRECTORY_SEPARATOR . 'crearBD.sql';
$rutaBaseDeDatos   = __DIR__ . DIRECTORY_SEPARATOR . 'bdd.sqlite';

// Los mismos datos de prueba que usa el test del teléfono (Prompt 4).
$uuidPrueba          = 'EPSG-GTI-PROY-3A';
$majorPrueba         = 2817;
$minorPrueba         =  235;
$txPowerPrueba       =  -53;
$nombreEmisoraPrueba = 'GTI-Jose';

// -------------------------------------------------------------------------------------------------
// Variable donde se va acumulando el resultado de cada comprobación.
// Cada comprobación va añadiendo su "nota" (true = bien, false = mal) a este array.
// Al final se mira si todas las notas son true.
// -------------------------------------------------------------------------------------------------
$comprobaciones = array();

// -------------------------------------------------------------------------------------------------
// Helpers de impresión por consola, para que el test se lea bien.
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
  echo "    OK   " . $texto . PHP_EOL;
}

function fallo( $texto ) {
  echo "    FALLO " . $texto . PHP_EOL;
}

// -------------------------------------------------------------------------------------------------
// Empezamos el test.
// -------------------------------------------------------------------------------------------------
titulo( "TEST AUTOMATICO DE LA BBDD - Tabla Medidas" );

paso( "Fichero SQL  : " . $rutaSQLCrearTabla );
paso( "Base de datos: " . $rutaBaseDeDatos );

// ¿Existe el fichero crearBD.sql? Sin él no hay nada que probar.
if ( ! file_exists( $rutaSQLCrearTabla ) ) {
  fallo( "No encuentro el fichero " . $rutaSQLCrearTabla );
  titulo( "TEST BD ERROR" );
  exit( 1 );
}
ok( "existe el fichero crearBD.sql" );

// -------------------------------------------------------------------------------------------------
// 1) CONEXIÓN Y CREACIÓN DE LA TABLA
// -------------------------------------------------------------------------------------------------
paso( "1. Abro la base de datos y creo la tabla Medidas" );

try {

  $bdd = new PDO( 'sqlite:' . $rutaBaseDeDatos );

  // Para que la BD avise de los errores en vez de callárselos.
  $bdd->setAttribute( PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION );
  $bdd->setAttribute( PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC );

  ok( "conectado a la base de datos SQLite" );

  // Leo el SQL de crearBD.sql y lo ejecuto tal cual.
  $sql = file_get_contents( $rutaSQLCrearTabla );
  $bdd->exec( $sql );

  ok( "ejecutado crearBD.sql (CREATE TABLE IF NOT EXISTS Medidas)" );

  // Compruebo que la tabla existe de verdad preguntándoselo a la propia BBDD.
  $existe = $bdd->query( "SELECT name FROM sqlite_master WHERE type='table' AND name='Medidas'" )->fetchColumn();

  if ( $existe === 'Medidas' ) {
    ok( "la tabla 'Medidas' existe" );
    $comprobaciones[] = true;
  } else {
    fallo( "la tabla 'Medidas' NO existe" );
    $comprobaciones[] = false;
  }

} catch ( Exception $e ) {

  fallo( "excepción al crear la tabla: " . $e->getMessage() );
  $comprobaciones[] = false;

  titulo( "TEST BD ERROR" );
  exit( 1 );
}

// -------------------------------------------------------------------------------------------------
// 2) INSERCIÓN DE UN REGISTRO DE PRUEBA
// -------------------------------------------------------------------------------------------------
paso( "2. Inserto un registro de prueba" );

try {

  // Primero borro la medida de prueba de una ejecución anterior, para que el test
  // se pueda repetir muchas veces sin que se acumulen filas.
  $borrar = $bdd->prepare( "DELETE FROM Medidas WHERE Uuid = ? AND Major = ? AND Minor = ?" );
  $borrar->execute( array( $uuidPrueba, $majorPrueba, $minorPrueba ) );

  if ( $borrar->rowCount() > 0 ) {
    ok( "borrada la medida de prueba de la ejecución anterior" );
  } else {
    ok( "no había ninguna medida de prueba anterior" );
  }

  // OJO: NO se manda ID ni FechaLectura a propósito.
  //  - El ID tiene que generarlo solo la base de datos (AUTOINCREMENT).
  //  - La FechaLectura tiene que rellenarla sola el DEFAULT del SQL.
  // Si aquí metiera la fecha a mano, el test no probaría nada del DEFAULT.
  $insertar = $bdd->prepare(
    "INSERT INTO Medidas ( Uuid, Major, Minor, TxPower, NombreEmisora )
     VALUES ( :uuid, :major, :minor, :txPower, :nombreEmisora )" );

  $insertar->execute( array(
    ':uuid'           => $uuidPrueba,
    ':major'          => $majorPrueba,
    ':minor'          => $minorPrueba,
    ':txPower'        => $txPowerPrueba,
    ':nombreEmisora'  => $nombreEmisoraPrueba,
  ) );

  $idGenerado = $bdd->lastInsertId();

  ok( "medida insertada: uuid=" . $uuidPrueba . " major=" . $majorPrueba
       . " minor=" . $minorPrueba . " txPower=" . $txPowerPrueba
       . " nombreEmisora=" . $nombreEmisoraPrueba );
  ok( "ID autogenerado por la BBDD = " . $idGenerado );

} catch ( Exception $e ) {

  fallo( "excepción al insertar la medida: " . $e->getMessage() );
  $comprobaciones[] = false;

  titulo( "TEST BD ERROR" );
  exit( 1 );
}

// -------------------------------------------------------------------------------------------------
// 3) COMPROBACIONES AUTOMÁTICAS CON "if"
// -------------------------------------------------------------------------------------------------
paso( "3. Comprobaciones automáticas" );

// Leo de vuelta la fila que acabo de insertar, para mirarla de verdad.
$consultar = $bdd->prepare( "SELECT * FROM Medidas WHERE ID = :id" );
$consultar->execute( array( ':id' => $idGenerado ) );
$medida = $consultar->fetch();

if ( $medida === false ) {
  fallo( "no encuentro la fila con ID = " . $idGenerado );
  $comprobaciones[] = false;
  $medida = array(); // para que no reviente al comprobar el resto
} else {
  ok( "leída de vuelta la fila con ID = " . $idGenerado );
}

// ---------------------------------------------------------------------------------------------
// 3.a) EL ID AUTOGENERADO ES MAYOR QUE 0
//     Si el AUTOINCREMENT no estuviera puesto, lastInsertId() devolvería "" o el
//     INSERT ni siquiera se habría hecho. Este "if" lo detecta.
// ---------------------------------------------------------------------------------------------
$idLeido = isset( $medida['ID'] ) ? (int) $medida['ID'] : 0;

if ( $idLeido > 0 ) {
  ok( "comprobación 1: el ID autogenerado es mayor que 0 ( ID = " . $idLeido . " )" );
  $comprobaciones[] = true;
} else {
  fallo( "comprobación 1: el ID autogenerado NO es mayor que 0 ( ID = " . $idLeido . " )" );
  $comprobaciones[] = false;
}

// ---------------------------------------------------------------------------------------------
// 3.b) LA FechaLectura SE HA RELLENADO AUTOMÁTICAMENTE
//     Tiene que venir puesta sin que nadie la haya mandado, y con el formato
//     "AAAA-MM-DD HH:MM:SS" que produce datetime('now','localtime').
// ---------------------------------------------------------------------------------------------
$fechaLeida = isset( $medida['FechaLectura'] ) ? trim( (string) $medida['FechaLectura'] ) : '';

if ( $fechaLeida !== '' ) {
  ok( "comprobación 2: la FechaLectura se ha rellenado sola ( " . $fechaLeida . " )" );
  $comprobaciones[] = true;
} else {
  fallo( "comprobación 2: la FechaLectura está VACÍA, el DEFAULT del SQL no ha funcionado" );
  $comprobaciones[] = false;
}

// El formato tiene que ser AAAA-MM-DD HH:MM:SS (19 caracteres).
if ( preg_match( '/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/', $fechaLeida ) === 1 ) {
  ok( "comprobación 3: la FechaLectura tiene el formato AAAA-MM-DD HH:MM:SS correcto" );
  $comprobaciones[] = true;
} else {
  fallo( "comprobación 3: la FechaLectura NO tiene el formato AAAA-MM-DD HH:MM:SS ( " . $fechaLeida . " )" );
  $comprobaciones[] = false;
}

// ---------------------------------------------------------------------------------------------
// 3.c) LOS 5 DATOS HAN VIAJADO BIEN DESDE EL PHP HASTA LA TABLA
//     Si alguien cambiara el tipo de una columna o se equivocara al mapear el
//     INSERT, aquí se vería.
// ---------------------------------------------------------------------------------------------
$coincideUuid    = isset( $medida['Uuid'] )          && $medida['Uuid']          === $uuidPrueba;
$coincideMajor   = isset( $medida['Major'] )         && (int) $medida['Major']   === $majorPrueba;
$coincideMinor   = isset( $medida['Minor'] )         && (int) $medida['Minor']   === $minorPrueba;
$coincideTxPower = isset( $medida['TxPower'] )       && (int) $medida['TxPower'] === $txPowerPrueba;
$coincideNombre  = isset( $medida['NombreEmisora'] ) && $medida['NombreEmisora'] === $nombreEmisoraPrueba;

if ( $coincideUuid && $coincideMajor && $coincideMinor && $coincideTxPower && $coincideNombre ) {
  ok( "comprobación 4: los 5 campos de la medida se han guardado tal cual" );
  $comprobaciones[] = true;
} else {
  fallo( "comprobación 4: algún campo NO ha vuelto como se envió" );
  fallo( "          Uuid=" . var_export( isset( $medida['Uuid'] ) ? $medida['Uuid'] : null, true )
       . " (esperado " . $uuidPrueba . ")" );
  fallo( "          Major=" . var_export( isset( $medida['Major'] ) ? $medida['Major'] : null, true )
       . " (esperado " . $majorPrueba . ")" );
  fallo( "          Minor=" . var_export( isset( $medida['Minor'] ) ? $medida['Minor'] : null, true )
       . " (esperado " . $minorPrueba . ")" );
  fallo( "          TxPower=" . var_export( isset( $medida['TxPower'] ) ? $medida['TxPower'] : null, true )
       . " (esperado " . $txPowerPrueba . ")" );
  fallo( "          NombreEmisora=" . var_export( isset( $medida['NombreEmisora'] ) ? $medida['NombreEmisora'] : null, true )
       . " (esperado " . $nombreEmisoraPrueba . ")" );
  $comprobaciones[] = false;
}

// ---------------------------------------------------------------------------------------------
// 3.d) EL ESQUEMA ES EL DEL DISEÑO
//     Se comprueba con PRAGMA table_info que hay 7 columnas, todas NOT NULL, y que
//     la clave primaria es ID. Esto es lo que garantiza que la tabla cumple 1FN/2FN/3FN:
//     si alguien añadiera una columna "sustituto de ID" o dejara alguna sin NOT NULL,
//     esta comprobación lo detecta.
// ---------------------------------------------------------------------------------------------
$columnas = $bdd->query( 'PRAGMA table_info(Medidas)' )->fetchAll();

$todasNotNull = true;
$pkEsID      = false;
$numeroColumnas = count( $columnas );

foreach ( $columnas as $columna ) {
  // 'notnull' vale 0 cuando la columna admite NULL y 1 cuando es NOT NULL.
  if ( (int) $columna['notnull'] !== 1 ) {
    $todasNotNull = false;
    fallo( "la columna " . $columna['name'] . " admite NULL, debería ser NOT NULL" );
  }
  if ( (int) $columna['pk'] === 1 && $columna['name'] === 'ID' ) {
    $pkEsID = true;
  }
}

if ( $numeroColumnas === 7 ) {
  ok( "comprobación 5: la tabla tiene las 7 columnas del diseño" );
  $comprobaciones[] = true;
} else {
  fallo( "comprobación 5: la tabla tiene " . $numeroColumnas . " columnas, deberían ser 7" );
  $comprobaciones[] = false;
}

if ( $todasNotNull ) {
  ok( "comprobación 6: las 7 columnas son NOT NULL" );
  $comprobaciones[] = true;
} else {
  fallo( "comprobación 6: hay columnas que NO son NOT NULL" );
  $comprobaciones[] = false;
}

if ( $pkEsID ) {
  ok( "comprobación 7: la clave primaria es la columna ID" );
  $comprobaciones[] = true;
} else {
  fallo( "comprobación 7: la clave primaria NO es la columna ID" );
  $comprobaciones[] = false;
}

// -------------------------------------------------------------------------------------------------
// 4) VEREDICTO
// -------------------------------------------------------------------------------------------------
paso( "4. Veredicto" );

$fallos = 0;
foreach ( $comprobaciones as $nota ) {
  if ( $nota === false ) {
    $fallos++;
  }
}

echo PHP_EOL . "    Comprobaciones: " . count( $comprobaciones )
   . "   Fallos: " . $fallos . PHP_EOL;

titulo( ( $fallos === 0 ) ? "TEST BD OK" : "TEST BD ERROR" );

// Código de salida: 0 si todo fue bien, 1 si hubo algún fallo.
// Esto permite encadenarlo en un terminal:
//     php testBD.php && echo "SIGUIENTE PASO"
exit( $fallos === 0 ? 0 : 1 );
