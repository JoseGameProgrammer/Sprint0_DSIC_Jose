# Prompt 3: Servidor REST en el Backend

Actúa como un desarrollador experto en PHP. Necesito que generes los endpoints REST `web/rest/GuardarMedida.php` y `web/rest/ObtenerMedidas.php` junto con sus tests automáticos siguiendo esta especificación:

## 1. Diseño gráfico
*(Se adjunta el diagrama de la página 3 con las flechas `HTTP POST (JSON)` y `HTTP GET (JSON)` hacia el Servidor REST y el diseño de rutas de las páginas 6 y 7).*

1) Método y ruta: POST ../rest/GuardarMedida.php
   Entrada (Cuerpo JSON):
   {
       uuid: Text,
       major: N,
       minor: Z,
       txPower: Z,
       nombreEmisora: Text
   }
   Llamada a la lógica:
   uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
   --> guardarMedida() --> B
   Salida (JSON):
   - Si B = true:  { error: 0, mensaje: Text }
   - Si B = false: { error: 1, mensaje: Text }

2) Método y ruta: GET ../rest/ObtenerMedidas.php?cuantasComoMaximo=n
   Entrada (URL): cuantasComoMaximo: N
   Llamada a la lógica:
   cuantasComoMaximo: N --> obtenerMedidas() --> [ Medida ]
   Salida (JSON):
   {
       medidas: [ Medida ]
   }

## 2. Aclaraciones en texto sobre el diseño
- Los endpoints de la carpeta `rest/` actúan únicamente como intermediarios HTTP: reciben las peticiones, extraen los parámetros del JSON o de la URL, invocan las funciones de `logica/LogicaMedidas.php` y devuelven la respuesta codificada en JSON.
- No deben contener sentencias SQL directamente.

## 3. Directrices que detallen exactamente qué queremos y cómo lo queremos
- Ambos ficheros deben declarar la cabecera `header('Content-Type: application/json; charset=utf-8');` e incluir `require_once '../logica/LogicaMedidas.php';`.
- En `web/rest/GuardarMedida.php`:
  - Lee el cuerpo JSON recibido por `POST` mediante `file_get_contents('php://input')` y `json_decode(..., true)`.
  - Comprueba que estén presentes los 5 campos (`uuid`, `major`, `minor`, `txPower`, `nombreEmisora`).
  - Invoca `guardarMedida($datos['uuid'], $datos['major'], $datos['minor'], $datos['txPower'], $datos['nombreEmisora'])`.
  - Si devuelve `true`, imprime en JSON `{ "error": 0, "mensaje": "Medida guardada correctamente" }`; si devuelve `false` o faltan campos, imprime `{ "error": 1, "mensaje": "Error al guardar la medida" }`.
- En `web/rest/ObtenerMedidas.php`:
  - Lee el parámetro `$_GET['cuantasComoMaximo']` (por defecto `100` si no viene en la URL).
  - Invoca `obtenerMedidas($cuantasComoMaximo)` y devuelve el JSON `{ "medidas": $listaMedidas }`.

## 4. Comentarios
- En la cabecera de cada archivo PHP incluye un bloque de comentario con el método, la ruta, el formato JSON de entrada/salida y la firma en nuestra notación de la función lógica que llama.
- Añade comentarios breves (`//`) explicando la lectura de la petición, la validación de parámetros y la generación del JSON de salida.

## 5. Tests automáticos
- Genera un script `testServidorREST.php` que pruebe automáticamente ambos endpoints:
  1. Envíe una petición `POST` simulada o vía HTTP local a `GuardarMedida.php` con un JSON de prueba y compruebe con un `if` que devuelve `"error": 0`.
  2. Envíe una petición `GET` a `ObtenerMedidas.php?cuantasComoMaximo=2` y verifique que el JSON devuelto contiene el array `"medidas"` con los datos esperados.
  3. Imprima por consola `"TEST SERVIDOR REST OK"` o `"TEST SERVIDOR REST ERROR"`.