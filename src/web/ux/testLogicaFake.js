// ==================================================================================================
// DISEÑO LÓGICO: testLogicaFake.js (test automático de la lógica fake del navegador)
// --------------------------------------------------------------------------------------------------
//                       --------------- testLogicaFake.js ---------------
//                       |
//                       |  --> ponerMedidasFalsas() --> B
//                       |
//                       |  --> comprobarObtenerMedidasFake() --> B
//                       |
//                       |  --> comprobarRefrescarMedidas() --> B
//                       |
//                       |  --> comprobarElEstadoDevuelto() --> B
//                       |
//                       |  --> devolverElFalso() -->
//                       |
//                       |  --> verificarLogicaFake() --> B
//                       |
//                       ----------------------------------------------------
//
// QUÉ HACE ESTE SCRIPT
//
// Comprueba, sin JUnit ni librerías externas, que la lógica de web/ux/LogicaFake.js
// funciona:
//
//   1. Falsea el XMLHttpRequest para que devuelva 3 Medida de prueba (ids 5, 4 y 3), y
//      comprueba con un "if" que obtenerMedidasFake() se las pasa al callback.
//   2. Ejecuta refrescarMedidas() y comprueba que la tabla "Última medida registrada"
//      tiene 1 fila con el ID 5, y que la de "Medidas anteriores" tiene 2 filas con los
//      IDs 4 y 3.
//   3. Imprime por consola "TEST LOGICA FAKE NAVEGADOR OK" o
//      "TEST LOGICA FAKE NAVEGADOR ERROR".
//
// POR QUÉ HAY QUE FALSEAR EL XMLHttpRequest Y NO CALL REAL
// ------------------------------------------------------
// Porque este test NO puede depender del servidor. Si el test llamara al PHP de verdad,
// el resultado dependería de que el servidor esté levantado, de que haya medidas
// guardadas, y de cuántas haya. Un test que depende de esas tres cosas no es un test:
// es una gamble, y un día da verde y al siguiente da rojo sin que haya cambiado nada.
//
// Falseando el XMLHttpRequest, el test es SIEMPRE igual: siempre 3 medidas, siempre los
// mismos ids, pase lo que pase con el servidor. Eso es lo que se busca.
//
// CÓMO SE EJECUTA
//
// Se ejecuta solo al abrir web/ux/index.html, y también se puede llamar a mano desde la
// consola del navegador escribiendo:   verificarLogicaFake()
//
// CÓMO SE CONSIGUE QUE EL FALSEO NO ROMPA LOS REFRESCOS DE VERDAD
// --------------------------------------------------------------
// LogicaFake.js tiene un refresco automático cada 3 segundos. Si se le cambia el
// XMLHttpRequest mientras sigue en marcha, cada 3 segundos llegaría una respuesta falsa y
// machacaría la tabla, y el test acabaría mirando datos que él no ha puesto.
//
// Por eso el test hace tres cosas, en este orden y en este orden exacto:
//
//     1. detenerRefresco()      -> para el temporizador de 3 segundos
//     2. falsea el XMLHttpRequest y hace sus comprobaciones
//     3. devuelve el XMLHttpRequest de verdad y iniciaRefresco()
//
// Así, al terminar, la página se queda como estaba, con sus refrescos de verdad.
//
// OJO: el falseo responde EN EL MOMENTO de la llamada a send(), sin esperar nada. El
// XMLHttpRequest de verdad es asíncrono (contesta más tarde), y el falseo también
// debería serlo, para que el test lo probara de verdad. Pero si fuese asíncrono, el test
// tendría que esperar de alguna manera a que acabara, y desde la consola eso significa
// temporizadores y promesas, con un "y ya" que dejaría el veredicto para un rato después
// de pedirlo. Con el falseo en síncrono, todo acaba antes de devolver el control, y el
// veredicto se imprime ya. Que conste: es una decisión del TEST, no de la lógica.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

// -------------------------------------------------------------------------------------------------
// Un poco de ayuda para imprimir por consola, como en el test de la interfaz.
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
// Las 3 Medida de prueba. Se declaran AQUÍ, arriba del todo, y no dentro de cada
// comprobación, porque las usan varias comprobaciones y además este test automático
// sirve de ejemplo de Medida para quien lo lea.
//
// OJO con el tipo de cada campo, que es el del diseño oficial:
//
//     Medida = ( id: N, uuid: Text, major: N, minor: Z,
//                txPower: Z, nombreEmisora: Text, fechaLectura: Text )
//               N = natural sin signo, Z = entero con signo
//
// minor y txPower son Z (con signo) y por eso el -53 de abajo es legal. Si minor fuera
// N, ese -53 no tendría sentido.
// -------------------------------------------------------------------------------------------------
var LAS_MEDIDAS_DE_PRUEBA = [
  { id: 5, uuid: 'EPSG-GTI-PROY-3A', major: 2817, minor: 235, txPower: -53, nombreEmisora: 'GTI-Jose', fechaLectura: '2026-09-28 20:45:08' },
  { id: 4, uuid: 'EPSG-GTI-PROY-3A', major: 2817, minor: 234, txPower: -53, nombreEmisora: 'GTI-Jose', fechaLectura: '2026-09-28 20:45:08' },
  { id: 3, uuid: 'EPSG-GTI-PROY-3A', major: 2817, minor: 233, txPower: -53, nombreEmisora: 'GTI-Jose', fechaLectura: '2026-09-28 19:45:08' }
];

// -------------------------------------------------------------------------------------------------
// --> devolverElFalso() -->
// -------------------------------------------------------------------------------------------------
//   --> devolverElFalso() -->
//
// Qué hace: devuelve el XMLHttpRequest de verdad, para dejar la página como estaba.
//
// OJO: se guarda el XHR original ANTES de cambiarlo, en la variable "elXhrOriginal". Si
// en vez de guardarlo se hiciera "window.XMLHttpRequest = window.XMLHttpRequest" esperando
// que se restaurara solo, no se restauraría nada: la variable ya contiene el falso.
// -------------------------------------------------------------------------------------------------
var elXhrOriginal = null;

function devolverElFalso() {

  if ( elXhrOriginal !== null ) {
    window.XMLHttpRequest = elXhrOriginal;
    elXhrOriginal = null;
  } // ()

} // ()

// -------------------------------------------------------------------------------------------------
// --> ponerMedidasFalsas() --> B
// -------------------------------------------------------------------------------------------------
//   lasMedidas: [ Medida ] --> ponerMedidasFalsas() --> B
//
// Qué hace: cambia el XMLHttpRequest de la página por uno falso, que cuando alguien le
//           pida algo devuelve las medidas que se le pasen, sin salir a la red. Devuelve
//           true si se ha podido.
//
// CÓMO ESTÁ HECHO EL XHR FALSO
// -----------------------------
// Tiene que tener LO MISMO que el de verdad, porque obtenerMedidasFake() lo usa:
//
//     laPeticion.open(metodo, url, asincrono)   -> no hace nada, se apunta
//     laPeticion.onload = funcion                -> se guarda, para poder llamarla
//     laPeticion.onerror = funcion               -> se guarda, por si acaso
//     laPeticion.send()                          -> contesta al instante
//     laPeticion.status                          -> 200, para que pase la comprobación
//     laPeticion.responseText                    -> el JSON, que es lo que se leería
//
// OJO con el status: si se dejara en 0, obtenerMedidasFake() vería que no es un código
// de éxito y devolvería una lista vacía, y el test daría error por un motivo que no es
// el que se quiere comprobar.
// -------------------------------------------------------------------------------------------------
function ponerMedidasFalsas( lasMedidas ) {

  // Se guarda el XHR de verdad, la primera vez. Si ya hay uno guardado, no se vuelve a
  // guardar: si no, se guardaría el falso como si fuera el bueno, y devolverElFalso()
  // dejaría un falso puesto para siempre.
  if ( elXhrOriginal === null ) {
    elXhrOriginal = window.XMLHttpRequest;
  } // ()

  // El JSON que se va a devolver. Se construye con JSON.stringify(), que es el contrario
  // de JSON.parse(): convierte un objeto de JavaScript en el texto JSON. Se hace así y no
  // escribiendo el texto a mano, para que el falseo no pueda desincronizarse de los datos
  // si alguien cambia LAS_MEDIDAS_DE_PRUEBA.
  var elJson = JSON.stringify( { medidas: lasMedidas } );

  // El XMLHttpRequest falso. Es una función (una clase, vamos) a la que se le llama con
  // "new", y por eso tiene que devolver explícitamente un objeto con la palabra clave
  // "this": si no, al hacer "new" saldría un objeto vacío y no serviría de nada.
  window.XMLHttpRequest = function () {

    this.status = 200;
    this.responseText = '';
    this.onload = null;
    this.onerror = null;

    this.open = function ( metodo, url, asincrono ) {
      this.url = url;
    };

    this.send = function () {

      // Aquí es donde el XHR de verdad mandaría la petición por la red. El falso solo
      // contesta, y lo hace callingando a onload, que es lo que haría el de verdad al
      // recibir la respuesta.
      this.responseText = elJson;

      if ( this.onload ) {
        this.onload();
      } // ()

    }; // ()

    this.setRequestHeader = function () {
      // No hace nada: el XHR de verdad sí lo usa, pero esta lógica solo hace GET sin
      // cabeceras. Se deja el hueco para que, si algún día se le pusiera, no reventara.
    };

    return this;

  };

  return true;

} // ()

// -------------------------------------------------------------------------------------------------
// --> comprobarObtenerMedidasFake() --> B
// -------------------------------------------------------------------------------------------------
//   --> comprobarObtenerMedidasFake() --> B
//
// Qué hace: la comprobación 1 del enunciado. Llama a obtenerMedidasFake() con un callback
//           y mira lo que recibe: tienen que ser las 3 medidas, en el mismo orden.
//
// OJO CON EL ORDEN DE LAS COMPROBACIONES, que es la parte fina de esta comprobación:
//
//   - Primero se comprueba que el callback se ha llamado. Si no se llamó, las medidas
//     seguirían sin llegar y no se podría seguir comparando nada.
//   - Después se comparan los elementos uno a uno, con el "id" que es el que las
//     distingue (5, 4 y 3).
//   - Y por último se mira que el array sea de largo 3, que es lo que demuestra que NO
//     hay uno de más. Si solo se comprobaran los 3 primeros, entraría uno cuarto sin que
//     el test se enterase.
// -------------------------------------------------------------------------------------------------
function comprobarObtenerMedidasFake() {

  separador();
  escribir( '  TEST AUTOMATICO DE LA LOGICA FAKE - web/ux/LogicaFake.js' );
  separador();
  escribir( '' );
  escribir( '--> 1. obtenerMedidasFake( cb ) entrega las 3 Medida al callback' );

  // El callback guarda lo que recibe en esta variable, para poder mirarlo después.
  var loQueHaLlegadoAlCallback = null;
  var elCallbackSeHaLlamado = false;

  ponerMedidasFalsas( LAS_MEDIDAS_DE_PRUEBA );

  // ESTA es la llamada que se está probando, con su callback.
  obtenerMedidasFake( function ( medidas ) {
    loQueHaLlegadoAlCallback = medidas;
    elCallbackSeHaLlamado = true;
  } );

  var todoBien = true;

  // --- el callback tiene que haberse llamado --------------------------------------
  if ( elCallbackSeHaLlamado === false ) {
    escribir( '    FALLO el callback no se ha llamado: obtenerMedidasFake() no ha pasado ninguna medida' );
    return false; // sin datos no se puede seguir
  } // ()

  escribir( '    OK    el callback se ha llamado' );

  // --- tiene que haber llegado una lista -----------------------------------------
  if ( ! loQueHaLlegadoAlCallback || loQueHaLlegadoAlCallback.length === 0 ) {
    escribir( '    FALLO el callback ha recibido una lista vacía' );
    return false;
  } // ()

  escribir( '    OK    el callback ha recibido una lista con ' + loQueHaLlegadoAlCallback.length + ' medidas' );

  // --- los 3 ids, en el mismo orden ----------------------------------------------
  // Se comparan uno a uno, porque lo que importa no es solo que estén, sino que lleguen
  // en el mismo orden: el backend los manda del más reciente al más antiguo, y la tabla
  // de la última medida depende de que medidas[0] sea el más nuevo.
  var losIdsEsperados = [ 5, 4, 3 ];
  var losIdsQueHanLlegado = [];

  for ( var i = 0 ; i < loQueHaLlegadoAlCallback.length ; i++ ) {
    losIdsQueHanLlegado.push( loQueHaLlegadoAlCallback[i].id );
  } // ()

  if ( loQueHaLlegadoAlCallback.length === losIdsEsperados.length ) {
    escribir( '    OK    la lista tiene los 3 elementos: ids ' + losIdsQueHanLlegado.join( ', ' ) );
  } else {
    escribir( '    FALLO la lista tiene ' + loQueHaLlegadoAlCallback.length
              + ' elementos y deben ser 3: ids ' + losIdsQueHanLlegado.join( ', ' ) );
    todoBien = false;
  } // ()

  for ( var j = 0 ; j < losIdsEsperados.length ; j++ ) {

    if ( loQueHaLlegadoAlCallback[j] && loQueHaLlegadoAlCallback[j].id === losIdsEsperados[j] ) {
      escribir( '    OK    la posición ' + j + ' es la Medida de id ' + losIdsEsperados[j] );
    } else {

      // OJO al detalle de qué se imprime: se mira que el elemento exista antes de leer su
      // id. Si loQueHaLlegadoAlCallback[j] fuera undefined, el .id reventaría el test
      // entero con un error en vez de dar un FALLO.
      escribir( '    FALLO en la posición ' + j + ' debería estar la Medida de id '
                + losIdsEsperados[j] + ' y hay: '
                + ( loQueHaLlegadoAlCallback[j]
                    ? 'la de id ' + loQueHaLlegadoAlCallback[j].id
                    : 'nada' ) );
      todoBien = false;

    } // ()

  } // ()

  return todoBien;

} // ()

// -------------------------------------------------------------------------------------------------
// --> comprobarRefrescarMedidas() --> B
// -------------------------------------------------------------------------------------------------
//   --> comprobarRefrescarMedidas() --> B
//
// Qué hace: la comprobación 2 del enunciado. Llama a refrescarMedidas() y mira lo que ha
//           quedado pintado en las dos tablas:
//
//     "Última medida registrada"  -> 1 fila, y su ID es el 5
//     "Medidas anteriores"        -> 2 filas, con los IDs 4 y 3
//
// OJO: esto comprueba el CORTE de la lista, que es lo importante. Si el código usara
// slice(0) en vez de slice(1), la medida de id 5 saldría en las DOS tablas, y este test
// lo detectaría porque en la segunda tabla no habría 2 filas sino 3.
//
// CÓMO SE CUENTAN LAS FILAS
// -------------------------
// Con .querySelectorAll('tr'), que devuelve una lista de todos los <tr> que hay dentro.
// No se cuentan a mano con los hijos, porque childNodes incluye los nodos de texto
// (los espacios y los saltos de línea del HTML), que NO son filas, y contarlos así daría
// un número que no cuadra.
//
// OJO: antes de contar se VACÍA la tabla, porque en la página puede haber lo que hubiera
// dejado el refresco de verdad (o las filas de ejemplo del HTML). Si no se vaciara, el
// test miraría un número de filas que no depende de lo que acaba de pintar el código, y
// podría dar verde por casualidad.
// -------------------------------------------------------------------------------------------------
function comprobarRefrescarMedidas() {

  escribir( '' );
  escribir( '--> 2. refrescarMedidas() pinta bien las dos tablas' );

  var todoBien = true;

  // --- la tabla de la última medida ---------------------------------------------
  var laUltima = document.getElementById( 'tbody-ultima-medida' );

  if ( laUltima === null ) {
    escribir( '    FALLO no existe #tbody-ultima-medida' );
    return false;
  } // ()

  laUltima.innerHTML = '';
  refrescarMedidas();

  var lasFilasUltima = laUltima.querySelectorAll( 'tr' );

  if ( lasFilasUltima.length === 1 ) {
    escribir( '    OK    "Última medida registrada" tiene 1 fila' );
  } else {
    escribir( '    FALLO "Última medida registrada" tiene ' + lasFilasUltima.length
              + ' filas y debe tener solo 1' );
    todoBien = false;
  } // ()

  if ( lasFilasUltima.length >= 1 ) {
    // la primera celda de la fila es el ID
    var elIdUltima = lasFilasUltima[0].querySelectorAll( 'td' )[0];

    if ( elIdUltima && elIdUltima.textContent === '5' ) {
      escribir( '    OK    y su ID es el 5, el más reciente' );
    } else {
      escribir( '    FALLO el ID de la última medida debería ser 5 y es "'
                + ( elIdUltima ? elIdUltima.textContent : 'nada' ) + '"' );
      todoBien = false;
    } // ()

    // las 5 celdas, que son las 5 columnas del diseño
    var lasCeldasUltima = lasFilasUltima[0].querySelectorAll( 'td' );

    if ( lasCeldasUltima.length === 5 ) {
      escribir( '    OK    la fila tiene las 5 celdas: id=' + lasCeldasUltima[0].textContent
                + ', Medición=' + lasCeldasUltima[1].textContent
                + ', Uuid=' + lasCeldasUltima[2].textContent
                + ', Nombre Emisora=' + lasCeldasUltima[3].textContent
                + ', Fecha y Hora=' + lasCeldasUltima[4].textContent );
    } else {
      escribir( '    FALLO la fila de la última medida tiene ' + lasCeldasUltima.length
                + ' celdas y debe tener 5' );
      todoBien = false;
    } // ()

  } // ()

  // --- la tabla de medidas anteriores -------------------------------------------
  var lasAnteriores = document.getElementById( 'tbody-medidas-anteriores' );

  if ( lasAnteriores === null ) {
    escribir( '    FALLO no existe #tbody-medidas-anteriores' );
    return false;
  } // ()

  lasAnteriores.innerHTML = '';
  refrescarMedidas();

  var lasFilasAnteriores = lasAnteriores.querySelectorAll( 'tr' );

  if ( lasFilasAnteriores.length === 2 ) {
    escribir( '    OK    "Medidas anteriores" tiene 2 filas' );
  } else {
    escribir( '    FALLO "Medidas anteriores" tiene ' + lasFilasAnteriores.length
              + ' filas y deben ser 2 (las 3 medidas menos la última)' );
    todoBien = false;
  } // ()

  // los ids, en orden: 4 y luego 3
  var losIdsAnteriores = [];

  for ( var k = 0 ; k < lasFilasAnteriores.length ; k++ ) {
    var laCelda = lasFilasAnteriores[k].querySelectorAll( 'td' )[0];
    losIdsAnteriores.push( laCelda ? laCelda.textContent : '?' );
  } // ()

  if ( losIdsAnteriores.join( ',' ) === '4,3' ) {
    escribir( '    OK    y sus IDs son 4 y 3, en ese orden' );
  } else {
    escribir( '    FALLO los IDs de "Medidas anteriores" deberían ser 4,3 y son '
              + losIdsAnteriores.join( ',' ) );
    todoBien = false;
  } // ()

  return todoBien;

} // ()

// -------------------------------------------------------------------------------------------------
// --> comprobarElEstadoDevuelto() --> B
// -------------------------------------------------------------------------------------------------
//   --> comprobarElEstadoDevuelto() --> B
//
// Qué hace: la comprobación 3 del enunciado, más un par de casos raros que son los que
//           más se olvidan.
//
// Se prueba qué pasa cuando el servidor NO tiene medidas, y cuando la petición falla. En
// los dos casos la lista tiene que quedar vacía y la tabla lo tiene que decir, en vez
// de quedarse con los datos viejos, que ya no serían ciertos.
//
// OJO: es importante vaciar las dos tablas ANTES de cada llamada a refrescarMedidas(),
// porque si no, y la función no pintara nada, se verían las filas de la comprobación
// anterior y el test daría un verde falso.
// -------------------------------------------------------------------------------------------------
function comprobarElEstadoDevuelto() {

  escribir( '' );
  escribir( '--> 3. Qué pasa cuando no hay medidas o cuando el servidor falla' );

  var todoBien = true;

  var laUltima = document.getElementById( 'tbody-ultima-medida' );
  var lasAnteriores = document.getElementById( 'tbody-medidas-anteriores' );

  // --- caso A: el servidor contesta, pero no hay ninguna medida --------------------
  escribir( '    caso A: el servidor contesta con una lista vacía' );

  ponerMedidasFalsas( [] );

  laUltima.innerHTML = '';
  lasAnteriores.innerHTML = '';
  refrescarMedidas();

  var filasUltimaVacia = laUltima.querySelectorAll( 'tr' ).length;
  var filasAnterioresVacia = lasAnteriores.querySelectorAll( 'tr' ).length;

  // Con 0 medidas, cada tabla tiene 1 fila: la que lo dice. NINGUNA tiene que quedarse
  // con 0 filas, porque eso dejaría la tabla en blanco sin explicar nada.
  if ( filasUltimaVacia === 1 && filasAnterioresVacia === 1 ) {
    escribir( '    OK    las dos tablas avisan de que no hay medidas, en vez de quedarse en blanco' );
  } else {
    escribir( '    FALLO con la lista vacía, las filas deberían ser 1 y 1 y son '
              + filasUltimaVacia + ' y ' + filasAnterioresVacia );
    todoBien = false;
  } // ()

  // --- caso B: el servidor está apagado -------------------------------------------
  // Aquí el XHR falso se cambia por uno que llama a onerror, que es lo que hace el de
  // verdad cuando no hay nadie escuchando. La lista tiene que quedar vacía igual.
  escribir( '    caso B: el servidor no contesta (error de red)' );

  window.XMLHttpRequest = function () {
    this.open = function () {};
    this.send = function () { if ( this.onerror ) { this.onerror(); } };
    this.setRequestHeader = function () {};
    return this;
  };

  laUltima.innerHTML = '';
  lasAnteriores.innerHTML = '';
  refrescarMedidas();

  var filasUltimaError = laUltima.querySelectorAll( 'tr' ).length;
  var filasAnterioresError = lasAnteriores.querySelectorAll( 'tr' ).length;

  if ( filasUltimaError === 1 && filasAnterioresError === 1 ) {
    escribir( '    OK    con el servidor caído las tablas avisan también, y no se quedan con datos viejos' );
  } else {
    escribir( '    FALLO con el servidor caído, las filas deberían ser 1 y 1 y son '
              + filasUltimaError + ' y ' + filasAnterioresError );
    todoBien = false;
  } // ()

  return todoBien;

} // ()

// -------------------------------------------------------------------------------------------------
// --> verificarLogicaFake() --> B
// -------------------------------------------------------------------------------------------------
//   --> verificarLogicaFake() --> B
//
// Qué hace: el hilo conductor. Para el refresco automático, falsea el XMLHttpRequest,
// hace las comprobaciones, lo devuelve de verdad, reanuda el refresco e imprime el
// veredicto.
//
// QUE DEVUELVA UN BOLEANO, IGUAL QUE verificarUX(), para poder encadenar ambos tests
// desde la consola:
//
//     verificarUX() && verificarLogicaFake() && alert("todo en orden")
//
// -------------------------------------------------------------------------------------------------
function verificarLogicaFake() {

  // (1) Se para el refresco automático, o las respuestas falsas llegarían solas cada 3
  //     segundos y se llevarían por delante lo que este test acaba de comprobar.
  detenerRefresco();

  var elResultadoDeLasMedidas = comprobarObtenerMedidasFake();
  var elResultadoDelRefresco  = comprobarRefrescarMedidas();
  var elResultadoDelEstado    = comprobarElEstadoDevuelto();

  // (2) Se devuelve el XMLHttpRequest de verdad y se reanuda el refresco automático,
  //     para dejar la página exactamente como estaba antes de empezar.
  devolverElFalso();
  iniciarRefresco();

  var todosLosResultados = elResultadoDeLasMedidas && elResultadoDelRefresco && elResultadoDelEstado;

  var losFallos = ( elResultadoDeLasMedidas ? 0 : 1 )
               + ( elResultadoDelRefresco ? 0 : 1 )
               + ( elResultadoDelEstado ? 0 : 1 );

  escribir( '' );
  escribir( '--> Veredicto' );
  escribir( '    Comprobaciones: 3   Fallos: ' + losFallos );
  separador();
  escribir( '  ' + ( todosLosResultados ? 'TEST LOGICA FAKE NAVEGADOR OK' : 'TEST LOGICA FAKE NAVEGADOR ERROR' ) );
  separador();

  return todosLosResultados;

} // ()

// -------------------------------------------------------------------------------------------------
// Se ejecuta el test solo, al cargar la página.
//
// OJO con el orden: este script tiene que cargarse DESPUÉS de LogicaFake.js, porque
// llama a detenerRefresco(), que está definido ahí. Por eso en el index.html el <script>
// de este test va el último.
// -------------------------------------------------------------------------------------------------
if ( document.readyState === 'loading' ) {

  document.addEventListener( 'DOMContentLoaded', function () {
    verificarLogicaFake();
  } );

} else {

  verificarLogicaFake();

} // ()
