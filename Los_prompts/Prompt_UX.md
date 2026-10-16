# Prompt 6: Generación de la UX en el Navegador

Actúa como un desarrollador experto en HTML5 y CSS3. Necesito que generes el código de la interfaz gráfica en el navegador (`web/ux/index.html`) y sus tests automáticos siguiendo esta especificación:

## 1. Diseño gráfico
**NOTA IMPORTANTE:** *Junto con este prompt te enviaré las imágenes con los diseños de interfaz gráfica que aparecen en las páginas 8 y 9 de mi documento (debes replicar especialmente la imagen de la página 9 correspondiente a la Web).*

+-------------------------------------------------------------------------+
|   SPRINT 0 - WEB con conexión a nuestra base de datos con medidas       |
+-------------------------------------------------------------------------+

                  Última medida registrada
+----+----------+------------------+----------------+---------------------+
| ID | Medición | Uuid             | Nombre Emisora | Fecha y Hora        |
+----+----------+------------------+----------------+---------------------+
| 5  | 1234     | EPSG-GTI-PROY-3A | GTI-Jose       | 2026-09-28 20:45:08 |
+----+----------+------------------+----------------+---------------------+

                     Medidas anteriores
+----+----------+------------------+----------------+---------------------+
| ID | Medición | Uuid             | Nombre Emisora | Fecha y Hora        |
+----+----------+------------------+----------------+---------------------+
| 4  | 1234     | EPSG-GTI-PROY-3A | GTI-Jose       | 2026-09-28 20:45:08 |
| 3  | 1234     | EPSG-GTI-PROY-3A | GTI-Jose       | 2026-09-28 19:45:08 |
| 2  | 1234     | EPSG-GTI-PROY-3A | GTI-Jose       | 2026-09-28 18:45:08 |
| 1  | 1234     | EPSG-GTI-PROY-3A | GTI-Jose       | 2026-09-28 17:45:08 |
+----+----------+------------------+----------------+---------------------+

## 2. Aclaraciones en texto sobre el diseño
- La página web debe reproducir exactamente la disposición, colores y estructura que verás en la imagen del diseño Web que te adjunto:
  1. Cabecera superior a ancho completo con fondo morado/azul claro (`#9999ff` aprox.) y el título centrado: `"SPRINT 0 - WEB con conexión a nuestra base de datos con medidas"`.
  2. Bloque central superior con el título `"Última medida registrada"` y una tabla centrada cuya cabecera tiene fondo verde intenso y muestra solo la medición más reciente.
  3. Bloque central inferior con el título `"Medidas anteriores"` y una tabla centrada cuya cabecera tiene fondo verde claro y filas alternando fondo blanco y gris suave para el historial de mediciones.
- Ambas tablas tienen exactamente 5 columnas: `ID`, `Medición`, `Uuid`, `Nombre Emisora` y `Fecha y Hora`.

## 3. Directrices que detallen exactamente qué queremos y cómo lo queremos
- Genera el archivo `web/ux/index.html` con HTML5 limpio y CSS integrado (o enlazado) fiel a la imagen proporcionada.
- El `<tbody>` de la tabla superior debe tener el atributo `id="tbody-ultima-medida"`.
- El `<tbody>` de la tabla inferior debe tener el atributo `id="tbody-medidas-anteriores"`.
- Incluye al final del `<body>` la etiqueta `<script src="LogicaFake.js"></script>` para conectar la interfaz con la lógica del navegador.

## 4. Comentarios
- Añade en la cabecera del archivo HTML un bloque de comentario (`<!-- ... -->`) que incluya la definición en nuestra notación oficial del tipo de dato que se pinta en las tablas:
  `Medida = ( id: N, uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text, fechaLectura: Text )`
- Añade comentarios breves en el HTML señalando la barra de título, la tabla de la última medida y la tabla de medidas anteriores.

## 5. Tests automáticos
- Añade un script de test automático de interfaz (`testUX.js` o función `verificarUX()`) que se ejecute por consola del navegador y compruebe mediante `if`:
  1. Que existen los contenedores `#tbody-ultima-medida` y `#tbody-medidas-anteriores`.
  2. Que ambas tablas cuentan exactamente con las 5 cabeceras `<th>`: `"ID"`, `"Medición"`, `"Uuid"`, `"Nombre Emisora"` y `"Fecha y Hora"`.
  3. Que imprima por consola `"TEST UX NAVEGADOR OK"` si todos los elementos están presentes o `"TEST UX NAVEGADOR ERROR"` si falta alguno.