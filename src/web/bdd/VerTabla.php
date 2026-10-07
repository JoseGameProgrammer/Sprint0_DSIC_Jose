<?php

/* =================================================================================================
   DISEÑO LÓGICO: VerTabla.php (ver la tabla Medidas en una tabla de verdad)
   -------------------------------------------------------------------------------------------------
   +--------------------------------------------------+
   |               BBDD: Tabla Medidas                |
   +--------------------------------------------------+
   | ID: N (Primary Key, AutoIncremental)             |
   | FechaLectura: Text                               |
   | Uuid: Text                                       |
   | Major: N                                         |
   | Minor: Z                                         |
   | TxPower: Z                                       |
   | NombreEmisora: Text                              |
   +--------------------------------------------------+

   Medida = (
       id: N,
       uuid: Text,
       major: N,
       minor: Z,
       txPower: Z,
       nombreEmisora: Text,
       fechaLectura: Text
   )

   TablaMedidas = [ Medida ]

   -------------------------------------------------------------------------------------------------
   QUÉ HACE ESTE SCRIPT

   Es un LECTOR de la base de datos. Enseña las filas que hay ahora mismo en Medidas,
   y ADEMÁS interpreta el campo major para decir de qué magnitud es cada fila (CO2,
   temperatura o ruido), porque el número solo no se entiende.

   Sirve para dos cosas:

      1. MIRAR QUÉ HAY: para cuando no quieres ir a la web ni hacer un curl, y simplemente
         quieres ver si se está guardando algo y qué.
      2. ENSÑAR ANTES DE ENTREGAR: el enunciado pide ver la medida guardada, y una tabla
         con las cabeceras y los valores bien puts es mucho más fácil de defender que
         un volcado de JSON.

   -------------------------------------------------------------------------------------------------
   CÓMO SE USA

   Desde el navegador (con el servidor encendido):

       http://localhost:8080/bdd/VerTabla.php
       http://localhost:8080/bdd/VerTabla.php?cuantasComoMaximo=20

   Desde la consola, sin necesidad de servidor:

       php .\web\bdd\VerTabla.php
       php .\web\bdd\VerTabla.php 20

   El script se da cuenta solo de dónde lo han abierto y cambia el formato: si viene
   del navegador pinta una tabla HTML, y si viene de la consola pinta una tabla de texto.

   -------------------------------------------------------------------------------------------------
   LO QUE ESTE FICHERO NO HACE

     - NO escribe NADA. Es de solo lectura. No llama a guardarMedida(), no hace INSERT,
       no borra. Puedes abrirlo todas las veces que quieras sin miedo de estropear nada.
     - NO usa SQL. La consulta la pide a obtenerMedidas(), que vive en la capa de lógica
       (../logica/LogicaMedidas.php). Este fichero solo pinta lo que le devuelvan.
       Es el mismo reparto de trabajo que hacen los endpoints de la carpeta rest/.
     - NO necesita phpMyAdmin, XAMPP ni ningún programa extra. Con el PHP que ya tienes
       y la extensión pdo_sqlite (que ya se usa para todo lo demás) es suficiente.
   ================================================================================================== */

// -------------------------------------------------------------------------------------------------
// 1) CAPA DE LÓGICA
// -------------------------------------------------------------------------------------------------
// Se pide la lógica, igual que hacen los endpoints. Aquí la flecha es --> porque este
// fichero CONSUME el resultado, no lo produce: la lógica no depende de este script.
require_once __DIR__ . '/../logica/LogicaMedidas.php';

// -------------------------------------------------------------------------------------------------
// 2) CUÁNTAS FILAS PEDIR
// -------------------------------------------------------------------------------------------------
// El límite sale del Query String si lo hay (navegador) o del primer argumento si lo hay
// (consola), y si no se dice nada se muestran 50.
//
// OJO con no leer $_GET en consola: en consola no existe el Query String y PHP avisa por
// consola. Por eso se pregunta antes con isset().
$limite = 50;

if ( isset( $_GET['cuantasComoMaximo'] ) ) {
  $limite = $_GET['cuantasComoMaximo'];
} elseif ( isset( $argv ) && isset( $argv[1] ) ) {
  $limite = $argv[1];
}

// Se limpia igual que lo hace obtenerMedidas(), pero aquí se avisa al usuario si lo que
// ha puesto no era un número, en vez de devolver callado una tabla vacía.
$limiteLimpio = filter_var( $limite, FILTER_VALIDATE_INT );
if ( $limiteLimpio === false || $limiteLimpio < 1 ) {
  $limiteLimpio = 50;
}

// -------------------------------------------------------------------------------------------------
// 3) LAS MEDIDAS
// -------------------------------------------------------------------------------------------------
$lasMedidas = obtenerMedidas( $limiteLimpio );

// -------------------------------------------------------------------------------------------------
// 4) INTERPRETAR EL major
// -------------------------------------------------------------------------------------------------
// El major NO es un número cualquiera: es dos cosas pegadas. El byte ALTO dice de qué
// magnitud es la medición y el byte BAJO dice el número de muestra.
//
// Arduino lo monta así (Publicador.h):
//
//     major = (tipoDeMedicion << 8) + numeroDeMuestra
//
// Y el tipo de medición es el enumerado MedicionesID de Publicador.h:
//
//     CO2 = 11
//     TEMPERATURA = 12
//     RUIDO = 13
//
// Por eso major / 256 devuelve el tipo: es la división entera por 256, que es justo
// quedarse con el byte alto.
//
// Ejemplos:
//     major 2817 = 11 * 256 + 1  -->  2817 / 256 = 11  -->  CO2, muestra 1
//     major 3073 = 12 * 256 + 1  -->  3073 / 256 = 12  -->  temperatura, muestra 1
//
// OJO CON UNA TRAMPA DE PHP: el signo "/" NO hace división entera, hace división con
// decimales. O sea, 2817 / 256 devuelve 11.00390625, y un "if ( $tipo === 11 )" da
// FALSE porque 11.00390625 no es igual a 11. Por eso aquí se usa intdiv(), que sí
// devuelve un entero limpio.
//
// OJO CON LOS RANGOS QUE CUENTAN MAL: el major no va de 2816 a 2831 como parece por el
// ejemplo, sino que el byte bajo es el contador del loop(), que es un uint8_t y llega
// hasta 255. Así que:
//     CO2         ->  major de 2816 a 3071   (11 * 256 ... 11 * 256 + 255)
//     TEMPERATURA ->  major de 3072 to 3327   (12 * 256 ... 12 * 256 + 255)
//     RUIDO       ->  major de 3328 a 3583   (13 * 256 ... 13 * 256 + 255)
// El tipo es SIEMPRE major / 256, sin excepciones.
/**
 * Devuelve el nombre legible de una magnitud a partir de su número de tipo.
 *
 * @param int $tipo Numero de tipo (byte alto del major).
 *
 * @return Text Nombre de la magnitud, o el texto de "desconocido" si no es uno de los tres.
 */
function nombreDeLaMagnitud( $tipo ) {

  if ( $tipo === 11 ) {
    return 'CO2 (ppm)';
  } // ()

  if ( $tipo === 12 ) {
    return 'Temperatura (grados)';
  } // ()

  if ( $tipo === 13 ) {
    return 'Ruido (dB)';
  } // ()

  return 'Desconocida (' . (int) $tipo . ')';

} // ()

/**
 * Devuelve el numero de muestra a partir del major.
 *
 * @param int $major Campo major completo.
 *
 * @return int Numero de muestra (byte bajo del major, o sea major % 256).
 */
function numeroDeMuestra( $major ) {

  return ((int) $major) % 256;

} // ()

// ¿Ha venido del navegador o de la consola? php_sapi_name() devuelve 'cli' cuando es
// consola y 'apache2'/'nginx'/'php' cuando es por HTTP. Esa es la forma de saberlo sin
// preguntar nada al usuario.
$esConsola = ( php_sapi_name() === 'cli' );

// -------------------------------------------------------------------------------------------------
// 5) SALIDA
// -------------------------------------------------------------------------------------------------

if ( $esConsola ) {

  /* -----------------------------------------------------------------------------------------------
     5.1) EN CONSOLA: tabla de texto
     ----------------------------------------------------------------------------------------------- */
  echo "" . PHP_EOL;
  echo "TABLA MEDIDAS (" . count( $lasMedidas ) . " medida(s) mostrada(s), limite pedido: "
       . (int) $limiteLimpio . ")" . PHP_EOL;
  echo str_repeat( '=', 110 ) . PHP_EOL;

  if ( count( $lasMedidas ) === 0 ) {
    echo "  (no hay ninguna fila todavia: eso significa que el movil no ha mandado nada)"
         . PHP_EOL;
  } // ()

  foreach ( $lasMedidas as $unaMedida ) {

    $tipo = intdiv( (int) $unaMedida['major'], 256 );

    echo "ID " . str_pad( (string) $unaMedida['id'], 4, ' ', STR_PAD_LEFT )
         . " | " . str_pad( (string) $unaMedida['fechaLectura'], 19 )
         . " | " . str_pad( (string) $unaMedida['nombreEmisora'], 13 )
         . " | major=" . str_pad( (string) $unaMedida['major'], 5 )
         . " minor=" . str_pad( (string) $unaMedida['minor'], 7 )
         . " (" . str_pad( nombreDeLaMagnitud( $tipo ), 20 ) . ")"
         . " tx=" . str_pad( (string) $unaMedida['txPower'], 5 )
         . " | " . $unaMedida['uuid'] . PHP_EOL;

  } // ()

  echo str_repeat( '=', 110 ) . PHP_EOL;

  if ( count( $lasMedidas ) > 0 ) {
    echo "La última de arriba es la más reciente: obtenerMedidas() trae ORDER BY ID DESC."
         . PHP_EOL;
  } // ()

  echo "" . PHP_EOL;

  // Si se ejecuta con include desde otro sitio, no se manda nada más. Una guarda de las
  // pocas que seEa razonables aquí: si no, este fichero se podría colar dentro de otro
  // por accidente y le soltaría la tabla en medio de su salida.
  return;

} // ()


/* -----------------------------------------------------------------------------------------------
   5.2) EN EL NAVEGADOR: tabla HTML
   ----------------------------------------------------------------------------------------------- */

header( 'Content-Type: text/html; charset=utf-8' );

?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Tabla Medidas</title>
<style>
/* Estilos mínimos y sin dependencias: ni Bootstrap ni nada. El objetivo es que se lea
   bien proyectado en una reunión, no que quede bonito. */
body   { font-family: Consolas, "Courier New", monospace; margin: 24px; background: #fff; }
h1     { font-size: 20px; margin: 0 0 4px 0; }
p.sub  { color: #555; margin: 0 0 18px 0; font-size: 13px; }
table  { border-collapse: collapse; font-size: 13px; }
th     { background: #e8e8e8; border: 1px solid #999; padding: 5px 9px; text-align: left; }
td     { border: 1px solid #ccc; padding: 5px 9px; }
tr:nth-child(even) td { background: #f6f6f6; }   /* zebra, para no perder la fila al leer */
.vacia { color: #a00; font-weight: bold; }
.tipoCO2 { color: #060; }
.tipoTEM { color: #a60; }
</style>
</head>
<body>

<h1>Tabla Medidas</h1>
<p class="sub">
  <?php echo count( $lasMedidas ); ?> medida(s) mostrada(s), l&iacute;mite pedido:
  <?php echo (int) $limiteLimpio; ?>. La de arriba es la m&aacute;s reciente.
  <br>La BBDD es un fichero SQLite: <code>web/bdd/bdd.sqlite</code>.
</p>

<?php if ( count( $lasMedidas ) === 0 ) { ?>

<p class="vacia">
  No hay ninguna fila. O el m&oacute;vil no ha mandado nada todav&iacute;a, o la BBDD
  est&aacute; en otra carpeta. Prueba a arrancar el servidor y a darle al bot&oacute;n
  &laquo;Buscar NUESTRO dispositivo&raquo; en el m&oacute;vil.
</p>

<?php } else { ?>

<table>

  <tr>
    <th>ID</th>
    <th>Fecha</th>
<th>ID_Sensor</th>
<th>Tipo_Medicion</th>
<th>Valor_Medicion</th>
    <!-- -->
    <!-- -->
    <!-- -->
    <!-- -->
    <th>Magnitud</th>
    <th>Muestra</th>
    <!-- -->
  </tr>

<?php foreach ( $lasMedidas as $unaMedida ) {

  $tipo    = intdiv( (int) $unaMedida['major'], 256 );
  $muestra = numeroDeMuestra( $unaMedida['major'] );

  // Las clases solo pintan el color; el texto ya viene escrito arriba por nombreDeLaMagnitud().
  if ( $tipo === 11 ) {
    $clase = 'tipoCO2';
  } elseif ( $tipo === 12 ) {
    $clase = 'tipoTEM';
  } else {
    $clase = '';
  } // ()

  ?>
  <tr>
    <td><?php echo (int) $unaMedida['id']; ?></td>
    <td><?php echo htmlspecialchars( $unaMedida['fechaLectura'] ); ?></td>
    <td><?php echo htmlspecialchars( $unaMedida['nombreEmisora'] ); ?></td>
    <td><?php echo htmlspecialchars( $unaMedida['uuid'] ); ?></td>
    <td><?php echo (int) $unaMedida['major']; ?></td>
    <td><?php echo (int) $unaMedida['minor']; ?></td>
    <td class="<?php echo $clase; ?>"><?php echo htmlspecialchars( nombreDeLaMagnitud( $tipo ) ); ?></td>
    <td><?php echo (int) $muestra; ?></td>
    <td><?php echo (int) $unaMedida['txPower']; ?></td>
  </tr>
<?php } // () ?>

</table>

<?php } // () ?>

</body>
</html>