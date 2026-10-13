<?php
// ==================================================================================================
// DISEÑO LÓGICO: test automatizado de la base de datos (web_database/testBD.php)
// --------------------------------------------------------------------------------------------------
//   Verifica la creación del esquema SQLite, integridad estructural de la tabla Medidas,
//   las 5 columnas relacionales, e inserción/lectura/borrado de registros de prueba.
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
    echo "    OK   ejecutado crearBD.sql (CREATE TABLE IF NOT EXISTS Medidas)\n";

    $stmtTabla = $pdo->query( "SELECT name FROM sqlite_master WHERE type='table' AND name='Medidas'" );
    $tablaExiste = $stmtTabla->fetch();
    if ( ! $tablaExiste ) {
        echo "    FALLO: la tabla 'Medidas' no existe en la base de datos\n";
        exit( 1 );
    }
    echo "    OK   la tabla 'Medidas' existe\n\n";

    echo "--> 2. Inserto un registro de prueba\n";
    $sentenciaInsert = $pdo->prepare(
        "INSERT INTO Medidas (Fecha, ID_Sensor, Tipo_Medicion, Valor_Medicion)
         VALUES (datetime('now','localtime'), :idSensor, :tipo, :valor)"
    );
    $sentenciaInsert->execute( array(
        ':idSensor' => 99,
        ':tipo'     => 'TEST_VERIFICACION',
        ':valor'    => 777
    ) );
    $idInsertado = (int) $pdo->lastInsertId();
    if ( $idInsertado <= 0 ) {
        echo "    FALLO: no se pudo obtener el ID autoincremental insertado\n";
        exit( 1 );
    }
    echo "    OK   se ha insertado el registro y se le ha dado el ID $idInsertado\n\n";

    echo "--> 3. Leo el registro que acabo de insertar\n";
    $sentenciaSelect = $pdo->prepare( "SELECT * FROM Medidas WHERE ID = :id" );
    $sentenciaSelect->execute( array( ':id' => $idInsertado ) );
    $registro = $sentenciaSelect->fetch();

    if ( ! $registro ) {
        echo "    FALLO: la consulta SELECT no devolvió el registro insertado\n";
        exit( 1 );
    }
    echo "    OK   la consulta SELECT ha devuelto el registro $idInsertado\n";

    $columnasEsperadas = array( 'ID', 'Fecha', 'ID_Sensor', 'Tipo_Medicion', 'Valor_Medicion' );
    foreach ( $columnasEsperadas as $col ) {
        if ( ! array_key_exists( $col, $registro ) ) {
            echo "    FALLO: falta la columna '$col' en el registro\n";
            exit( 1 );
        }
    }
    echo "    OK   las 5 columnas se llaman exactamente como en el diseño (ID, Fecha, ID_Sensor, Tipo_Medicion, Valor_Medicion)\n";

    if ( (int) $registro['ID_Sensor'] !== 99 || $registro['Tipo_Medicion'] !== 'TEST_VERIFICACION' || (int) $registro['Valor_Medicion'] !== 777 ) {
        echo "    FALLO: los datos leídos no coinciden con los insertados\n";
        exit( 1 );
    }
    echo "    OK   el registro leído coincide con lo que se insertó\n\n";

    echo "--> 4. Borro el registro de prueba\n";
    $sentenciaDelete = $pdo->prepare( "DELETE FROM Medidas WHERE ID = :id" );
    $sentenciaDelete->execute( array( ':id' => $idInsertado ) );
    echo "    OK   se ha borrado el registro\n\n";

    echo "======================================================================\n";
    echo "  TEST BD OK\n";
    echo "======================================================================\n";
    exit( 0 );

} catch ( Exception $e ) {
    echo "    FALLO (Excepción): " . $e->getMessage() . "\n";
    exit( 1 );
}
