<?php
// ==================================================================================================
// DISEÑO LÓGICO: test automatizado del servidor REST (web_communication/testServidorREST.php)
// --------------------------------------------------------------------------------------------------
//   Arranca un servidor HTTP embebido para validar de forma integral:
//   - POST GuardarMedida.php con JSON válido (código 200 y error: 0).
//   - GET ObtenerMedidas.php con cuantasComoMaximo (código 200 y array de 4 campos).
//   - Rechazo de peticiones inválidas (error: 1).
//   - Desacoplamiento estricto: cero sentencias SQL en los endpoints de comunicación.
// ==================================================================================================

$phpBin  = PHP_BINARY;
$docRoot = __DIR__;
$puerto  = 8899;
$baseUrl = "http://127.0.0.1:$puerto";

$comprobaciones = 0;
$fallos = 0;

function verificar( $condicion, $mensaje ) {
    global $comprobaciones, $fallos;
    $comprobaciones++;
    if ( $condicion ) {
        echo "    OK    $mensaje\n";
    } else {
        $fallos++;
        echo "    FALLO $mensaje\n";
    }
}

echo "======================================================================\n";
echo "  TEST AUTOMATICO DEL SERVIDOR REST - web_communication\n";
echo "======================================================================\n\n";

echo "--> 0. Arranco el servidor HTTP de pruebas\n";
$descriptors = array(
    0 => array('pipe', 'r'),
    1 => array('pipe', 'w'),
    2 => array('pipe', 'w')
);
$proc = proc_open('"' . $phpBin . '" -S 127.0.0.1:' . $puerto . ' -t "' . $docRoot . '"', $descriptors, $pipes, $docRoot);
usleep(300000); // 300 ms para asegurar arranque
verificar( is_resource($proc), "servidor de pruebas levantado en $baseUrl" );

try {
    echo "\n--> 1. POST /GuardarMedida.php con los 5 campos requeridos\n";
    $optsPost = array(
        'http' => array(
            'method'        => 'POST',
            'header'        => "Content-Type: application/json\r\n",
            'content'       => json_encode( array(
                'uuid'          => 'EPSG-GTI-PROY-3A',
                'major'         => 2817,
                'minor'         => 235,
                'txPower'       => -53,
                'nombreEmisora' => 'GTI-Jose-Test'
            ) ),
            'ignore_errors' => true
        )
    );
    $respPost = file_get_contents( "$baseUrl/GuardarMedida.php", false, stream_context_create( $optsPost ) );
    $jsonPost = json_decode( $respPost, true );
    verificar( is_array( $jsonPost ) && isset( $jsonPost['error'] ) && $jsonPost['error'] === 0, "respuesta POST válida con error: 0 y mensaje de confirmación" );

    echo "\n--> 2. GET /ObtenerMedidas.php?cuantasComoMaximo=2\n";
    $respGet = file_get_contents( "$baseUrl/ObtenerMedidas.php?cuantasComoMaximo=2" );
    $jsonGet = json_decode( $respGet, true );
    $medidasOk = is_array( $jsonGet ) && isset( $jsonGet['medidas'] ) && is_array( $jsonGet['medidas'] );
    verificar( $medidasOk, "respuesta GET contiene array 'medidas'" );

    if ( $medidasOk && count( $jsonGet['medidas'] ) > 0 ) {
        $m = $jsonGet['medidas'][0];
        $camposValidos = !isset($m['id']) && isset($m['fecha']) && isset($m['idSensor']) && isset($m['tipoMedicion']) && isset($m['valorMedicion']);
        verificar( $camposValidos, "las medidas devueltas contienen los 4 campos canónicos (fecha, idSensor, tipoMedicion, valorMedicion, sin ID)" );
    }

    echo "\n--> 3. Rechazo de peticiones erróneas (error: 1)\n";
    $optsInvalido = array(
        'http' => array(
            'method'        => 'POST',
            'header'        => "Content-Type: application/json\r\n",
            'content'       => json_encode( array( 'uuid' => 'sin-campos-obligatorios' ) ),
            'ignore_errors' => true
        )
    );
    $respInvalido = file_get_contents( "$baseUrl/GuardarMedida.php", false, stream_context_create( $optsInvalido ) );
    $jsonInvalido = json_decode( $respInvalido, true );
    verificar( isset( $jsonInvalido['error'] ) && $jsonInvalido['error'] === 1, "petición incompleta rechazada con error: 1" );

    $respGetEnPost = file_get_contents( "$baseUrl/GuardarMedida.php" );
    $jsonGetEnPost = json_decode( $respGetEnPost, true );
    verificar( isset( $jsonGetEnPost['error'] ) && $jsonGetEnPost['error'] === 1, "GET a endpoint POST rechazado con error: 1" );

    echo "\n--> 4. Comprobación de regla de desacoplamiento (cero SQL en endpoints)\n";
    $codigoGuardar = file_get_contents( __DIR__ . '/GuardarMedida.php' );
    $codigoObtener = file_get_contents( __DIR__ . '/ObtenerMedidas.php' );
    $sinSQL = ! preg_match( '/\b(SELECT|INSERT|UPDATE|DELETE|sqlite:|PDO)\b/i', $codigoGuardar )
           && ! preg_match( '/\b(SELECT|INSERT|UPDATE|DELETE|sqlite:|PDO)\b/i', $codigoObtener );
    verificar( $sinSQL, "los endpoints no contienen sentencias SQL ni acceso directo a BBDD" );

} finally {
    // Parar servidor de pruebas
    if ( is_resource( $proc ) ) {
        proc_terminate( $proc );
        proc_close( $proc );
    }

    // Limpieza de datos creados en el test
    try {
        $rutaBD = __DIR__ . '/../web_database/bdd.sqlite';
        if ( file_exists( $rutaBD ) ) {
            $pdo = new PDO( 'sqlite:' . $rutaBD );
            $pdo->exec( "DELETE FROM Medidas WHERE Tipo_Medicion = 'CO2' AND Valor_Medicion = 235" );
        }
    } catch ( Exception $e ) {
    }
}

echo "\n    Comprobaciones: $comprobaciones   Fallos: $fallos\n\n";
echo "======================================================================\n";
if ( $fallos === 0 ) {
    echo "  TEST SERVIDOR REST OK\n";
    echo "======================================================================\n";
    exit( 0 );
} else {
    echo "  TEST SERVIDOR REST FALLO ($fallos fallos)\n";
    echo "======================================================================\n";
    exit( 1 );
}
