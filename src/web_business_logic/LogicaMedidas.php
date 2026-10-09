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
  define( 'RUTA_ESQUEMA_BBDD', __DIR__ . '/../web_database/crearBD.sql' );
}
if ( ! defined( 'RUTA_FICHERO_BBDD' ) ) {
  define( 'RUTA_FICHERO_BBDD', __DIR__ . '/../web_database/bdd.sqlite' );
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
    if ( filter_var( $major, FILTER_VALIDATE_INT ) === false ) return false;
    if ( filter_var( $minor, FILTER_VALIDATE_INT ) === false ) return false;

    $tipo = $major >> 8;
    $tipo_texto = "DESCONOCIDO";
    if ($tipo == 11) $tipo_texto = "CO2";
    else if ($tipo == 12) $tipo_texto = "TEMPERATURA";
    else if ($tipo == 13) $tipo_texto = "RUIDO";
    else if ($major == 11 || $major == 12 || $major == 13) {
        if ($major == 11) $tipo_texto = "CO2";
        if ($major == 12) $tipo_texto = "TEMPERATURA";
        if ($major == 13) $tipo_texto = "RUIDO";
    }

    $valor = (int)$minor;
    $idSensor = 1;

    try {
        $bdd = conectarBBDD();
        $sentencia = $bdd->prepare(
            "INSERT INTO Medidas (Fecha, ID_Sensor, Tipo_Medicion, Valor_Medicion)
             VALUES (datetime('now','localtime'), :idSensor, :tipo, :valor)" );

        $sentencia->bindValue( ':idSensor', $idSensor, PDO::PARAM_INT );
        $sentencia->bindValue( ':tipo', $tipo_texto );
        $sentencia->bindValue( ':valor', $valor, PDO::PARAM_INT );

        return $sentencia->execute();
    } catch ( Exception $e ) {
        error_log( 'guardarMedida(): error: ' . $e->getMessage() );
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
  $limite = filter_var( $cuantasComoMaximo, FILTER_VALIDATE_INT );
  if ( $limite === false || $limite < 1 ) {
    return array();
  }

  try {
    $bdd = conectarBBDD();
    $sentencia = $bdd->prepare(
      "SELECT ID as id,
              Fecha as fecha,
              ID_Sensor as idSensor,
              Tipo_Medicion as tipoMedicion,
              Valor_Medicion as valorMedicion
         FROM Medidas
        ORDER BY ID DESC
        LIMIT :limite" );

    $sentencia->bindValue( ':limite', (int) $limite, PDO::PARAM_INT );
    $sentencia->execute();
    $filas = $sentencia->fetchAll();
  } catch ( Exception $e ) {
    error_log( 'obtenerMedidas(): error al leer las medidas: ' . $e->getMessage() );
    return array();
  }

  $lasMedidas = array();
  foreach ( $filas as $laFila ) {
    $lasMedidas[] = array(
      'id'            => (int) $laFila['id'],
      'fecha'         => (string) $laFila['fecha'],
      'idSensor'      => (int) $laFila['idSensor'],
      'tipoMedicion'  => (string) $laFila['tipoMedicion'],
      'valorMedicion' => (int) $laFila['valorMedicion']
    );
  }
  return $lasMedidas;
} // ()
?>