// ==================================================================================================
// DISEÑO LÓGICO: test automatizado de la interfaz gráfica web (web_gui/testUX.js)
// --------------------------------------------------------------------------------------------------
//   Verifica la estructura del DOM en index.html, cabeceras de tabla de 5 columnas,
//   y las funciones de renderizado de web_gui/gui.js (celdaDe, filaDe, filaDeMedida,
//   pintarLaUltimaMedida, pintarLasMedidasAnteriores).
// ==================================================================================================

function verificarUX() {
  console.log("======================================================================");
  console.log("  TEST AUTOMATICO DE LA INTERFAZ - web_gui/index.html & gui.js");
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

  // Si no hay entorno DOM (ej. ejecución fuera de navegador), avisar
  if ( typeof document === 'undefined' ) {
    console.warn("  [AVISO] No se detectó entorno DOM (document no definido).");
    return false;
  }

  // 1. Verificación de contenedores tbody
  var tbodyUltima = document.getElementById('tbody-ultima-medida');
  var tbodyHistorial = document.getElementById('tbody-medidas-anteriores');
  test( tbodyUltima !== null, "existe el elemento #tbody-ultima-medida en la página" );
  test( tbodyHistorial !== null, "existe el elemento #tbody-medidas-anteriores en la página" );

  // 2. Verificación de cabeceras de columnas (4 columnas canónicas)
  var thsUltima = document.querySelectorAll('.tabla-ultima-medida thead tr th');
  test( thsUltima.length === 4, "la tabla de última medida tiene exactamente 4 columnas" );

  var thsHistorial = document.querySelectorAll('.tabla-medidas-anteriores thead tr th');
  test( thsHistorial.length === 4, "la tabla de medidas anteriores tiene exactamente 4 columnas" );

  var nombresEsperados = ['Fecha', 'ID Sensor', 'Tipo', 'Valor'];
  var nombresCoinciden = true;
  for ( var i = 0; i < nombresEsperados.length; i++ ) {
    if ( !thsUltima[i] || thsUltima[i].textContent.trim() !== nombresEsperados[i] ) {
      nombresCoinciden = false;
    }
  }
  test( nombresCoinciden, "las cabeceras son exactamente Fecha, ID Sensor, Tipo, Valor" );

  // 3. Verificación de funciones de construcción de celdas y filas
  if ( typeof celdaDe === 'function' && typeof filaDeMedida === 'function' && typeof filaDe === 'function' ) {
    var cSimple = celdaDe("ValorPrueba");
    test( cSimple && cSimple.tagName === 'TD' && cSimple.textContent === "ValorPrueba", "celdaDe() construye un <td> con el texto correspondiente" );

    var cSpan = celdaDe("Mensaje", 4);
    test( cSpan && cSpan.colSpan === 4 && cSpan.className === 'sin-medidas', "celdaDe() aplica colSpan=4 y clase sin-medidas cuando procede" );

    var fAviso = filaDe("No hay datos");
    test( fAviso && fAviso.tagName === 'TR' && fAviso.children.length === 1 && fAviso.children[0].colSpan === 4, "filaDe() construye un <tr> con una sola celda que ocupa 4 columnas" );

    var medidaMock = {
      fecha: '2026-10-13 10:00:00',
      idSensor: 1,
      tipoMedicion: 'CO2',
      valorMedicion: 235
    };
    var fMedida = filaDeMedida(medidaMock);
    var celdasMedidaOk = fMedida && fMedida.children.length === 4
      && String(fMedida.children[0].textContent) === "2026-10-13 10:00:00"
      && String(fMedida.children[1].textContent) === "1"
      && String(fMedida.children[2].textContent) === "CO2"
      && String(fMedida.children[3].textContent) === "235";
    test( celdasMedidaOk, "filaDeMedida() construye un <tr> con las 4 celdas en el orden exacto del diseño" );
  } else {
    test( false, "las funciones visuales de gui.js están disponibles en el entorno" );
  }

  // 4. Verificación de funciones de renderizado en DOM
  if ( typeof pintarLaUltimaMedida === 'function' && typeof pintarLasMedidasAnteriores === 'function' ) {
    pintarLaUltimaMedida( null );
    var textoUltimaVacia = (tbodyUltima.textContent || tbodyUltima.innerHTML || '');
    test( textoUltimaVacia.indexOf("No hay ninguna medida") !== -1, "pintarLaUltimaMedida(null) muestra aviso de no hay medidas" );

    pintarLaUltimaMedida( { fecha: '2026-10-13 10:00:00', idSensor: 1, tipoMedicion: 'CO2', valorMedicion: 235 } );
    test( tbodyUltima.children.length === 1 && tbodyUltima.children[0].children.length === 4, "pintarLaUltimaMedida() renderiza la medida con sus 4 celdas" );

    pintarLasMedidasAnteriores( [] );
    var textoHistorialVacio = (tbodyHistorial.textContent || tbodyHistorial.innerHTML || '');
    test( textoHistorialVacio.indexOf("No hay medidas anteriores") !== -1, "pintarLasMedidasAnteriores([]) muestra aviso de historial vacío" );
  }

  console.log("----------------------------------------------------------------------");
  console.log("  Pruebas: " + comprobaciones + "   Fallos: " + fallos);
  console.log("======================================================================");
  if ( fallos === 0 ) {
    console.log("  TEST INTERFAZ OK");
    console.log("======================================================================\n");
    return true;
  } else {
    console.error("  TEST INTERFAZ FALLO (" + fallos + " fallos)");
    console.log("======================================================================\n");
    return false;
  }
}

// Ejecución automática al cargar el documento
if ( typeof document !== 'undefined' ) {
  if ( document.readyState === 'loading' ) {
    document.addEventListener( 'DOMContentLoaded', verificarUX );
  } else {
    verificarUX();
  }
}
