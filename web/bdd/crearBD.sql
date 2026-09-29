/* =================================================================================================
   DISEÑO LÓGICO: BBDD - Tabla Medidas
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
   POR QUÉ ESTA TABLA CUMPLE LA 1FN, LA 2FN Y LA 3FN:

   1FN (valores atómicos)
       Cada columna guarda UN solo valor, sin listas ni grupos repetidos. No hay
       columnas tipo "varias medidas" ni "nombres separados por comas". Por ejemplo
       Major es UN número, no una lista de muestras.

   2FN (dependencia completa de la clave primaria)
       La única clave primaria es ID, que es UNA sola columna, así que la 2FN se
       cumple por definición (no puede haber dependencia parcial de una clave
       compuesta). Además, ID no depende de nada: es un id artificial.

   3FN (sin dependencias transitivas)
       Ninguna columna depende de otra columna que no sea la clave primaria. Uuid,
       Major, Minor, TxPower y NombreEmisora son características propias de la
       medida y NO se deducen unas de otras. Por ejemplo, Major NO depende de Uuid:
       dos emisoras distintas pueden mandar el mismo Major.

   -------------------------------------------------------------------------------------------------
   MAPEO DE LOS TIPOS DE LA NOTACIÓN FORMAL A SQL:

     ID: N               ->  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
     FechaLectura: Text  ->  TEXT NOT NULL DEFAULT (datetime('now','localtime'))
     Uuid: Text          ->  TEXT NOT NULL
     Major: N            ->  INTEGER NOT NULL
     Minor: Z            ->  INTEGER NOT NULL
     TxPower: Z          ->  INTEGER NOT NULL
     NombreEmisora: Text ->  TEXT NOT NULL

   OJO con la diferencia entre N y Z:
     - N (natural, sin signo) se guarda en INTEGER. SQLite no tiene enteros sin
       signo de verdad, pero con PRIMARY KEY AUTOINCREMENT y con los valores que
       usamos (0..65535) no hay problema.
     - Z (entero CON signo) se guarda también en INTEGER, y aquí sí hace falta el
       signo: Minor transporta el valor del sensor (puede ser negativo, por ejemplo
       -12 grados) y TxPower es un dBm, casi siempre negativo (por ejemplo -53).
       Por eso Minor y TxPower son Z y no N.

   -------------------------------------------------------------------------------------------------
   CÓMO SE USA ESTE ARCHIVO
   Se ejecuta sobre una base de datos SQLite VACÍA (o ya creada): la tabla se crea
   con CREATE TABLE IF NOT EXISTS, así que se puede ejecutar tantas veces como haga
   falta sin quejarse de que ya existe.
   =================================================================================================
*/

-- Se crea la tabla Medidas. IF NOT EXISTS para que sea re-ejecutable.
CREATE TABLE IF NOT EXISTS Medidas (

    -- ID: N -> N (número natural, sin signo)
    -- Clave primaria autoincremental: la genera la propia base de datos al
    -- insertar, y es lo que identifica de forma única cada fila.
    ID            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, -- ID: N, PK, autoincremental

    -- FechaLectura: Text -> Text
    -- Fecha y hora (AAAA-MM-DD HH:MM:SS) del momento en que el servidor guarda la
    -- medición. El servidor no la manda: se rellena sola con la hora local, así
    -- que es imposible que se guarde una medida "sin fecha".
    FechaLectura  TEXT NOT NULL DEFAULT (datetime('now','localtime')), -- FechaLectura: Text, automática

    -- Uuid: Text -> Text
    -- Identificador de 16 bytes de la placa emisora, en hexadecimal. Sirve para
    -- saber QUÉ beacon es el que se ha medido y para filtrar los anuncios.
    Uuid          TEXT NOT NULL, -- Uuid: Text, id del beacon

    -- Major: N -> N (número natural, sin signo)
    -- Campo de 2 bytes del iBeacon. El byte ALTO es el tipo de medición y el byte
    -- BAJO es el número de muestra. Ejemplo: 2817 = 11*256 + 1 -> tipo 11 (CO2),
    -- muestra 1.
    Major         INTEGER NOT NULL, -- Major: N, tipo de medida (alto) + muestra (bajo)

    -- Minor: Z -> Z (entero CON signo)
    -- Campo de 2 bytes del iBeacon que transporta el valor REAL medido por el
    -- sensor. Lleva signo porque puede ser negativo (por ejemplo -12 grados).
    Minor         INTEGER NOT NULL, -- Minor: Z, valor medido (con signo)

    -- TxPower: Z -> Z (entero CON signo)
    -- Potencia de referencia de la señal en dBm, 1 byte con signo. Siempre es
    -- negativa (0 dBm es el máximo; valores típicos: -53, -59, -70).
    TxPower       INTEGER NOT NULL, -- TxPower: Z, potencia en dBm (con signo)

    -- NombreEmisora: Text -> Text
    -- Nombre Bluetooth con el que se anuncia la placa física (p.ej. "GTI-Jose").
    -- Permite distinguir dos emisoras que usen el mismo Uuid.
    NombreEmisora TEXT NOT NULL     -- NombreEmisora: Text, nombre bluetooth

); -- CREATE TABLE Medidas
