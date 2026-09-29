<?php

// ==================================================================================================
// DISEÑO LÓGICO: capa de lógica del negocio de las medidas
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
//   TablaMedidas = [ Medida ]
//
//                    ------------- logica/LogicaMedidas.php -------------
//                    |
//   uuid: Text, major: N, minor: Z, txPower: Z,
//   nombreEmisora: Text
//         --> guardarMedida() -->
//     B <--
//                    |
//                    |
//   cuantasComoMaximo: N
//         --> obtenerMedidas() <--
//   [ Medida ] <--
//                    |
//                       --> conectarBBDD() --x
//                    |
//                    ----------------------------------------------------
//
// QUÉ ES ESTA CAPA
//
// Esta es la lógica VERDADERA del negocio. Aquí NO se sabe nada de HTTP, ni de
// sesiones, ni de $_POST, ni de json_encode(): son funciones que reciben
// parámetros normales y devuelven datos normales. Quien las llama (el endpoint
// REST del prompt 3) es quien se encarga de traducir de la petición web a
// estos tipos lógicos, y de traducir la respuesta a JSON.
//
// SEPARAR ESTAS DOS COSAS ES LO IMPORTANTE: si el día de mañana se cambia el
// transporte (de REST a GraphQL, o a una cola de mensajes), estas funciones no
// se tocan, porque no saben nada del transporte.
//
// OJO CON LAS FLECHAS DE LA DERECHA, que indican si la función MODIFICA el
// estado o solo lo LEE:
//   guardarMedida()    -->  inserta una fila: MODIFICA la base de datos.
//   obtenerMedidas()  <--  solo hace un SELECT: NO la modifica.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

// -------------------------------------------------------------------------------------------------
// Rutas de la base de datos y del esquema.
// Se pueden sobrescribir ANTES de incluir este fichero, desde otro fichero, si algún día
// el servidor usase otra ruta. Por eso van con "defined() ||" y no con un define() a secas.
// -------------------------------------------------------------------------------------------------
if ( ! defined( 'RUTA_ESQUEMA_BBDD' ) ) {
  define( 'RUTA_ESQUEMA_BBDD', __DIR__ . '/../bdd/crearBD.sql' );
}
if ( ! defined( 'RUTA_FICHERO_BBDD' ) ) {
  define( 'RUTA_FICHERO_BBDD', __DIR__ . '/../bdd/bdd.sqlite' );
}

// -------------------------------------------------------------------------------------------------
// Conexión a la base de datos, reutilizada entre llamadas.
//
// DISEÑO: --> conectarBBDD() --x
//   No recibe nada y no devuelve nada en la notación: es un "efecto" que deja
//   lista la conexión. Se marca --x (estática) porque su resultado no depende de
//   ninguna instancia: depende solo de las rutas de arriba.
//
//   En la práctica devuelve el objeto PDO, que es como se puede usar de verdad.
//   Se guarda en una variable estática para no abrir una conexión nueva en cada
//   llamada a guardarMedida() u obtenerMedidas().
// -------------------------------------------------------------------------------------------------
function conectarBBDD() {

  static $laConexion = null;

  // Si ya hay conexión, se reutiliza.
  if ( $laConexion instanceof PDO ) {
    return $laConexion;
  }

  try {

    $laConexion = new PDO( 'sqlite:' . RUTA_FICHERO_BBDD );

    // Para que los errores de la BBDD se lancen como excepciones PHP y no se
    // pierdan en silencio. Sin esto, un INSERT mal escrito devolvería false sin
    // decir por qué.
    $laConexion->setAttribute( PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION );

    // Se devuelven las filas como arrays asociativos ('Uuid' => ..., 'Major' => ...).
    $laConexion->setAttribute( PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC );

    // Se aplica el esquema por si la tabla no existiera todavía.
    // Es un "CREATE TABLE IF NOT EXISTS", así que es idempotente: se puede ejecutar
    // en cada conexión sin coste ni daño. Así el endpoint REST no peta con
    // "no such table: Medidas" si alguien no ha ejecutado crearBD.sql antes.
    if ( file_exists( RUTA_ESQUEMA_BBDD ) ) {
      $laConexion->exec( file_get_contents( RUTA_ESQUEMA_BBDD ) );
    }

  } catch ( Exception $e ) {

    // Si no hay conexión no hay nada que hacer: se propaga el error hacia arriba.
    throw new RuntimeException(
      'conectarBBDD(): no se ha podido conectar con la base de datos: ' . $e->getMessage() );

  }

  return $laConexion;

} // ()


// -------------------------------------------------------------------------------------------------
// uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
//
// Qué hace: inserta UNA medida nueva en la tabla Medidas.
//
//   MODIFICA el estado de la base de datos (flecha --> a la derecha), y devuelve
//   un booleano: true si se ha guardado, false si los datos no eran válidos o si
//   la base de datos ha rechazado la inserción.
//
//   OJO: aquí NO se manda el ID (lo pone la base de datos con AUTOINCREMENT) ni se
//   inventa nada más. La fecha la pone la propia sentencia con datetime('now').
//
//   SEGURIDAD: la consulta va PREPARADA con parámetros con nombre (:uuid, :major...)
//   y nunca se concatenan valores dentro del texto SQL. Concatenar sería abrir la
//   puerta a la inyección SQL.
// -------------------------------------------------------------------------------------------------
function guardarMedida( $uuid, $major, $minor, $txPower, $nombreEmisora ) {

  // --- VALIDACIÓN DE LOS TEXTO: no pueden estar vacíos -----------------------------------
  if ( ! is_string( $uuid ) || trim( $uuid ) === '' ) {
    return false;
  }
  if ( ! is_string( $nombreEmisora ) || trim( $nombreEmisora ) === '' ) {
    return false;
  }

  // --- VALIDACIÓN DE LOS NÚMEROS: tienen que ser enteros ---------------------------------
  // OJO con el "=== false": filter_var() devuelve el entero 0 cuando el valor ES 0,
  // y 0 es "falso" en PHP. Si se comparara con "if ( ! $x )" se rechazaría de más
  // cualquier medida cuyo minor valiera 0.
  if ( filter_var( $major, FILTER_VALIDATE_INT ) === false ) {
    return false;
  }
  if ( filter_var( $minor, FILTER_VALIDATE_INT ) === false ) {
    return false;
  }
  if ( filter_var( $txPower, FILTER_VALIDATE_INT ) === false ) {
    return false;
  }

  try {

    $bdd = conectarBBDD();

    // Se PREPARA la consulta. Los :nombre son huecos, no texto: los valores van aparte.
    $sentencia = $bdd->prepare(
      "INSERT INTO Medidas (FechaLectura, Uuid, Major, Minor, TxPower, NombreEmisora)
       VALUES (datetime('now','localtime'), :uuid, :major, :minor, :txPower, :nombreEmisora)" );

    // Se ASIGNAN los parámetros a los huecos.
    $sentencia->bindValue( ':uuid',          trim( $uuid ) );
    $sentencia->bindValue( ':major',         (int) $major,        PDO::PARAM_INT );
    $sentencia->bindValue( ':minor',         (int) $minor,        PDO::PARAM_INT );
    $sentencia->bindValue( ':txPower',       (int) $txPower,      PDO::PARAM_INT );
    $sentencia->bindValue( ':nombreEmisora', trim( $nombreEmisora ) );

    // Se EJECUTA. execute() devuelve true si la fila se ha insertado.
    return $sentencia->execute();

  } catch ( Exception $e ) {

    // MANEJO DE ERRORES: si la BBDD falla (tabla que no existe, fichero bloqueado,
    // disco lleno...) la función devuelve false en vez de reventar el servidor.
    // El detalle del error se deja en el log del servidor, no se enseña al cliente.
    error_log( 'guardarMedida(): error al insertar la medida: ' . $e->getMessage() );

    return false;

  }

} // ()


// -------------------------------------------------------------------------------------------------
// cuantasComoMaximo: N --> obtenerMedidas() --> [ Medida ]
//
// Qué hace: devuelve una lista con las últimas medidas guardadas.
//
//   Solo LEE el estado de la base de datos (flecha <-- a la derecha), no la modifica.
//
//   Se ordena por ID DESC, así que lo primero del array es la medida más reciente.
//   "cuantasComoMaximo" es el tope: nunca se devuelven más de las pedidas.
//
//   Cada elemento devuelto es un array asociativo con las 7 claves del tipo Medida:
//   id, uuid, major, minor, txPower, nombreEmisora, fechaLectura.
// -------------------------------------------------------------------------------------------------
function obtenerMedidas( $cuantasComoMaximo = 100 ) {

  // El límite tiene que ser un entero positivo: si no, no se consulta nada
  // (un LIMIT 0 o un LIMIT negativo es un error de SQL).
  $limite = filter_var( $cuantasComoMaximo, FILTER_VALIDATE_INT );
  if ( $limite === false || $limite < 1 ) {
    return array();
  }

  try {

    $bdd = conectarBBDD();

    // Se PREPARA la consulta. Los alias "as id", "as uuid"...
    // sirven para que las claves del array sean exactamente las del tipo Medida,
    // en minúsculas y en camelCase, y no los nombres de las columnas de la tabla.
    $sentencia = $bdd->prepare(
      "SELECT ID as id,
              Uuid as uuid,
              Major as major,
              Minor as minor,
              TxPower as txPower,
              NombreEmisora as nombreEmisora,
              FechaLectura as fechaLectura
         FROM Medidas
        ORDER BY ID DESC
        LIMIT :limite" );

    // El límite se pasa como ENTERO, no como texto. Por eso el PDO::PARAM_INT:
    // algunos motores rechazan un LIMIT con un parámetro de tipo texto.
    $sentencia->bindValue( ':limite', (int) $limite, PDO::PARAM_INT );

    $sentencia->execute();

    $filas = $sentencia->fetchAll();

  } catch ( Exception $e ) {

    error_log( 'obtenerMedidas(): error al leer las medidas: ' . $e->getMessage() );

    // Si falla la consulta se devuelve una lista vacía. El endpoint REST lo
    // traducirá a {"error": "..."} en vez de devolver medias verdades.
    return array();

  }

  // --- se ajustan los tipos al tipo lógico Medida ------------------------------------------
  // Con SQLite, PDO ya devuelve los enteros como enteros y los textos como texto.
  // Pero con MySQL, PDO devuelve TODO como texto salvo que se pida lo contrario, y
  // entonces major/minor/txPower llegarían como "2817" en vez de 2817. Con estos
  // (int) la función cumple su contrato (major: N, minor: Z, txPower: Z) sea cual
  // sea el motor, y quien la llame no tiene que acordarse de convertir.
  $lasMedidas = array();

  foreach ( $filas as $laFila ) {
    $lasMedidas[] = array(
      'id'            => (int) $laFila['id'],
      'uuid'          => (string) $laFila['uuid'],
      'major'         => (int) $laFila['major'],
      'minor'         => (int) $laFila['minor'],
      'txPower'       => (int) $laFila['txPower'],
      'nombreEmisora' => (string) $laFila['nombreEmisora'],
      'fechaLectura'  => (string) $laFila['fechaLectura'],
    );
  }

  return $lasMedidas;

} // ()

?>
