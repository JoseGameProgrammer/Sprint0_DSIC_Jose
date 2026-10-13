<?php
// ==================================================================================================
// DISEÑO LÓGICO: test automatizado de la lógica de negocio (web_business_logic/testLogicaMedidas.php)
// --------------------------------------------------------------------------------------------------
//   Verifica las funciones guardarMedida() y obtenerMedidas() con el modelo canónico de 5 campos,
//   comprobando decodificación de major a Tipo_Medicion, valores con signo (minor), validación
//   de tipos y comportamiento del límite de consulta.
// ==================================================================================================

require_once __DIR__ . '/LogicaMedidas.php';

$fallos = 0;
$pruebas = 0;

function verificar( $condicion, $mensaje ) {
    global $fallos, $pruebas;
    $pruebas++;
    if ( $condicion ) {
        echo "    OK    $mensaje\n";
    } else {
        $fallos++;
        echo "    FALLO $mensaje\n";
    }
}

echo "======================================================================\n";
echo "  TEST AUTOMATICO DE LA LOGICA DEL NEGOCIO - LogicaMedidas.php\n";
echo "======================================================================\n\n";

echo "--> 1. Prueba de guardarMedida() con magnitud CO2\n";
$resCO2 = guardarMedida( 'EPSG-GTI-PROY-3A', 2817, 235, -53, 'GTI-Jose' );
verificar( $resCO2 === true, "guardarMedida() para CO2 (major 2817, minor 235) devuelve true" );

echo "\n--> 2. Prueba de obtenerMedidas() y verificación de las 5 claves del modelo\n";
$medidas = obtenerMedidas( 5 );
verificar( is_array( $medidas ) && count( $medidas ) > 0, "obtenerMedidas() devuelve un array con datos" );

$primera = isset( $medidas[0] ) ? $medidas[0] : null;
$clavesValidas = $primera !== null
    && array_key_exists( 'id', $primera )
    && array_key_exists( 'fecha', $primera )
    && array_key_exists( 'idSensor', $primera )
    && array_key_exists( 'tipoMedicion', $primera )
    && array_key_exists( 'valorMedicion', $primera );
verificar( $clavesValidas, "el registro obtenido contiene exactamente los 5 campos del diseño" );
verificar( $primera && $primera['tipoMedicion'] === 'CO2' && $primera['valorMedicion'] === 235, "la medida más reciente decodifica 'CO2' y valor 235" );

echo "\n--> 3. Prueba de guardarMedida() con magnitud TEMPERATURA y valor negativo con signo\n";
$resTemp = guardarMedida( 'EPSG-GTI-PROY-3A', 3073, -13, -53, 'GTI-Jose' );
verificar( $resTemp === true, "guardarMedida() para TEMPERATURA (major 3073, minor -13) devuelve true" );

$medidasTemp = obtenerMedidas( 1 );
$ultimaTemp = isset( $medidasTemp[0] ) ? $medidasTemp[0] : null;
verificar( $ultimaTemp && $ultimaTemp['tipoMedicion'] === 'TEMPERATURA' && $ultimaTemp['valorMedicion'] === -13, "temperatura almacenada conserva signo negativo (-13)" );

echo "\n--> 4. Pruebas de validación de entradas inválidas\n";
$resInvalido = guardarMedida( 'EPSG-GTI-PROY-3A', 'no_es_entero', 235, -53, 'GTI-Jose' );
verificar( $resInvalido === false, "guardarMedida() rechaza major no numérico devolviendo false" );

$resLimiteInvalido = obtenerMedidas( -5 );
verificar( is_array( $resLimiteInvalido ) && count( $resLimiteInvalido ) === 0, "obtenerMedidas() con límite negativo devuelve array vacío sin fallar" );

// Limpieza de las medidas insertadas en el test
try {
    $bdd = conectarBBDD();
    $stmtDel = $bdd->prepare( "DELETE FROM Medidas WHERE Tipo_Medicion IN ('CO2', 'TEMPERATURA') AND Valor_Medicion IN (235, -13)" );
    $stmtDel->execute();
} catch ( Exception $e ) {
    // Si no se pueden borrar, no detiene el test
}

echo "\n    Pruebas: $pruebas   Fallos: $fallos\n\n";
echo "======================================================================\n";
if ( $fallos === 0 ) {
    echo "  TEST LOGICA BACKEND OK\n";
    echo "======================================================================\n";
    exit( 0 );
} else {
    echo "  TEST LOGICA BACKEND FALLO ($fallos fallos)\n";
    echo "======================================================================\n";
    exit( 1 );
}
