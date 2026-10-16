# Prompt 4: Código en el Teléfono de la Lógica Fake

Actúa como un desarrollador experto en Android (Java). Necesito que generes el código del método `enviarMedidaAlServidor()` dentro de la clase `ServicioEscucharBeacons.java` y una función de test automático por consola siguiendo esta especificación:

## 1. Diseño gráfico
*(Se adjunta la imagen del diseño de `ServicioEscucharBeacons.java` de la página 10 y el diseño de interfaz móvil de la página 8 del documento).*

   ------------------------ ServicioEscucharBeacons.java ------------------------
   |
   | ETIQUETA_LOG: Text
   | tiempoDeEspera: N
   | seguir: B
   |
   |
                     ServicioEscucharBeacons() -->
   |
   |
   intent: Intent --> onHandleIntent() -->
   |
   |
                     parar() -->
   |
   |
                     onDestroy() -->
   |
   |
   uuid: Text, major: N, minor: Z,
   txPower: Z, nombreEmisora: Text
                  --> enviarMedidaAlServidor() -->
   |
   ------------------------------------------------------------------------------

## 2. Aclaraciones en texto sobre el diseño
- `ServicioEscucharBeacons.java` es el servicio en segundo plano de la aplicación Android encargado de escuchar o simular la recepción de beacons y enviarlos al servidor REST.
- El método `enviarMedidaAlServidor()` recibe los 5 valores extraídos del beacon (`uuid`, `major`, `minor`, `txPower`, `nombreEmisora`), construye el paquete JSON y utiliza la clase `PeticionarioREST` para enviarlo por `HTTP POST` a `GuardarMedida.php`.

## 3. Directrices que detallen exactamente qué queremos y cómo lo queremos
- El método debe recibir los 5 parámetros: `String uuid, int major, int minor, int txPower, String nombreEmisora`.
- Debe empaquetar esos 5 datos en un `String` con formato JSON válido usando las claves exactas: `"uuid"`, `"major"`, `"minor"`, `"txPower"` y `"nombreEmisora"`.
- Añade una comprobación sencilla guardando el último `major` y `minor` enviados para no enviar dos veces seguidas la misma medida al servidor.
- Crea una instancia de `PeticionarioREST` e invoca `hacerPeticionREST("POST", urlDestino, cuerpoJSON, new PeticionarioREST.RespuestaREST() { ... })` apuntando a `../rest/GuardarMedida.php`.
- En el `callback(int codigo, String cuerpo)` de `RespuestaREST`, muestra por consola con `Log.d(ETIQUETA_LOG, ...)` el código HTTP y la respuesta recibida.

## 4. Comentarios
- Añade encima de la función un bloque de comentario con su firma en la notación oficial:
  `uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> enviarMedidaAlServidor() -->`
- Añade comentarios breves (`//`) explicando dónde se comprueba si la medida está repetida, dónde se construye el JSON y dónde se envía con `PeticionarioREST`.

## 5. Tests automáticos
- Sin usar librerías externas (sin JUnit), genera dentro de la clase un método `probarEnviarMedidaAlServidor()` que ejecute un test automático mostrando el veredicto por consola (`Log.d`):
  1. Llame a la función con datos de prueba (`uuid="EPSG-GTI-PROY-3A"`, `major=2817`, `minor=235`, `txPower=-53`, `nombreEmisora="GTI-Jose"`).
  2. Compruebe automáticamente mediante condicionales `if` si el JSON generado contiene los 5 campos esperados, si una segunda llamada idéntica seguida es filtrada por duplicidad, y si el servidor responde con `codigo == 200` y `"error": 0`.
  3. Imprima por consola `"TEST AUTOMÁTICO TELÉFONO OK"` si se cumple o `"TEST AUTOMÁTICO TELÉFONO ERROR"` si falla.