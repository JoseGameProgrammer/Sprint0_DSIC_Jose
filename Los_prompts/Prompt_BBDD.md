# Prompt 1: Generación de la Base de Datos

Actúa como un desarrollador experto en bases de datos SQL (SQLite / MySQL). Necesito que generes el código para crear la base de datos de nuestro sistema y sus tests automáticos siguiendo esta especificación:

## 1. Diseño gráfico
*(Se adjunta la imagen del cilindro de la Base de Datos de la página 3 y el esquema de la tabla Medidas de la página 4 del documento de diseño).*

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

## 2. Aclaraciones en texto sobre el diseño
- El diseño cumple con la 1FN (valores atómicos), 2FN (dependencia completa de la clave primaria ID) y 3FN (sin dependencias transitivas).
- ID: N -> Clave primaria autoincremental (número natural sin signo) que identifica cada fila.
- FechaLectura: Text -> Fecha y hora del momento en que se guarda la medición en el servidor.
- Uuid: Text -> Identificador de 16 bytes de la placa emisora para filtrar anuncios Bluetooth.
- Major: N -> Campo de 2 bytes donde el byte alto es el tipo de medición y el byte bajo es el contador de muestra.
- Minor: Z -> Campo de 2 bytes con signo que transporta el valor real medido por el sensor (CO2 o temperatura).
- TxPower: Z -> Campo de 1 byte con signo que indica la potencia de referencia de la señal en dBm.
- NombreEmisora: Text -> Cadena con el nombre Bluetooth con el que se anuncia la placa física.

## 3. Directrices que detallen exactamente qué queremos y cómo lo queremos
- Genera un archivo `crearBD.sql` que ejecute un `CREATE TABLE IF NOT EXISTS Medidas`.
- Mapea los tipos de la notación formal a tipos SQL:
  - `ID: N` -> `INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL`
  - `FechaLectura: Text` -> `TEXT NOT NULL DEFAULT (datetime('now','localtime'))`
  - `Uuid: Text` -> `TEXT NOT NULL`
  - `Major: N` -> `INTEGER NOT NULL`
  - `Minor: Z` -> `INTEGER NOT NULL`
  - `TxPower: Z` -> `INTEGER NOT NULL`
  - `NombreEmisora: Text` -> `TEXT NOT NULL`

## 4. Comentarios
- Incluye en la cabecera del archivo SQL un bloque de comentario (`/* ... */`) con la agregación `Medida = (...)` en nuestra notación oficial.
- Añade comentarios breves (`--`) junto a cada columna indicando su tipo lógico (`N`, `Z`, `Text`) y su propósito.

## 5. Tests automáticos
- Genera un script automático `testBD.php` ejecutable por consola que:
  1. Cree la tabla ejecutando el SQL.
  2. Inserte un registro de prueba (`Uuid="EPSG-GTI-PROY-3A"`, `Major=2817`, `Minor=235`, `TxPower=-53`, `NombreEmisora="GTI-Jose"`).
  3. Compruebe automáticamente con condicionales `if` que el `ID` autogenerado es mayor que 0 y que `FechaLectura` se ha rellenado automáticamente, mostrando por consola `"TEST BD OK"` o `"TEST BD ERROR"`.