# Prompt 2: Lógica del Negocio en el Backend (la verdadera)

Actúa como un desarrollador experto en PHP y SQL. Necesito que generes el fichero `web/logica/LogicaMedidas.php` con las funciones `guardarMedida()` y `obtenerMedidas()` y sus tests automáticos siguiendo esta especificación:

## 1. Diseño gráfico
*(Se adjunta la imagen del diagrama `logica/LogicaMedidas.php` de la página 11 del documento de diseño).*

          ------------- logica/LogicaMedidas.php -------------
          |
uuid: Text, major: N, minor: Z, txPower: Z,
nombreEmisora: Text
      --> guardarMedida() -->
    B <--
          |
          |
cuantasComoMaximo: N
      --> obtenerMedidas() <--
[ Medida ] <--
          |
          ----------------------------------------------------

## 2. Aclaraciones en texto sobre el diseño
- Esta capa contiene la lógica verdadera del negocio en el servidor y no sabe nada de HTTP ni de sesiones; recibe parámetros directos y devuelve datos en los tipos definidos.
- `guardarMedida()` modifica el estado de la base de datos (`-->` a la derecha) insertando una nueva medida y devuelve un booleano (`B <--` a la izquierda: `true` si se guardó, `false` si falló).
- `obtenerMedidas()` solo consulta el estado (`<--` a la derecha), recibe la cantidad máxima de registros a devolver (`cuantasComoMaximo: N`) y retorna una lista de medidas (`[ Medida ] <--`).

## 3. Directrices que detallen exactamente qué queremos y cómo lo queremos
- En `web/logica/LogicaMedidas.php`:
  - `guardarMedida($uuid, $major, $minor, $txPower, $nombreEmisora)`:
    - Valida que los parámetros de texto no estén vacíos y los numéricos sean enteros.
    - Ejecuta una sentencia SQL preparada (PDO `prepare` y `execute`) con:
      `INSERT INTO Medidas (FechaLectura, Uuid, Major, Minor, TxPower, NombreEmisora) VALUES (datetime('now','localtime'), :uuid, :major, :minor, :txPower, :nombreEmisora)`.
    - Devuelve `true` si la inserción fue correcta y `false` en caso de error.
  - `obtenerMedidas($cuantasComoMaximo = 100)`:
    - Ejecuta una sentencia SQL preparada con:
      `SELECT ID as id, Uuid as uuid, Major as major, Minor as minor, TxPower as txPower, NombreEmisora as nombreEmisora, FechaLectura as fechaLectura FROM Medidas ORDER BY ID DESC LIMIT :limite`.
    - Devuelve un array de arrays asociativos con las 7 claves exactas del tipo `Medida`.

## 4. Comentarios
- En la cabecera de cada función incluye un bloque de comentario obligatorio con la firma exacta en nuestra notación oficial:
  - `uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B`
  - `cuantasComoMaximo: N --> obtenerMedidas() --> [ Medida ]`
- Añade comentarios breves (`//`) explicando la preparación de las consultas SQL, la asignación de parámetros y el manejo de errores.

## 5. Tests automáticos
- Genera un script `testLogicaMedidas.php` ejecutable por consola que:
  1. Llame a `guardarMedida("EPSG-GTI-PROY-3A", 2817, 235, -53, "GTI-Jose")` y compruebe con `assert` o `if` que devuelve `true`.
  2. Llame a `obtenerMedidas(5)` y verifique automáticamente que devuelve un array no vacío, en orden descendente de `id` y con las 7 propiedades de `Medida`.
  3. Imprima por consola `"TEST LOGICA BACKEND OK"` si todo se cumple o `"TEST LOGICA BACKEND ERROR"` si algo falla.