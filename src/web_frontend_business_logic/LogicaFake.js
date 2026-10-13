// ==================================================================================================
// DISEÑO LÓGICO: web_frontend_business_logic/LogicaFake.js (proxy de cliente para el navegador)
// --------------------------------------------------------------------------------------------------
//
//               -------- web_frontend_business_logic/LogicaFake.js --------
//               |
//   uuid: Text, major: N, minor: Z, txPower: Z,
//   nombreEmisora: Text --> guardarMedida() --> B
//               |
//   cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
//               |
//   cb: Callback --> obtenerMedidasFake() <--
//               |
//               -----------------------------------------------------------
//
//   Medida = (
//       fecha: Text,
//       idSensor: N,
//       tipoMedicion: Text,
//       valorMedicion: Z
//   )
//
// QUÉ ES ESTE FICHERO
//
// Es la capa proxy del frontend que encapsula el transporte HTTP hacia la API REST del backend.
// Proporciona exactamente la misma interfaz lógica que el backend:
//   - guardarMedida(...)
//   - obtenerMedidas(...)
//
// DESACOPLAMIENTO ESTRICTO:
// Este fichero NO accede al DOM ni manipula elementos HTML ni tablas. Su única responsabilidad
// es comunicarse con el backend y devolver estructuras de datos puras a la capa de presentación (GUI).
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

// Dirección del endpoint REST para obtener las mediciones registradas.
var RUTA_SERVICIO_MEDIDAS = '../web_communication/ObtenerMedidas.php?cuantasComoMaximo=100';

// Función auxiliar para registrar advertencias de red sin saturar la consola.
function avisarErrorRed( unMensaje ) {
  if ( typeof avisarUnaVez === 'function' ) {
    avisarUnaVez( unMensaje );
  } else if ( typeof console !== 'undefined' && typeof console.warn === 'function' ) {
    console.warn( unMensaje );
  }
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
// Qué hace: Interfaz proxy cliente idéntica al backend para obtener las medidas.
// -------------------------------------------------------------------------------------------------
function obtenerMedidas( cb ) {
  return obtenerMedidasFake( cb );
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
// Qué hace: Interfaz proxy cliente idéntica al backend para guardar una medida.
// -------------------------------------------------------------------------------------------------
function guardarMedida( uuid, major, minor, txPower, nombreEmisora, cb ) {
  if ( typeof cb === 'function' ) {
    cb( true );
  }
  return true;
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: cb: Callback --> obtenerMedidasFake() <--
// Qué hace: solicita mediante HTTP GET las medidas al servidor REST y las entrega al callback cb.
// -------------------------------------------------------------------------------------------------
function obtenerMedidasFake( cb ) {
  if ( typeof cb !== 'function' ) {
    return;
  }

  var laPeticion = new XMLHttpRequest();
  laPeticion.open( 'GET', RUTA_SERVICIO_MEDIDAS, true );

  laPeticion.onload = function () {
    var laRespuesta = null;

    try {
      laRespuesta = JSON.parse( laPeticion.responseText );
    } catch ( elError ) {
      avisarErrorRed( 'obtenerMedidasFake(): la respuesta del servidor no es JSON: ' + elError.message );
      cb( [] );
      return;
    }

    if ( laPeticion.status < 200 || laPeticion.status >= 300 ) {
      avisarErrorRed( 'obtenerMedidasFake(): el servidor ha contestado con el código ' + laPeticion.status );
      cb( [] );
      return;
    }

    if ( laRespuesta === null || ! laRespuesta.medidas ) {
      cb( [] );
      return;
    }

    cb( laRespuesta.medidas );
  };

  laPeticion.onerror = function () {
    avisarErrorRed( 'obtenerMedidasFake(): no se ha podido contactar con el servidor. ¿Está levantado?' );
    cb( [] );
  };

  laPeticion.send();
} // ()
