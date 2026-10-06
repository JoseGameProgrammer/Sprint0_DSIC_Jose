# web_design

Componente: **capa web** del proyecto. Expone las medidas por REST, las guarda en la base de
datos y ofrece una vista de usuario que las consulta y refresca automáticamente.

Código correspondiente: `src/web/`
Ficheros: `rest/{GuardarMedida, ObtenerMedidas, diHola, hacerLogin}.php`,
`logica/{LogicaMedidas, diHola, hacerLogin}.php`,
`bdd/{crearBD.sql, VerTabla.php}`, `ux/{index.html, LogicaFake.js}`
Pruebas: `rest/testServidorREST.php`, `logica/testLogicaMedidas.php`, `bdd/testBD.php`,
`ux/{testUX.js, testLogicaFake.js}`

---

## 1. Diseño del Componente

### 1.1 Tipos fundamentales

| Símbolo | Tipo lógico | Representación en el código |
| :--- | :--- | :--- |
| `N` | Número natural | `int` |
| `Z` | Entero con signo | `int` |
| `B` | Booleano | `bool` |
| `Text` | Cadena | `string` |
| `VoF` | Valor de verdad | `true` / `false` (PHP) y `true` / `false` (JavaScript) |

### 1.2 Agregación principal

```text
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
```

### 1.3 Interpretación del campo `major`

El byte alto de `major` es el tipo de medición y el byte bajo es el número de muestra:

```text
tipoDeMedicion = major div 256
numeroDeMuestra = major mod 256

    2817 = 11 * 256 + 1  ->  tipo 11 = CO2,       muestra 1
    3073 = 12 * 256 + 1  ->  tipo 12 = TEMPERATURA, muestra 1
```

En PHP la división entera es `intdiv(major, 256)`; el valor con signo de `minor` se divide igual
con `intdiv((int) minor, 256)` para el caso de las temperaturas negativas.

### 1.4 Capa de lógica del negocio

Funciones puras: reciben parámetros normales y devuelven datos normales. No saben nada de HTTP,
de sesiones, de `$_POST` ni de `json_encode()`.

```text
uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
                --> guardarMedida() --> B

cuantasComoMaximo: N
                --> obtenerMedidas() <-- [ Medida ]

              --> conectarBBDD() --x

nombre: Text, password: Text --> hacerLogin() --> VoF

usuario: Text   --> diHola() --> ( nombre: Text, saludo: Text )
```

### 1.5 Capa REST (endpoints)

Los endpoints traducen HTTP a llamadas de la lógica. **No contienen SQL**: toda la persistencia
está en la capa de lógica, y los tests lo verifican explícitamente.

```text
uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
                --> guardarMedida() --> B        (escribe)

cuantasComoMaximo: N
                --> obtenerMedidas() <-- [ Medida ]

nombre: Text, password: Text --> hacerLogin() --> VoF

usuario: Text                 --> diHola() --> ( nombre: Text, saludo: Text )
```

### 1.6 Capa de usuario (JavaScript)

```text
unaMedida: Medida     --> pintarLaUltimaMedida() -->
lasMedidas: [ Medida ] --> pintarLasMedidasAnteriores() -->
unaMedida: Medida     --> filaDeMedida() --> [ Text ]
unValor: Text         --> celdaDe() --> [ Text ]
unTexto: Text         --> filaDe() --> [ Text ]
unMensaje: Text       --> avisarUnaVez() -->
                      --> refrescarMedidas() -->
                      --> iniciarRefresco() -->
                      --> detenerRefresco() -->
                      --> obtenerMedidasFake() <--
```

### 1.7 Clases

La capa web es funcional: no define clases. El único tipo compuesto es `Medida`.

### 1.8 Diseño de la base de datos

Formato conforme a `context/Database_Design_Spec.md`. La tabla está definida en
`src/web/bdd/crearBD.sql` y la base de datos es el fichero `src/web/bdd/bdd.sqlite`.

```text
====================================================================================
TABLE: Medidas

DESCRIPTION: Almacena una fila por cada medición de un beacon iBeacon recibida por el
servidor. El valor medido viaja en el campo minor, con signo, y el tipo de magnitud y el
número de muestra viajan codificados en el campo major.

COLUMNS:

+ ID            | INTEGER | NOT NULL | Auto-Increment
+ FechaLectura  | TEXT    | NOT NULL | (datetime('now','localtime'))
+ Uuid          | TEXT    | NOT NULL |
+ Major         | INTEGER | NOT NULL |
+ Minor         | INTEGER | NOT NULL |
+ TxPower       | INTEGER | NOT NULL |
+ NombreEmisora | TEXT    | NOT NULL |

PRIMARY KEY: ID

FOREIGN KEYS:

+ (ninguna)

CONSTRAINTS:

+ ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
+ FechaLectura TEXT NOT NULL DEFAULT (datetime('now','localtime'))
+ Uuid TEXT NOT NULL
+ Major INTEGER NOT NULL
+ Minor INTEGER NOT NULL
+ TxPower INTEGER NOT NULL
+ NombreEmisora TEXT NOT NULL
====================================================================================
```

Las restricciones de rango de `Major` (0–65535), `Minor` (−32768–32767) y `TxPower` (−128–127)
**no** están declaradas en la tabla: las valida la capa de aplicación, que es quien decide si una
medida recibida es admisible. La tabla solo declara la clave primaria y la obligatoriedad de sus
columnas.

Mapeo de la notación formal a los tipos SQL:

| Tipo lógico | Columna | Tipo SQL | Motivo |
| :--- | :--- | :--- | :--- |
| `N` | `ID` | `INTEGER PRIMARY KEY AUTOINCREMENT` | Natural, generado por la base de datos |
| `Text` | `FechaLectura` | `TEXT` | Fecha y hora `AAAA-MM-DD HH:MM:SS` |
| `Text` | `Uuid` | `TEXT` | Identificador de la baliza |
| `N` | `Major` | `INTEGER` | Campo de 2 bytes sin signo, rango 0–65535 |
| `Z` | `Minor` | `INTEGER` | Campo de 2 bytes **con** signo, puede ser negativo |
| `Z` | `TxPower` | `INTEGER` | dBm, siempre negativo |
| `Text` | `NombreEmisora` | `TEXT` | Nombre Bluetooth de la placa |

El script usa `CREATE TABLE IF NOT EXISTS`, así que se puede ejecutar tantas veces como haga falta
sobre una base de datos vacía o ya creada.

---

## 2. Aclaraciones del Diseño

- **La separación entre lógica y transporte es lo importante del diseño.** Si mañana cambia el
  transporte (de REST a GraphQL, o a una cola de mensajes), `LogicaMedidas.php` no se toca,
  porque no sabe qué transporte hay. Los endpoints se limitan a traducir de la petición web a
  estos tipos lógicos, y de la respuesta a JSON.

- **Los endpoints no llevan SQL.** Hay un test que lo comprueba leyendo el código fuente de
  `GuardarMedida.php` y `ObtenerMedidas.php` y verificando que no aparecen las palabras `SELECT`,
  `INSERT`, `sqlite:` ni `require_once '...LogicaMedidas.php'` fuera del `require`.

- **El servidor debe servir la carpeta `web/` completa, no solo `web/rest/`.** Los endpoints
  hacen `require_once '../logica/LogicaMedidas.php'`, que es una ruta relativa a la carpeta en la
  que se sirve. Si el servidor solo expone `web/rest/`, el `include` no se encuentra y el endpoint
  falla.

- **`Medida` no tiene una clase ni una tabla de tipos.** Se representa como array asociativo en
  PHP y como objeto literal en JavaScript. El `id` lo genera la base de datos y no lo manda
  nadie.

- **`fechaLectura` la pone la base de datos**, no el cliente, mediante el valor por defecto
  `datetime('now','localtime')`. Así es imposible guardar una medida sin fecha.

- **`cuantasComoMaximo` se valida antes de tocar la base de datos.** El endpoint devuelve HTTP 200
  con cero medidas ante valores absurdos (negativos, decimales o mayores que el rango de un entero
  de 64 bits) en lugar de propagar el error, para que un cliente malicioso no pueda tumbar el
  servicio.

- **La lógica de usuario trabaja contra una implementación falsa.** `LogicaFake.js` sustituye a
  la llamada REST por datos inventados, y `testLogicaFake.js` verifica que la vista los pinta
  bien. Así la interfaz se puede probar sin depender del servidor ni del móvil.

- **`avisarUnaVez()` no repite el mismo aviso.** Evita que un fallo de conexión persistente llene
  la pantalla de mensajes idénticos.

- **Las rutas de la base de datos se resuelven con `__DIR__`**, de forma que la carpeta `web/` se
  puede mover sin romper nada. Todas las rutas son relativas a la ubicación del propio fichero.

- **La base de datos es un fichero SQLite.** No hace falta ningún servidor de base de datos ni
  Apache: con el intérprete de PHP basta. El script `bdd/VerTabla.php` permite volcar el
  contenido tanto por consola como por navegador.

---

## 3. Reglas Generales

- **Lenguaje de programación objetivo:** PHP 8 para el servidor (lógica de negocio y endpoints
  REST) y JavaScript sin ningún framework ni dependencia externa para el cliente. La base de
  datos es SQLite, con un único fichero. Las rutas de prueba (`php -S`) se levantan con el
  intérprete de PHP, sin Apache.

- **Encabezados de funciones y métodos:** cada declaración debe llevar inmediatamente encima su
  diseño lógico dentro de un bloque de comentario delimitado por líneas discontinuas
  (`--------------------`), con el formato `entradas --> nombre() --> salidas`, seguido de una línea
  "Qué hace". Ese bloque describe el comportamiento observable, no la implementación.

  ```php
  // ------------------------------------------------------------------------------------
  // DISEÑO: cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
  // Qué hace: devuelve como mucho las medidas más recientes que se le pidan.
  // ------------------------------------------------------------------------------------
  ```

- **Legibilidad del código:** el código debe ser lo más claro y autoexplicativo posible, de modo
  que prácticamente no requiera comentarios adicionales dentro del cuerpo. Se comentan solo el
  porqué de las decisiones no deducibles, el sentido de los números mágicos y los bugs ya
  corregidos con su síntoma. Las funciones de la capa de lógica llevan nombres en español y sin
  abreviaturas.

- **Pruebas automatizadas:** hay que generar casos de prueba para todas las funciones y métodos
  críticos. Los críticos de este componente son:

  - `guardarMedida()`: comprobar que inserta una fila y devuelve `true`; comprobar que una medida
    con `minor` negativo se guarda y se recupera con el signo intacto.
  - `obtenerMedidas()`: comprobar que `cuantasComoMaximo` limita el número de filas y que
    devuelve las más recientes en orden descendente.
  - `conectarBBDD()`: comprobar que crea el esquema si la base de datos está vacía.
  - `obtenerMedidasFake()` y las funciones de pintado: comprobar que se construye una fila por
    medida y que la última medida se muestra en su sitio.
  - Los endpoints: comprobar el código HTTP, el formato del JSON de respuesta y el campo `error`.
  - La robustez: comprobar que los valores límite de `cuantasComoMaximo` no producen error.

  Los tres scripts existentes cubren esa superficie: `bdd/testBD.php` para el esquema y la
  persistencia, `logica/testLogicaMedidas.php` para la lógica de negocio y
  `rest/testServidorREST.php` para los endpoints, que además levanta y para su propio servidor de
  pruebas y limpia al terminar las filas que ha creado. La vista de usuario se comprueba con
  `ux/testLogicaFake.js` y `ux/testUX.js`.