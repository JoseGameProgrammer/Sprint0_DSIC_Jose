// ==================================================================================================
// DISEÑO LÓGICO: testUX.js (test automático de la interfaz del navegador)
// --------------------------------------------------------------------------------------------------
//
//                    --------------------- testUX.js ---------------------
//                    |
//                    | Medida = ( id: N, uuid: Text, major: N, minor: Z,
//                    |             txPower: Z, nombreEmisora: Text,
//                    |             fechaLectura: Text )
//                    |
//                    |  --> comprobarLosDosCuerpos() --> B
//                    |
//                    |  --> comprobarLasCincoCabeceras() --> B
//                    |
//                    |  --> verificarUX() --> B
//                    |
//                    ----------------------------------------------------
//
// QUÉ HACE ESTE FICHERO
//
// Comprueba, sin JUnit ni librerías externas, que la interfaz de web/ux/index.html
// tiene todo lo que necesita para funcionar:
//
//   1. Que existen los contenedores #tbody-ultima-medida y #tbody-medidas-anteriores.
//   2. Que las dos tablas cuentan exactamente con las 5 cabeceras <th>:
//      "ID", "Medición", "Uuid", "Nombre Emisora" y "Fecha y Hora".
//   3. Que imprime por consola "TEST UX NAVEGADOR OK" si todo está, o
//      "TEST UX NAVEGADOR ERROR" si falta algo.
//
// CÓMO SE EJECUTA
//
// Opción A, la fácil: abrir web/ux/index.html en el navegador. El script se ejecuta solo
//               al cargar la página y escribe el resultado en la consola.
//
// Opción B, a mano: en la consola del navegador, con la página abierta, escribir
//                     verificarUX()
//               y se vuelve a comprobar todo y a imprimir el veredicto.
//
// CÓMO SE ABRE LA CONSOLA
//
//   Chrome / Edge:   pulsar F12, y elegir la pestaña "Console" o "Consola".
//   Firefox:         F12, pestaña "Consola".
//
// OJO: si abres el fichero con doble clic, el archivo se sirve como "file://".
// En ese caso el navegador puede que no quiera cargarlo por las reglas de seguridad, y
// la consola se quedará en blanco con un aviso. Si es el caso, la página funciona igual
// pero el test no se ve. La solución es levantar un servidor de verdad, que además es
// como se acabará usando la página:
//
//     cd web
//     php -S localhost:8000
//     y abrir  http://localhost:8000/ux/index.html
// ==================================================================================================

// -------------------------------------------------------------------------------------------------
// Un poco de ayuda para imprimir por consola.
//
// OJO con esto: console.log() no existe en los navegadores muy antiguos (Internet Explorer
// 8 y menos), y en el móvil a veces no se ve nada. Estos envoltorios miran primero si
// console existe, y si no, escriben con document.write() o con alert(), que son las formas
// de siempre. Así el mismo test sirve en cualquier sitio.
// -------------------------------------------------------------------------------------------------
function escribir( elTexto ) {

  if ( window.console && typeof window.console.log === 'function' ) {
    window.console.log( elTexto );
    return;
  } // ()

  if ( document.body ) {
    document.body.appendChild( document.createTextNode( elTexto + '\n' ) );
    return;
  } // ()

  alert( elTexto );

} // ()

function separador() {
  escribir( '' );
  escribir( '======================================================================' );
}

// -------------------------------------------------------------------------------------------------
// --> comprobarLosDosCuerpos() --> B
// Qué hace: comprueba que existan los dos <tbody> que la lógica del navegador necesita para
//           rellenar las tablas.
//
// OJO con el "=== null": getElementById() devuelve null cuando no encuentra nada, y
// "if (elElemento)" bastaría. Se escribe la comparación entera para que se vea
// exactamente qué se está mirando.
//
// OJO también con el "return false" en cuanto falla uno: si el primer <tbody> no existe,
// buscar dentro de él daría un error, así que se corta y se sigue con el segundo.
// -------------------------------------------------------------------------------------------------
function comprobarLosDosCuerpos() {

  separador();
  escribir( '  TEST AUTOMATICO DE LA INTERFAZ - web/ux/index.html' );
  separador();
  escribir( '' );
  escribir( '--> 1. Los dos contenedores <tbody>' );

  var todoBien = true;

  var elIdUltima = document.getElementById( 'tbody-ultima-medida' );
  var elIdAnteriores = document.getElementById( 'tbody-medidas-anteriores' );

  if ( elIdUltima === null ) {
    escribir( '    FALLO no existe el elemento #tbody-ultima-medida' );
    todoBien = false;
  } else {
    escribir( '    OK    existe #tbody-ultima-medida' );
  } // ()

  if ( elIdAnteriores === null ) {
    escribir( '    FALLO no existe el elemento #tbody-medidas-anteriores' );
    todoBien = false;
  } else {
    escribir( '    OK    existe #tbody-medidas-anteriores' );
  } // ()

  return todoBien;

} // ()

// -------------------------------------------------------------------------------------------------
// --> comprobarLasCincoCabeceras() --> B
// Qué hace: comprueba que las DOS tablas tengan exactamente 5 celdas <th> en su <thead>, y que
//           se llamen "ID", "Medición", "Uuid", "Nombre Emisora" y "Fecha y Hora".
//
// Se cuentan SÓLO los <th> de la cabecera de la tabla. OJO con esto, que es el error típico:
// un <th> de una fila de datos (una celda de encabezado vertical) también cuenta como <th>
// y haría que la cuenta no cuadrara. Por eso se mira solo el thead, que es la única parte
// de la tabla donde los <th> son las etiquetas de las columnas.
// -------------------------------------------------------------------------------------------------
function comprobarLasCincoCabeceras() {

  escribir( '' );
  escribir( '--> 2. Las 5 cabeceras de cada tabla' );

  // Las 5 cabeceras exactas que pide el enunciado, en este orden.
  var lasCabecerasPedidas = [ 'ID', 'Medición', 'Uuid', 'Nombre Emisora', 'Fecha y Hora' ];

  // Las dos tablas, con el id de su <tbody>, para poder distinguirlas en el informe.
  var lasTablas = [
    { nombre: 'última medida', cuerpo: document.getElementById( 'tbody-ultima-medida' ) },
    { nombre: 'medidas anteriores', cuerpo: document.getElementById( 'tbody-medidas-anteriores' ) }
  ];

  var todoBien = true;

  for ( var i = 0 ; i < lasTablas.length ; i++ ) {

    var laTabla = lasTablas[i];
    var elCuerpo = laTabla.cuerpo;

    // Si el <tbody> no existe, no hay tabla que mirar. Ya se ha avisado antes.
    if ( elCuerpo === null ) {
      escribir( '    FALLO la tabla de ' + laTabla.nombre + ' no existe, no se pueden mirar sus cabeceras' );
      todoBien = false;
      continue;
    } // ()

    // De <tbody> se sube a <table> con .parentNode, y de ahí se busca el <thead> para contar
    // SOLO las cabeceras de columna.
    var laTablaCompleta = elCuerpo.parentNode;
    var elThead = laTablaCompleta.querySelector( 'thead' );

    if ( elThead === null ) {
      escribir( '    FALLO la tabla de ' + laTabla.nombre + ' no tiene <thead>, así que no tiene cabeceras' );
      todoBien = false;
      continue;
    } // ()

    // querySelectorAll devuelve una lista viva de elementos, y .length dice cuántos hay.
    var losTh = elThead.querySelectorAll( 'th' );

    // --- la cuenta tiene que ser exactamente 5 -------------------------------------------
    // Ni 4 (falta una columna) ni 6 (sobra una). El "=== 5" no un "<= 5" a propósito.
    if ( losTh.length === 5 ) {
      escribir( '    OK    la tabla de ' + laTabla.nombre + ' tiene 5 cabeceras <th>' );
    } else {
      escribir( '    FALLO la tabla de ' + laTabla.nombre + ' tiene ' + losTh.length
                + ' cabeceras <th>, y deben ser exactamente 5' );
      todoBien = false;
      continue;
    } // ()

    // --- y además, tienen que SER las pedidas, en el orden correcto ----------------------
    var losTextos = [];
    var todasLasCabecerasEstan = true;

    for ( var j = 0 ; j < losTh.length ; j++ ) {

      // OJO: el texto del <th> puede venir con espacios de más ("  ID  ") por cómo se
      // escriban en el HTML. Con textContent y quitando los espacios de los dos lados, la
      // comparación es exacta y no hay sorpresas por la maquetación.
      var elTexto = losTh[j].textContent.replace( /^\s+|\s+$/g, '' );

      losTextos.push( elTexto );

      if ( elTexto !== lasCabecerasPedidas[j] ) {
        escribir( '    FALLO la tabla de ' + laTabla.nombre + ' tiene "' + elTexto
                  + '" en la columna ' + ( j + 1 ) + ' y debería tener "' + lasCabecerasPedidas[j] + '"' );
        todasLasCabecerasEstan = false;
      } // ()

    } // ()

    if ( todasLasCabecerasEstan ) {
      escribir( '    OK    las cabeceras de ' + laTabla.nombre + ' son: ' + losTextos.join( ', ' ) );
    } // ()

    if ( ! todasLasCabecerasEstan ) {
      todoBien = false;
    } // ()

  } // ()

  return todoBien;

} // ()

// -------------------------------------------------------------------------------------------------
// --> verificarUX() --> B
// Qué hace: el hilo conductor. Llama a las comprobaciones, imprime el veredicto y devuelve
//           true si todo fue bien y false si algo faltó.
//
// Que devuelva un booleano, además de imprimir, permite encadenar comprobaciones en la consola
// sin tener que mirar el texto: por ejemplo,  verificarUX() && alert("todo en orden")
// -------------------------------------------------------------------------------------------------
function verificarUX() {

  var elResultadoDelCuerpos = comprobarLosDosCuerpos();
  var elResultadoDeCabeceras = comprobarLasCincoCabeceras();

  var todosLosResultados = elResultadoDelCuerpos && elResultadoDeCabeceras;

  var losFallos = ( elResultadoDelCuerpos ? 0 : 1 ) + ( elResultadoDeCabeceras ? 0 : 1 );

  escribir( '' );
  escribir( '--> Veredicto' );
  escribir( '    Comprobaciones: 2   Fallos: ' + losFallos );
  separador();
  escribir( '  ' + ( todosLosResultados ? 'TEST UX NAVEGADOR OK' : 'TEST UX NAVEGADOR ERROR' ) );
  separador();

  return todosLosResultados;

} // ()

// -------------------------------------------------------------------------------------------------
// Se ejecuta el test solo, al cargar la página.
//
// OJO con el addEventListener: el <script> está al final del <body>, así que cuando se ejecuta
// los <tbody> YA están en el documento y se podría comprobar directamente. Pero si alguien
// mueve el <script> a la cabecera, el document ya no tendría el <tbody> todavía y el test
// fallaría sin motivo. Con DOMContentLoaded se espera a que la página esté construida enteras,
// que es más robusto y da igual dónde esté el script.
// -------------------------------------------------------------------------------------------------
if ( document.readyState === 'loading' ) {

  // La página aún se está construyendo: se espera a que esté lista.
  document.addEventListener( 'DOMContentLoaded', function () {
    verificarUX();
  } );

} else {

  // La página ya está lista (el script estaba al final del body, o se ha llamado a mano).
  verificarUX();

} // ()
