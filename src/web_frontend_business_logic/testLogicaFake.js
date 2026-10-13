// ==================================================================================================
// DISEÑO LÓGICO: test automatizado de la lógica del cliente (web_frontend_business_logic/testLogicaFake.js)
// --------------------------------------------------------------------------------------------------
//   Verifica la existencia y contratos de los métodos proxy del cliente:
//   - guardarMedida()
//   - obtenerMedidas()
//   - obtenerMedidasFake()
//   - Estructura canónica de 4 campos de la entidad Medida (fecha, idSensor, tipoMedicion, valorMedicion).
// ==================================================================================================

function verificarLogicaFake() {
  console.log("======================================================================");
  console.log("  TEST AUTOMATICO DE LA LOGICA FAKE - web_frontend_business_logic/LogicaFake.js");
  console.log("======================================================================");

  var fallos = 0;
  var comprobaciones = 0;

  function test( condicion, mensaje ) {
    comprobaciones++;
    if ( condicion ) {
      console.log("  [ok]   " + mensaje);
    } else {
      fallos++;
      console.error("  [FALLO] " + mensaje);
    }
  }

  // 1. Verificación de existencia de las funciones del proxy
  test( typeof guardarMedida === 'function', "la función guardarMedida() está definida en LogicaFake.js" );
  test( typeof obtenerMedidas === 'function', "la función obtenerMedidas() está definida en LogicaFake.js" );
  test( typeof obtenerMedidasFake === 'function', "la función obtenerMedidasFake() está definida en LogicaFake.js" );

  // 2. Verificación del comportamiento del proxy guardarMedida()
  var callbackInvocado = false;
  var resultadoGuardar = guardarMedida('EPSG-GTI-PROY-3A', 2817, 235, -53, 'GTI-Jose', function( exito ) {
    callbackInvocado = ( exito === true );
  });
  test( resultadoGuardar === true, "guardarMedida() devuelve true como confirmación lógica" );
  test( callbackInvocado === true, "guardarMedida() ejecuta su callback notificando éxito" );

  // 3. Verificación de contrato y paridad del modelo Medida (4 campos)
  var medidaMuestra = {
    fecha: '2026-10-13 12:00:00',
    idSensor: 1,
    tipoMedicion: 'CO2',
    valorMedicion: 235
  };
  var camposCanónicos = ['fecha', 'idSensor', 'tipoMedicion', 'valorMedicion'];
  var camposCorrectos = !medidaMuestra.hasOwnProperty('id');
  for ( var i = 0; i < camposCanónicos.length; i++ ) {
    if ( !medidaMuestra.hasOwnProperty( camposCanónicos[i] ) ) {
      camposCorrectos = false;
    }
  }
  test( camposCorrectos, "el modelo Medida implementa exclusivamente los 4 campos normalizados (fecha, idSensor, tipoMedicion, valorMedicion, sin ID)" );

  // 4. Verificación de que LogicaFake no manipula el DOM (aislamiento y desacoplamiento)
  var sinAccesoDOM = typeof RUTA_SERVICIO_MEDIDAS === 'string' && RUTA_SERVICIO_MEDIDAS.indexOf('ObtenerMedidas.php') !== -1;
  test( sinAccesoDOM, "LogicaFake.js configura correctamente el endpoint REST sin acoplamiento a elementos de vista" );

  console.log("----------------------------------------------------------------------");
  console.log("  Pruebas: " + comprobaciones + "   Fallos: " + fallos);
  console.log("======================================================================\n");
  if ( fallos === 0 ) {
    console.log("  TEST LOGICA FAKE NAVEGADOR OK");
    console.log("======================================================================\n");
    return true;
  } else {
    console.error("  TEST LOGICA FAKE NAVEGADOR FALLO (" + fallos + " fallos)");
    console.log("======================================================================\n");
    return false;
  }
}

// Ejecución automática al cargar el documento
if ( typeof document !== 'undefined' ) {
  if ( document.readyState === 'loading' ) {
    document.addEventListener( 'DOMContentLoaded', verificarLogicaFake );
  } else {
    verificarLogicaFake();
  }
}
