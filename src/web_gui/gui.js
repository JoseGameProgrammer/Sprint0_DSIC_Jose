// ==================================================================================================
// DISEÑO LÓGICO: web_gui/gui.js (interfaz gráfica de usuario en el navegador)
// --------------------------------------------------------------------------------------------------
//
//                      ---------------- web_gui/gui.js ----------------
//                      |
//   unaMedida: Medida      --> pintarLaUltimaMedida() -->
//   lasMedidas: [ Medida ] --> pintarLasMedidasAnteriores() -->
//   unaMedida: Medida      --> filaDeMedida() --> [ Text ]
//   unValor: Text          --> celdaDe() --> [ Text ]
//   unTexto: Text          --> filaDe() --> [ Text ]
//   unMensaje: Text        --> avisarUnaVez() -->
//                          --> refrescarMedidas() -->
//                          --> iniciarRefresco() -->
//                          --> detenerRefresco() -->
//                      |
//                      ------------------------------------------------
//
// QUÉ ES ESTE FICHERO
//
// Es la capa de presentación pura (GUI) para el navegador web. Se encarga de manipular el DOM:
// renderizar la tarjeta de la última medición recibida y la tabla de mediciones anteriores,
// así como gestionar el temporizador periódico de refresco.
//
// REGLA DE DESACOPLAMIENTO ESTRICTO:
// Esta capa NO realiza peticiones HTTP directas (ni fetch ni XMLHttpRequest ni sockets).
// Toda la obtención de datos se delega exclusivamente al proxy del cliente:
// `obtenerMedidas()` en `web_frontend_business_logic/LogicaFake.js`.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

// Cada cuántos milisegundos se vuelve a pedir y a pintar las medidas (3 segundos).
var MILISEGUNDOS_REFRESCO = 3000;

// Guarda el identificador del temporizador del refresco automático para poder detenerlo.
var idDelRefresco = null;

// Registro de mensajes avisados para evitar saturar la consola o la UI.
var mensajesYaAvisados = {};

// -------------------------------------------------------------------------------------------------
// DISEÑO: unMensaje: Text --> avisarUnaVez() -->
// Qué hace: escribe un aviso en la consola, pero solo la primera vez que se llama con ese texto.
// -------------------------------------------------------------------------------------------------
function avisarUnaVez( unMensaje ) {
  if ( mensajesYaAvisados.hasOwnProperty( unMensaje ) ) {
    return;
  }
  mensajesYaAvisados[ unMensaje ] = true;
  if ( window.console && typeof window.console.warn === 'function' ) {
    window.console.warn( unMensaje );
  }
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: unValor: Text, elNumeroDeColumnas: N --> celdaDe() --> [ Text ]
// Qué hace: devuelve un elemento <td> con el texto dentro y opcionalmente colSpan especificado.
// -------------------------------------------------------------------------------------------------
function celdaDe( unValor, elNumeroDeColumnas ) {
  var laCelda = document.createElement( 'td' );
  laCelda.textContent = ( unValor !== undefined && unValor !== null ) ? String( unValor ) : '';

  if ( elNumeroDeColumnas && elNumeroDeColumnas > 1 ) {
    laCelda.colSpan = elNumeroDeColumnas;
    laCelda.className = 'sin-medidas';
  }

  return laCelda;
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: unTexto: Text --> filaDe() --> [ Text ]
// Qué hace: devuelve un elemento <tr> con una sola celda que abarca las 5 columnas.
// -------------------------------------------------------------------------------------------------
function filaDe( unTexto ) {
  var laFila = document.createElement( 'tr' );
  laFila.appendChild( celdaDe( unTexto, 5 ) );
  return laFila;
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: unaMedida: Medida --> filaDeMedida() --> [ Text ]
// Qué hace: construye un <tr> con las 5 celdas de una Medida en el orden exacto de columnas.
// -------------------------------------------------------------------------------------------------
function filaDeMedida( unaMedida ) {
  var laFila = document.createElement( 'tr' );

  laFila.appendChild( celdaDe( unaMedida.id ) );
  laFila.appendChild( celdaDe( unaMedida.fecha ) );
  laFila.appendChild( celdaDe( unaMedida.idSensor ) );
  laFila.appendChild( celdaDe( unaMedida.tipoMedicion ) );
  laFila.appendChild( celdaDe( unaMedida.valorMedicion ) );

  return laFila;
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: unaMedida: Medida --> pintarLaUltimaMedida() -->
// Qué hace: renderiza la fila más reciente en la tabla de última medida registrada.
// -------------------------------------------------------------------------------------------------
function pintarLaUltimaMedida( unaMedida ) {
  var elCuerpo = document.getElementById( 'tbody-ultima-medida' );
  if ( elCuerpo === null ) {
    return;
  }

  elCuerpo.innerHTML = '';

  if ( ! unaMedida ) {
    elCuerpo.appendChild( filaDe( 'No hay ninguna medida registrada todavía' ) );
    return;
  }

  elCuerpo.appendChild( filaDeMedida( unaMedida ) );
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: lasMedidas: [ Medida ] --> pintarLasMedidasAnteriores() -->
// Qué hace: renderiza el historial de medidas anteriores en su tabla correspondiente.
// -------------------------------------------------------------------------------------------------
function pintarLasMedidasAnteriores( lasMedidas ) {
  var elCuerpo = document.getElementById( 'tbody-medidas-anteriores' );
  if ( elCuerpo === null ) {
    return;
  }

  elCuerpo.innerHTML = '';

  if ( ! lasMedidas || lasMedidas.length === 0 ) {
    elCuerpo.appendChild( filaDe( 'No hay medidas anteriores' ) );
    return;
  }

  for ( var i = 0 ; i < lasMedidas.length ; i++ ) {
    elCuerpo.appendChild( filaDeMedida( lasMedidas[i] ) );
  }
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: --> refrescarMedidas() -->
// Qué hace: solicita las medidas al proxy de cliente (obtenerMedidas) y repinta ambas tablas.
// -------------------------------------------------------------------------------------------------
function refrescarMedidas() {
  if ( typeof obtenerMedidas !== 'function' ) {
    avisarUnaVez( 'refrescarMedidas(): obtenerMedidas() no está disponible en el entorno' );
    return;
  }

  obtenerMedidas( function ( medidas ) {
    pintarLaUltimaMedida( medidas && medidas.length > 0 ? medidas[0] : null );
    pintarLasMedidasAnteriores( medidas && medidas.length > 1 ? medidas.slice( 1 ) : [] );
  } );
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: --> iniciarRefresco() -->
// Qué hace: inicia la actualización periódica de la interfaz cada MILISEGUNDOS_REFRESCO.
// -------------------------------------------------------------------------------------------------
function iniciarRefresco() {
  detenerRefresco();
  refrescarMedidas();
  idDelRefresco = setInterval( refrescarMedidas, MILISEGUNDOS_REFRESCO );
} // ()

// -------------------------------------------------------------------------------------------------
// DISEÑO: --> detenerRefresco() -->
// Qué hace: detiene el temporizador de refresco periódico si estuviera activo.
// -------------------------------------------------------------------------------------------------
function detenerRefresco() {
  if ( idDelRefresco !== null ) {
    clearInterval( idDelRefresco );
    idDelRefresco = null;
  }
} // ()

// -------------------------------------------------------------------------------------------------
// ARRANQUE AUTOMÁTICO DE LA INTERFAZ TRAS CARGAR EL DOM
// -------------------------------------------------------------------------------------------------
if ( typeof document !== 'undefined' ) {
  if ( document.readyState === 'loading' ) {
    document.addEventListener( 'DOMContentLoaded', function () {
      iniciarRefresco();
    } );
  } else {
    iniciarRefresco();
  }
}
