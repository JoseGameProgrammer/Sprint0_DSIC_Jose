<?php
// ==================================================================================================
// DISEÑO LÓGICO: test automatizado de la base de datos (web_database/testBD.php)
// --------------------------------------------------------------------------------------------------
//   Verifica la creación del esquema SQLite con clave primaria compuesta (ID_Sensor, Fecha),
//   integridad estructural de la tabla Medidas (sin columna ID), las 4 columnas relacionales,
//   e inserción/lectura/borrado de registros de prueba.
// ==================================================================================================

$rutaSQL = __DIR__ . '/crearBD.sql';
$rutaBD  = __DIR__ . '/bdd.sqlite';

echo "======================================================================\n";
echo "  TEST AUTOMATICO DE LA BBDD - Tabla Medidas\n";
echo "======================================================================\n\n";

echo "--> Fichero SQL  : $rutaSQL\n";
echo "--> Base de datos: $rutaBD\n";

if ( ! file_exists( $rutaSQL ) ) {
    echo "    FALLO: no existe el fichero crearBD.sql\n";
    exit( 1 );
}
echo "    OK   existe el fichero crearBD.sql\n\n";

try {
    echo "--> 1. Abro la base de datos y creo la tabla Medidas\n";
    $pdo = new PDO( 'sqlite:' . $rutaBD );
    $pdo->setAttribute( PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION );
    $pdo->setAttribute( PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC );
    echo "    OK   conectado a la base de datos SQLite\n";

    $sql = file_get_contents( $rutaSQL );
    $pdo->exec( $sql );
    echo "    OK   ejecutado crearBD.sql (CREATE TABLE IF NOT EXISTS Medidas con PRIMARY KEY (ID_Sensor, Fecha))\n";

    $stmtTabla = $pdo->query( "SELECT name FROM sqlite_master WHERE type='table' AND name='Medidas'" );
    $tablaExiste = $stmtTabla->fetch();
    if ( ! $tablaExiste ) {
        echo "    FALLO: la tabla 'Medidas' no existe en la base de datos\n";
        exit( 1 );
    }
    echo "    OK   la tabla 'Medidas' existe\n\n";

    echo "--> 2. Inserto un registro de prueba con clave primaria compuesta\n";
    $fechaPrueba = '2099-01-01 12:00:00';
    $idSensorPrueba = 99;
    $tipoPrueba = 'TEST_VERIFICACION';
    $valorPrueba = 777;

    $sentenciaInsert = $pdo->prepare(
        "INSERT INTO Medidas (Fecha, ID_Sensor, Tipo_Medicion, Valor_Medicion)
         VALUES (:fecha, :idSensor, :tipo, :valor)"
    );
    $sentenciaInsert->execute( array(
        ':fecha'    => $fechaPrueba,
        ':idSensor' => $idSensorPrueba,
        ':tipo'     => $tipoPrueba,
        ':valor'    => $valorPrueba
    ) );
    echo "    OK   se ha insertado el registro con clave (ID_Sensor=$idSensorPrueba, Fecha='$fechaPrueba')\n\n";

    echo "--> 3. Leo el registro que acabo de insertar mediante su clave compuesta\n";
    $sentenciaSelect = $pdo->prepare( "SELECT * FROM Medidas WHERE ID_Sensor = :idSensor AND Fecha = :fecha" );
    $sentenciaSelect->execute( array(
        ':idSensor' => $idSensorPrueba,
        ':fecha'    => $fechaPrueba
    ) );
    $registro = $sentenciaSelect->fetch();

    if ( ! $registro ) {
        echo "    FALLO: la consulta SELECT no devolvió el registro insertado\n";
        exit( 1 );
    }
    echo "    OK   la consulta SELECT ha devuelto el registro\n";

    $columnasEsperadas = array( 'Fecha', 'ID_Sensor', 'Tipo_Medicion', 'Valor_Medicion' );
    foreach ( $columnasEsperadas as $col ) {
        if ( ! array_key_exists( $col, $registro ) ) {
            echo "    FALLO: falta la columna '$col' en el registro\n";
            exit( 1 );
        }
    }
    if ( array_key_exists( 'ID', $registro ) ) {
        echo "    FALLO: la columna obsoleta 'ID' sigue existiendo en el registro\n";
        exit( 1 );
    }
    echo "    OK   las 4 columnas se llaman exactamente como en el diseño (Fecha, ID_Sensor, Tipo_Medicion, Valor_Medicion)\n";

    if ( (int) $registro['ID_Sensor'] !== $idSensorPrueba || $registro['Tipo_Medicion'] !== $tipoPrueba || (int) $registro['Valor_Medicion'] !== $valorPrueba ) {
        echo "    FALLO: los datos leídos no coinciden con los insertados\n";
        exit( 1 );
    }
    echo "    OK   el registro leído coincide con lo que se insertó\n\n";

    echo "--> 4. Verifico restricción de clave primaria duplicada\n";
    $claveDuplicadaRechazada = false;
    try {
        $sentenciaDuplicada = $pdo->prepare(
            "INSERT INTO Medidas (Fecha, ID_Sensor, Tipo_Medicion, Valor_Medicion)
             VALUES (:fecha, :idSensor, :tipo, :valor)"
        );
        $sentenciaDuplicada->execute( array(
            ':fecha'    => $fechaPrueba,
            ':idSensor' => $idSensorPrueba,
            ':tipo'     => 'DUPLICADO',
            ':valor'    => 999
        ) );
    } catch ( PDOException $ex ) {
        $claveDuplicadaRechazada = true;
    }
    if ( ! $claveDuplicadaRechazada ) {
        echo "    FALLO: la clave primaria compuesta no rechazó la inserción duplicada\n";
        exit( 1 );
    }
    echo "    OK   la clave primaria compuesta (ID_Sensor, Fecha) rechaza correctamente duplicados\n\n";

    echo "--> 5. Borro el registro de prueba\n";
    $sentenciaDelete = $pdo->prepare( "DELETE FROM Medidas WHERE ID_Sensor = :idSensor AND Fecha = :fecha" );
    $sentenciaDelete->execute( array(
        ':idSensor' => $idSensorPrueba,
        ':fecha'    => $fechaPrueba
    ) );
    echo "    OK   se ha borrado el registro\n\n";

    echo "======================================================================\n";
    echo "  TEST BD OK\n";
    echo "======================================================================\n";
    exit( 0 );

} catch ( Exception $e ) {
    echo "    FALLO (Excepción): " . $e->getMessage() . "\n";
    exit( 1 );
}
