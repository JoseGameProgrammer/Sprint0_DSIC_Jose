// ==================================================================================================
// DISEÑO LÓGICO: ux/LogicaFake.js (lógica del navegador, "fake")
// --------------------------------------------------------------------------------------------------
//
//                      ---------------- ux/LogicaFake.js ----------------
//                      |
//   cb: Callback --> obtenerMedidasFake() <--
//                      |
//                      |
//                         --> refrescarMedidas() -->
//                      |
//                      --------------------------------------------------
//
//   Medida = (
//       id: N,
//       uuid: Text,
//       major: N,
//       minor: Z,
//       txPower: Z,
//       nombreEmisora: Text,
//       fechaLectura: Text
//   )
//
// QUÉ ES ESTE FICHERO
//
// Es la capa que hace de puente entre el servidor REST (los PHP del prompt 3) y la
// tabla del navegador (el index.html del prompt 6). Por eso se llama "fake": en esta
// aplicación la lógica de verdad está en el servidor, y lo de aquí es una copia
// de lectura que solo pinta en pantalla.
//
// OJO CON LA DIRECCIÓN DE LAS FLECHAS, que es lo que distingue a las dos funciones:
//
//   cb: Callback --> obtenerMedidasFake() <--
//     A la derecha entra el callback, por la izquierda sale. NO toca el estado de
//     nada: se limita a leer y a devolver lo que el servidor le ha dado. Si se
//     llamara dos veces con el mismo estado, el resultado sería el mismo.
//
//         --> refrescarMedidas() -->
//     Las dos flechas a la derecha: SÍ produce un efecto, porque pinta filas en el
//     DOM y además arranca un temporizador de refresco. Es un efecto sobre la VISTA,
//     no sobre los datos: los datos no cambian, lo que cambia es lo que se ve.
//
// SEPARAR LAS DOS COSAS ES LO IMPORTANTE
// ---------------------------------------
// obtenerMedidasFake() NO sabe nada del DOM: ni de tablas, ni de ids, ni de qué se
// pinte. Solo sabe pedir datos y devolverlos. Quien sabe de la tabla es
// refrescarMedidas().
//
// Por qué importa: así se puede pedir las medidas desde la consola (por ejemplo,
// escribir obtenerMedidasFake(function(m){ console.log(m) }) y verlas en la consola)
// sin que se toque nada de la pantalla. Y si algún día la tabla cambiara de forma,
// no habría que tocar la función de pedir datos.
//
// DE DÓNDE SALEN LOS DATOS
// -------------------------
// Del endpoint del prompt 3:
//
//      GET ../rest/ObtenerMedidas.php?cuantasComoMaximo=100
//      --> { "medidas": [ Medida ] }
//
// OJO con la barra final de la URL: /ux/LogicaFake.js baja un nivel con "../" para
// llegar a /rest/. Si esa barra faltara, el navegador buscaría /ux/rest/ y daría 404.
//
// OJO con el 100: es cuántas medidas se piden COMO MÁXIMO, no cuántas se pintan. Es un
// tope, como en el diseño del endpoint. Aquí no se usa para nada más.
// ==================================================================================================
// --------------------------------------------------------------------------------------------------

// La dirección del servidor REST. Se separa del final de la URL a propósito: la barra
// final va en la constante de arriba, y la ruta del endpoint aquí, para que sea
// evidente que una termina en "/" y la otra no.
var RUTA_SERVICIO_MEDIDAS = '../rest/ObtenerMedidas.php?cuantasComoMaximo=100';

// Cada cuántos milisegundos se vuelve a pedir y a pintar las medidas. Lo dice el
// enunciado: 3000 milisegundos, o sea, cada 3 segundos.
var MILISEGUNDOS_REFRESCO = 3000;

// Guarda el identificador del temporizador del refresco automático.
//
// Para qué sirve guardarlo, si solo se usa para pararlo: un setInterval() devuelve un
// número, y con ese número se puede cancelar con clearInterval(). Si no se guardara,
// el temporizador se pondría en marcha y no se podría parar nunca desde la consola, y
// tampoco lo podría parar el test automático de testLogicaFake.js para trabajar con
// datos falsos sin que el refresco real lo cambiara por debajo. Vale null cuando no
// hay ningún temporizador en marcha.
var idDelRefresco = null;

// -------------------------------------------------------------------------------------------------
// --> obtenerMedidasFake() <--
// -------------------------------------------------------------------------------------------------
//   cb: Callback --> obtenerMedidasFake() <--
//
// Qué hace: pide al servidor REST las medidas guardadas y se las pasa al callback cb.
//
//   Es una función de LECTURA (la flecha <-- está a la derecha). No modifica nada.
//
// CÓMO VA:
//   1. Prepara una petición HTTP GET con XMLHttpRequest.
//   2. Cuando el servidor conteste, convierte el JSON recibido en un objeto de JavaScript.
//   3. Le pasa a cb el array de medidas.
//
// POR QUÉ XMLHttpRequest Y NO fetch
// ---------------------------------
// Porque es lo que pide el enunciado, y además funciona en navegadores viejos sin
// necesidad de ningún polyfill. (fetch es más moderno, pero necesita Promise y no existe en
// navegadores antiguos).
//
// OJO CON LA ASINCRONÍA, QUE ES LO IMPORTANTE
// --------------------------------------------
// Esta función NO devuelve las medidas: devuelve undefined, y las medidas llegan DESPUÉS,
// cuando el servidor conteste, que puede ser dentro de 5 milisegundos o dentro de 5
// segundos. Por eso se le pasa un callback en vez de devolverlas: quien quiera las
// medidas le pasa una función, y esa función se ejecuta cuando haya datos.
//
//   MAL:  var medidas = obtenerMedidasFake();      // sería undefined, siempre
//   BIEN: obtenerMedidasFake(function(medidas) { console.log(medidas) });
//
// -------------------------------------------------------------------------------------------------
function obtenerMedidasFake( cb ) {

  // El "new XMLHttpRequest()" se escribe sin la palabra "new" delante de la función a la
  // que se llama, pero SIN new NO funciona: sin new no se crea el objeto.
  var laPeticion = new XMLHttpRequest();

  // open() dice QUÉ se pide (método GET) y A DÓNDE (la URL del endpoint REST).
  //
  // El tercer parámetro (true) es el "asíncrono": si fuera false, el navegador bloquearía
  // la página entera hasta que el servidor contestara, y la tabla se quedaría congelada.
  // Con true, el navegador sigue pintando la página mientras espera.
  laPeticion.open( 'GET', RUTA_SERVICIO_MEDIDAS, true );

  // onload es lo que se ejecuta cuando el servidor YA HA CONTESTADO, con éxito o sin éxito.
  // Es el equivalente al "entonces" de una promesa, pero en este estilo más antiguo.
  laPeticion.onload = function () {

    var laRespuesta = null;

    try {
      // Se convierte el texto JSON en un objeto. El segundo true es para que salga un
      // objeto y no un array de pares.
      laRespuesta = JSON.parse( laPeticion.responseText );
    } catch ( elError ) {
      // Si el servidor devuelve algo que no es JSON (un aviso de PHP, una página de
      // error del servidor...), JSON.parse() lanza una excepción. Sin esta trampa, el
      // error se vería en la consola y además la tabla se quedaría con las filas viejas.
      avisarUnaVez( 'obtenerMedidasFake(): la respuesta del servidor no es JSON: ' + elError.message );
      cb( [] );
      return;
    }

    // Si el servidor contesta con un código de error (404, 500...), no se fía. Puede que
    // la respuesta no tenga la forma {"medidas": [...]}.
    if ( laPeticion.status < 200 || laPeticion.status >= 300 ) {
      avisarUnaVez( 'obtenerMedidasFake(): el servidor ha contestado con el código ' + laPeticion.status );
      cb( [] );
      return;
    }

    // Si el JSON es válido pero no trae la clave "medidas", también se trata como lista
    // vacía, y no como un error, porque a lo mejor el backend todavía no está escrito.
    if ( laRespuesta === null || ! laRespuesta.medidas ) {
      cb( [] );
      return;
    }

    // Camino normal: se le pasan las medidas al callback que le ha dado quien llama.
    cb( laRespuesta.medidas );

  };

  // onerror es lo que se ejecuta si la petición NO LLEGA a hacer nada: el servidor está
  // apagado, la URL está mal escrita, no hay red...
  //
  // OJO: si el servidor no está en marcha, el navegador NO da un 404 (eso sería "el
  // servidor existe pero no tiene ese archivo"). Da un error de red, que es justo lo
  // que se recoge aquí. Por eso se pinta una lista vacía: es preferible una tabla
  // diciendo "no hay medidas" a una tabla con datos viejos que ya no son ciertos.
  laPeticion.onerror = function () {
    avisarUnaVez( 'obtenerMedidasFake(): no se ha podido contactar con el servidor. ¿Está levantado y en la URL correcta?' );
    cb( [] );
  };

  // send() manda la petición. En un GET no lleva cuerpo, que es lo que se hace con
  // abrir otro paréntesis y nada dentro.
  laPeticion.send();

} // ()

// -------------------------------------------------------------------------------------------------
// --> refrescarMedidas() -->
// -------------------------------------------------------------------------------------------------
//   --> refrescarMedidas() -->
//
// Qué hace: pide las medidas al servidor y repinta las dos tablas de la página.
//
//   Esta función SÍ tiene efecto (las dos flechas -->): cambia lo que se ve en pantalla.
//
// LOS DOS CORTES DE LA LISTA, QUE SON EL CORAZÓN DEL EJERCICIO
// -----------------------------------------------------------
// El endpoint devuelve las medidas ordenadas de la MÁS RECIENTE a la MÁS ANTIGUA
// (ORDER BY ID DESC en el backend). Y el diseño de la interfaz quiere la última en
// una tabla aparte, y todas las demás en otra:
//
//     medidas[0]         -> la más reciente      -> tabla "Última medida registrada"
//     medidas.slice(1)   -> todas las demás     -> tabla "Medidas anteriores"
//
// OJO con slice(1): el 1 es porque el índice del array empieza en CERO. Slice(1) quita
// el primer elemento y devuelve el resto. Un slice(0) devolvería TODAS, y la más
// reciente aparecería dos veces, una en cada tabla. Ese es el error típico.
//
// OJO con que la lista puede estar vacía: si no hay ninguna medida, medidas[0] es
// undefined. Por eso antes de pintar la primera se comprueba que la lista tenga algo.
// -------------------------------------------------------------------------------------------------
function refrescarMedidas() {

  // Se llama a obtenerMedidasFake() con un callback. Todo lo que va entre llaves es lo
  // que se ejecuta cuando lleguen los datos, o sea, más adelante.
  obtenerMedidasFake( function ( medidas ) {

    pintarLaUltimaMedida( medidas[0] );
    pintarLasMedidasAnteriores( medidas.slice( 1 ) );

  } );

} // ()

// -------------------------------------------------------------------------------------------------
// --> pintarLaUltimaMedida() -->
// -------------------------------------------------------------------------------------------------
//   unaMedida: Medida --> pintarLaUltimaMedida() -->
//
// Qué hace: pinta UNA fila en la tabla "Última medida registrada".
//
// Si la medida no existe (undefined), que es lo que pasa cuando la lista está vacía,
// se pinta una fila que lo dice, en vez de dejar la tabla en blanco. Una tabla vacía
// sin explicación parece un fallo; una fila que dice "no hay medidas" dice la verdad.
// -------------------------------------------------------------------------------------------------
function pintarLaUltimaMedida( unaMedida ) {

  var elCuerpo = document.getElementById( 'tbody-ultima-medida' );

  if ( elCuerpo === null ) {
    return;
  } // ()

  // Vaciar el <tbody> antes de meter filas nuevas. OJO: se hace con innerHTML = '' en
  // vez de con removeChild(), porque es más corto y aquí no hay nada que conservar.
  //
  // OJO CON POR QUÉ SÍ SE USA innerHTML AQUÍ Y EN LAS CELDAS NO: vaciar un <tbody> con
  // innerHTML no mete nada dentro, es literalmente "déjalo en blanco". En cambio, para
  // escribir DENTRO de las celdas se usa textContent, y eso es a propósito, porque si un
  // beacon trajera un nombreEmisora con <b> dentro y se metiera con innerHTML, el
  // navegador lo interpretaría como HTML y no como texto. Con textContent, "<b>" sale
  // escrito tal cual y no se ejecuta nada. Es la diferencia entre imprimir un dato y
  // ejecutarlo.
  elCuerpo.innerHTML = '';

  if ( ! unaMedida ) {
    elCuerpo.appendChild( filaDe( 'No hay ninguna medida registrada todavía' ) );
    return;
  } // ()

  elCuerpo.appendChild( filaDeMedida( unaMedida ) );

} // ()

// -------------------------------------------------------------------------------------------------
// --> pintarLasMedidasAnteriores() -->
// -------------------------------------------------------------------------------------------------
//   lasMedidas: [ Medida ] --> pintarLasMedidasAnteriores() -->
//
// Qué hace: pinta una fila por cada medida de la lista en la tabla del historial.
//
// Se pinta cada fila con un bucle for. Con "for (var i = 0; ...)" y no con for...of,
// porque con for...of no se tiene el índice, y aquí no se necesita: con este índice es
// como se hace el bucle clásico y se ve claramente dónde empieza y dónde acaba.
// -------------------------------------------------------------------------------------------------
function pintarLasMedidasAnteriores( lasMedidas ) {

  var elCuerpo = document.getElementById( 'tbody-medidas-anteriores' );

  if ( elCuerpo === null ) {
    return;
  } // ()

  elCuerpo.innerHTML = '';

  if ( ! lasMedidas || lasMedidas.length === 0 ) {
    elCuerpo.appendChild( filaDe( 'No hay medidas anteriores' ) );
    return;
  } // ()

  for ( var i = 0 ; i < lasMedidas.length ; i++ ) {
    elCuerpo.appendChild( filaDeMedida( lasMedidas[i] ) );
  } // ()

} // ()

// -------------------------------------------------------------------------------------------------
// --> filaDeMedida() -->
// -------------------------------------------------------------------------------------------------
//   unaMedida: Medida --> filaDeMedida() --> [ Text ]
//
// Qué hace: devuelve una <tr> con las 5 celdas de una Medida, en el orden de las columnas.
//
// Devuelve el elemento ya construido y NO lo mete en la tabla: eso lo hace quien llama,
// con appendChild(). Esta función solo construye. Separar "construir" de "colocar" evita
// tener que escribir dos veces lo mismo para la tabla de la última medida y la del
// historial, que pintan filas exactamente iguales.
//
// EL ORDEN DE LAS CELDAS ES EL ORDEN DE LAS COLUMNAS
// --------------------------------------------------
// La tabla tiene 5 columnas, y aquí se meten 5 celdas, en este orden:
//
//     id  ->  minor  ->  uuid  ->  nombreEmisora  ->  fechaLectura
//    "ID"  "Medición"  "Uuid"  "Nombre Emisora"  "Fecha y Hora"
//
// Si se cambiasen de orden, el nombreEmisora aparecería bajo la columna ID. Ojo con esto.
//
// OJO CON LOS NÚMEROS: id y minor son números (N y Z) en el diseño, no textos. Se pasan
// tal cual al textContent, que los convierte a texto solo al pintarlos, que es
// exactamente lo que hace un <td>: guarda texto.
// -------------------------------------------------------------------------------------------------
function filaDeMedida( unaMedida ) {

  var laFila = document.createElement( 'tr' );

  laFila.appendChild( celdaDe( unaMedida.id ) );
  laFila.appendChild( celdaDe( unaMedida.minor ) );
  laFila.appendChild( celdaDe( unaMedida.uuid ) );
  laFila.appendChild( celdaDe( unaMedida.nombreEmisora ) );
  laFila.appendChild( celdaDe( unaMedida.fechaLectura ) );

  return laFila;

} // ()

// -------------------------------------------------------------------------------------------------
// --> celdaDe() -->
// -------------------------------------------------------------------------------------------------
//   unValor: Text --> celdaDe() --> [ Text ]
//
// Qué hace: devuelve una celda <td> con el texto dentro.
//
// OJO con el colspan: la celda de los mensajes ("No hay medidas") tiene que ocupar las 5
// columnas todas juntas, porque si no, se vería cortada en la primera columna. Por eso
// celdaDe() acepta un segundo parámetro para el colspan, que por defecto es 1 (o sea,
// una columna de las cinco, que es el caso normal de una Medida).
// -------------------------------------------------------------------------------------------------
function celdaDe( unValor, elNumeroDeColumnas ) {

  var laCelda = document.createElement( 'td' );

  laCelda.textContent = unValor;

  if ( elNumeroDeColumnas && elNumeroDeColumnas > 1 ) {
    laCelda.colSpan = elNumeroDeColumnas;
    laCelda.className = 'sin-medidas';
  } // ()

  return laCelda;

} // ()

// -------------------------------------------------------------------------------------------------
// --> filaDe() -->
// -------------------------------------------------------------------------------------------------
//   unTexto: Text --> filaDe() --> [ Text ]
//
// Qué hace: devuelve una <tr> con UNA sola celda que ocupa las 5 columnas. Es la fila que
//           se pinta cuando no hay nada que enseñar.
// -------------------------------------------------------------------------------------------------
function filaDe( unTexto ) {

  var laFila = document.createElement( 'tr' );

  laFila.appendChild( celdaDe( unTexto, 5 ) );

  return laFila;

} // ()

// -------------------------------------------------------------------------------------------------
// --> avisarUnaVez() -->
// -------------------------------------------------------------------------------------------------
//   unMensaje: Text --> avisarUnaVez() -->
//
// Qué hace: escribe un aviso en la consola, pero SOLO la primera vez que se llama con ese
//           mismo mensaje.
//
// POR QUÉ SOLO UNA VEZ
// --------------------
// El refresco automático llama a refrescarMedidas() cada 3 segundos. Si el servidor está
// apagado, sin esto la consola se llenaría de 20 avisos por minuto, y cuando alguien
// quisiera ver un error real no se vería entre tanto ruido. Con un registro de los
// mensajes ya escritos, se avisa una vez y punto.
//
// OJO con la variable "mensajesYaAvisados": es un objeto vacío que hace de diccionario.
// La forma de comprobar si algo está dentro es con "haOwnProperty", que devuelve true
// si la clave existe. Es la manera de tener un conjunto de textos en JavaScript sin
// necesitar un Set.
// -------------------------------------------------------------------------------------------------
var mensajesYaAvisados = {};

function avisarUnaVez( unMensaje ) {

  if ( mensajesYaAvisados.hasOwnProperty( unMensaje ) ) {
    return;
  } // ()

  mensajesYaAvisados[ unMensaje ] = true;

  if ( window.console && typeof window.console.warn === 'function' ) {
    window.console.warn( unMensaje );
  } // ()

} // ()

// -------------------------------------------------------------------------------------------------
// --> iniciarRefresco() -->
// -------------------------------------------------------------------------------------------------
//   --> iniciarRefresco() -->
//
// Qué hace: llama a refrescarMedidas() una vez enseguida y luego cada 3 segundos, con un
//           temporizador.
//
// POR QUÉ UNA LLAMADA INMEDIATA Y NO SOLO EL TEMPORIZADOR
// -------------------------------------------------------
// Si solo se pusiera el setInterval, la tabla tardaría 3 segundos en pintarse la primera
// vez, y se verían las filas de ejemplo del HTML (o nada) durante esos 3 segundos. Con
// una llamada antes de armar el temporizador, la tabla se pinta en cuanto hay respuesta.
//
// OJO con el idDelRefresco: se guarda para poder parar el temporizador después. Si se
// llama dos veces a esta función sin parar antes, se quedarían DOS temporizadores en
// marcha y cada petición se haría por el doble, así que al principio se para lo que
// hubiera.
//
// ESTA ES LA ÚNICA PARTE DE TODO EL FICHERO QUE USA EL NAVEGADOR Y NO EL DISEÑO LÓGICO
// --------------------------------------------------------------------------------------
// Es el arranque de la página, que no es lógica de negocio sino el equivalente en el
// navegador del "main" de un programa. Está aquí al final, y no en medio de las funciones
// de arriba, para que el diagrama se lea entero antes de ver este detalle.
// -------------------------------------------------------------------------------------------------
function iniciarRefresco() {

  // Si ya había un temporizador, se para antes de poner otro, o se acumularían.
  detenerRefresco();

  // La primera vez, sin esperar.
  refrescarMedidas();

  // Y a partir de ahora, cada 3 segundos. El 3000 son milisegundos.
  idDelRefresco = setInterval( refrescarMedidas, MILISEGUNDOS_REFRESCO );

} // ()

// -------------------------------------------------------------------------------------------------
// --> detenerRefresco() -->
// -------------------------------------------------------------------------------------------------
//   --> detenerRefresco() -->
//
// Qué hace: para el temporizador del refresco automático.
//
// Existe por dos motivos:
//   1. El test automático (testLogicaFake.js) la llama para poder trabajar con medidas
//      falsas sin que el refresco real se cuele y le cambie los datos por debajo.
//   2. Está bien tenerla, porque si en algún momento hace falta parar la página de
//      pedir cosas a lo servidor (por ejemplo, porque el servidor se ha caído), se
//      llama a esta función y se deja de preguntar cada 3 segundos.
//
// OJO: si no hay ningún temporizador en marcha, no hace nada. clearInterval() con un
// identificador que no existe no da ningún error, pero se comprueba igualmente para que
// quede claro en el código que el caso está pensado.
// -------------------------------------------------------------------------------------------------
function detenerRefresco() {

  if ( idDelRefresco !== null ) {
    clearInterval( idDelRefresco );
    idDelRefresco = null;
  } // ()

} // ()

// -------------------------------------------------------------------------------------------------
// ARRANQUE DE LA PÁGINA
// -------------------------------------------------------------------------------------------------
// OJO con el addEventListener: como en el test de la interfaz, se espera a que la página
// esté construida enteras antes de hacer nada. Si este código estuviera al principio del
// fichero, los <tbody> todavía no existirían y la tabla se pintaría a medias.
if ( document.readyState === 'loading' ) {
  document.addEventListener( 'DOMContentLoaded', function () {
    iniciarRefresco();
  } );

} else {
  iniciarRefresco();
} // ()
