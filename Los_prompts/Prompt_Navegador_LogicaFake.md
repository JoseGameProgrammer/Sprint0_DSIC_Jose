# Prompt 5: Código en el Navegador de la Lógica Fake

Actúa como un desarrollador experto en JavaScript (Vanilla JS). Necesito que generes el archivo `web/ux/LogicaFake.js` con las funciones `obtenerMedidasFake(cb)` y `refrescarMedidas()`, junto con sus tests automáticos siguiendo esta especificación:

## 1. Diseño gráfico
*(Se adjunta la imagen del diagrama `ux/LogicaFake.js` de la página 11 del documento de diseño).*

          ---------------- ux/LogicaFake.js ----------------
          |
cb: Callback --> obtenerMedidasFake() <--
          |
          |
             --> refrescarMedidas() -->
          |
          --------------------------------------------------

Medida = (
    id: N,
    uuid: Text,
    major: N,
    minor: Z,
    txPower: Z,
    nombreEmisora: Text,
    fechaLectura: Text
)

## 2. Aclaraciones en texto sobre el diseño
- `ux/LogicaFake.js` comunica la página web del navegador con el servidor REST y actualiza las tablas de la interfaz.
- `obtenerMedidasFake(cb)` es una función de lectura (`<--` a la derecha) que recibe una función callback `cb`, pide al servidor las medidas almacenadas mediante `HTTP GET` y se las pasa al callback.
- `refrescarMedidas()` es una función con efectos sobre la vista (`-->` a la derecha) que invoca a `obtenerMedidasFake()` y repinta las tablas HTML cada 3 segundos.

## 3. Directrices que detallen exactamente qué queremos y cómo lo queremos
- En `web/ux/LogicaFake.js`:
  - `obtenerMedidasFake(cb)`:
    - Realiza una petición asíncrona `GET` mediante `XMLHttpRequest` a `../rest/ObtenerMedidas.php?cuantasComoMaximo=100`.
    - Al recibir el JSON `{ "medidas": [ Medida ] }`, invoca `cb(respuesta.medidas)`.
  - `refrescarMedidas()`:
    - Invoca a `obtenerMedidasFake(function(medidas) { ... })` y actualiza las dos tablas del DOM:
      1. Tabla "Última medida registrada" (`#tbody-ultima-medida`): pinta únicamente el primer elemento (`medidas[0]`) con las 5 columnas: `id`, `minor` (Medición), `uuid`, `nombreEmisora` y `fechaLectura`.
      2. Tabla "Medidas anteriores" (`#tbody-medidas-anteriores`): pinta el resto de elementos (`medidas.slice(1)`) con las mismas 5 columnas.
  - Inicia un temporizador con `setInterval(refrescarMedidas, 3000)` al cargar la página para refrescar los datos cada 3 segundos.

## 4. Comentarios
- Añade en la cabecera de cada función su firma exacta en nuestra notación oficial:
  - `cb: Callback --> obtenerMedidasFake() <--`
  - `--> refrescarMedidas() -->`
- Añade comentarios breves (`//`) explicando la petición HTTP asíncrona, la separación entre `medidas[0]` y `medidas.slice(1)`, y la actualización del DOM.

## 5. Tests automáticos
- Genera un archivo o función de test automático (`testLogicaFake.js`) ejecutable en la consola del navegador que:
  1. Simule `XMLHttpRequest` devolviendo una lista de 3 objetos `Medida` de prueba (IDs 5, 4 y 3) y compruebe con `if` que `obtenerMedidasFake()` entrega los 3 elementos al callback.
  2. Ejecute `refrescarMedidas()` y verifique automáticamente que la tabla "Última medida registrada" contiene 1 fila con el ID 5 y la tabla "Medidas anteriores" contiene 2 filas con los IDs 4 y 3.
  3. Imprima por consola `"TEST LOGICA FAKE NAVEGADOR OK"` o `"TEST LOGICA FAKE NAVEGADOR ERROR"`.