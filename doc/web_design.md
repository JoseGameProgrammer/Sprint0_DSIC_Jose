# web_design

Componente: **capa web** del proyecto. Expone las medidas por REST, las guarda en la base de
datos y ofrece una vista de usuario que las consulta y refresca automǭticamente.

Cdigo correspondiente: `src/web/`
Ficheros: `rest/{GuardarMedida, ObtenerMedidas, diHola, hacerLogin}.php`,
`logica/{LogicaMedidas, diHola, hacerLogin}.php`,
`bdd/{crearBD.sql, VerTabla.php}`, `ux/{index.html, LogicaFake.js}`
Pruebas: `rest/testServidorREST.php`, `logica/testLogicaMedidas.php`, `bdd/testBD.php`,
`ux/{testUX.js, testLogicaFake.js}`

---

## 1. Diseo del Componente

### 1.1 Tipos fundamentales

| Smbolo | Tipo lgico | Representacin en el cdigo |
| :--- | :--- | :--- |
| `N` | Nǧmero natural | `int` |
| `Z` | Entero con signo | `int` |
| `B` | Booleano | `bool` |
| `Text` | Cadena | `string` |
| `VoF` | Valor de verdad | `true` / `false` (PHP) y `true` / `false` (JavaScript) |

### 1.2 Agregacin principal

```text
Medida = (
    id: N,
    fecha: Text,
    idSensor: N,
    tipoMedicion: Text,
    valorMedicion: Z
)

TablaMedidas = [ Medida ]
```

### 1.3 Interpretacin del campo `major`

El byte alto de `major` es el tipo de medicin y el byte bajo es el nǧmero de muestra:

```text
tipoDeMedicion = major div 256
numeroDeMuestra = major mod 256

    2817 = 11 * 256 + 1  ->  tipo 11 = CO2,       muestra 1
    3073 = 12 * 256 + 1  ->  tipo 12 = TEMPERATURA, muestra 1
```

En PHP la divisin entera es `intdiv(major, 256)`; el valor con signo de `minor` se divide igual
con `intdiv((int) minor, 256)` para el caso de las temperaturas negativas.

### 1.4 Capa de lgica del negocio

Funciones puras: reciben parǭmetros normales y devuelven datos normales. No saben nada de HTTP,
de sesiones, de `$_POST` ni de `json_encode()`.

```text
uuid (opcional): Text, major: N, minor: Z, txPower (opcional): Z, nombreEmisora (opcional): Text --> guardarMedida() --> B

cuantasComoMaximo: N
                --> obtenerMedidas() <-- [ Medida ]

              --> conectarBBDD() --x

nombre: Text, password: Text --> hacerLogin() --> VoF

usuario: Text   --> diHola() --> ( nombre: Text, saludo: Text )
```

### 1.5 Capa REST (endpoints)

Los endpoints traducen HTTP a llamadas de la lgica. **No contienen SQL**: toda la persistencia
estǭ en la capa de lgica, y los tests lo verifican explcitamente.

```text
uuid (opcional): Text, major: N, minor: Z, txPower (opcional): Z, nombreEmisora (opcional): Text --> guardarMedida() --> B        (escribe)

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

La capa web es funcional: no define clases. El ǧnico tipo compuesto es `Medida`.

### 1.8 Diseo de la base de datos

Formato conforme a `context/Database_Design_Spec.md`. La tabla estǭ definida en
`src/web/bdd/crearBD.sql` y la base de datos es el fichero `src/web/bdd/bdd.sqlite`.

```text
====================================================================================
TABLE: Medidas

DESCRIPTION: Almacena una fila por cada medicin de un beacon iBeacon recibida por el
servidor. El valor medido viaja en el campo minor, con signo, y el tipo de magnitud y el
nǧmero de muestra viajan codificados en el campo major.

COLUMNS:

+ ID             | INTEGER | NOT NULL | Auto-Increment
+ Fecha          | TEXT    | NOT NULL | (datetime('now','localtime'))
+ ID_Sensor      | INTEGER | NOT NULL | 1
+ Tipo_Medicion  | TEXT    | NOT NULL | 
+ Valor_Medicion | INTEGER | NOT NULL | 

PRIMARY KEY: ID

FOREIGN KEYS:

+ (ninguna)

CONSTRAINTS:

+ ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
+ Fecha TEXT NOT NULL DEFAULT (datetime('now','localtime'))
+ ID_Sensor INTEGER NOT NULL
+ Tipo_Medicion TEXT NOT NULL
+ Valor_Medicion INTEGER NOT NULL
====================================================================================
```

Las restricciones de rango de `Major` y `Minor`
**no** estǭn declaradas en la tabla: las valida la capa de aplicacin, que es quien decide si una
medida recibida es admisible. La tabla solo declara la clave primaria y la obligatoriedad de sus
columnas.

Mapeo de la notacin formal a los tipos SQL:

| Tipo lgico | Columna | Tipo SQL | Motivo |
| :--- | :--- | :--- | :--- |
| `N` | `ID` | `INTEGER PRIMARY KEY AUTOINCREMENT` | Natural, generado por la base de datos |
| `Text` | `Fecha` | `TEXT` | Fecha y hora `AAAA-MM-DD HH:MM:SS` |
| `N` | `ID_Sensor` | `INTEGER` | Identificador ǧnico del sensor (por defecto 1) |
| `Text` | `Tipo_Medicion` | `TEXT` | Tipo de medicin (ej. "CO2", "TEMPERATURA") |
| `Z` | `Valor_Medicion` | `INTEGER` | Valor de la medicin |

El script usa `CREATE TABLE IF NOT EXISTS`, as que se puede ejecutar tantas veces como haga falta
sobre una base de datos vaca o ya creada.

---

## 2. Aclaraciones del Diseo

- **La separacin entre lgica y transporte es lo importante del diseo.** Si maana cambia el
  transporte (de REST a GraphQL, o a una cola de mensajes), `LogicaMedidas.php` no se toca,
  porque no sabe quǸ transporte hay. Los endpoints se limitan a traducir de la peticin web a
  estos tipos lgicos, y de la respuesta a JSON.

- **Los endpoints no llevan SQL.** Hay un test que lo comprueba leyendo el cdigo fuente de
  `GuardarMedida.php` y `ObtenerMedidas.php` y verificando que no aparecen las palabras `SELECT`,
  `INSERT`, `sqlite:` ni `require_once '...LogicaMedidas.php'` fuera del `require`.

- **El servidor debe servir la carpeta `web/` completa, no solo `web/rest/`.** Los endpoints
  hacen `require_once '../logica/LogicaMedidas.php'`, que es una ruta relativa a la carpeta en la
  que se sirve. Si el servidor solo expone `web/rest/`, el `include` no se encuentra y el endpoint
  falla.

- **`Medida` no tiene una clase ni una tabla de tipos.** Se representa como array asociativo en
  PHP y como objeto literal en JavaScript. El `id` lo genera la base de datos y no lo manda
  nadie.

- **`fecha` la pone la base de datos**, no el cliente, mediante el valor por defecto
  `datetime('now','localtime')`. As es imposible guardar una medida sin fecha.

- **`cuantasComoMaximo` se valida antes de tocar la base de datos.** El endpoint devuelve HTTP 200
  con cero medidas ante valores absurdos (negativos, decimales o mayores que el rango de un entero
  de 64 bits) en lugar de propagar el error, para que un cliente malicioso no pueda tumbar el
  servicio.

- **La lgica de usuario trabaja contra una implementacin falsa.** `LogicaFake.js` sustituye a
  la llamada REST por datos inventados, y `testLogicaFake.js` verifica que la vista los pinta
  bien. As la interfaz se puede probar sin depender del servidor ni del mvil.

- **`avisarUnaVez()` no repite el mismo aviso.** Evita que un fallo de conexin persistente llene
  la pantalla de mensajes idǸnticos.

- **Las rutas de la base de datos se resuelven con `__DIR__`**, de forma que la carpeta `web/` se
  puede mover sin romper nada. Todas las rutas son relativas a la ubicacin del propio fichero.

- **La base de datos es un fichero SQLite.** No hace falta ningǧn servidor de base de datos ni
  Apache: con el intǸrprete de PHP basta. El script `bdd/VerTabla.php` permite volcar el
  contenido tanto por consola como por navegador.

---

## 3. Reglas Generales

- **Lenguaje de programacin objetivo:** PHP 8 para el servidor (lgica de negocio y endpoints
  REST) y JavaScript sin ningǧn framework ni dependencia externa para el cliente. La base de
  datos es SQLite, con un ǧnico fichero. Las rutas de prueba (`php -S`) se levantan con el
  intǸrprete de PHP, sin Apache.

- **Encabezados de funciones y mǸtodos:** cada declaracin debe llevar inmediatamente encima su
  diseo lgico dentro de un bloque de comentario delimitado por lneas discontinuas
  (`--------------------`), con el formato `entradas --> nombre() --> salidas`, seguido de una lnea
  "QuǸ hace". Ese bloque describe el comportamiento observable, no la implementacin.

  ```php
  // ------------------------------------------------------------------------------------
  // DISE'O: cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
  // QuǸ hace: devuelve como mucho las medidas mǭs recientes que se le pidan.
  // ------------------------------------------------------------------------------------
  ```

- **Legibilidad del cdigo:** el cdigo debe ser lo mǭs claro y autoexplicativo posible, de modo
  que prǭcticamente no requiera comentarios adicionales dentro del cuerpo. Se comentan solo el
  porquǸ de las decisiones no deducibles, el sentido de los nǧmeros mǭgicos y los bugs ya
  corregidos con su sntoma. Las funciones de la capa de lgica llevan nombres en espaol y sin
  abreviaturas.

- **Pruebas automatizadas:** hay que generar casos de prueba para todas las funciones y mǸtodos
  crticos. Los crticos de este componente son:

  - `guardarMedida()`: comprobar que inserta una fila y devuelve `true`; comprobar que una medida
    con `minor` negativo se guarda y se recupera con el signo intacto.
  - `obtenerMedidas()`: comprobar que `cuantasComoMaximo` limita el nǧmero de filas y que
    devuelve las mǭs recientes en orden descendente.
  - `conectarBBDD()`: comprobar que crea el esquema si la base de datos estǭ vaca.
  - `obtenerMedidasFake()` y las funciones de pintado: comprobar que se construye una fila por
    medida y que la ǧltima medida se muestra en su sitio.
  - Los endpoints: comprobar el cdigo HTTP, el formato del JSON de respuesta y el campo `error`.
  - La robustez: comprobar que los valores lmite de `cuantasComoMaximo` no producen error.

  Los tres scripts existentes cubren esa superficie: `bdd/testBD.php` para el esquema y la
  persistencia, `logica/testLogicaMedidas.php` para la lgica de negocio y
  `rest/testServidorREST.php` para los endpoints, que ademǭs levanta y para su propio servidor de
  pruebas y limpia al terminar las filas que ha creado. La vista de usuario se comprueba con
  `ux/testLogicaFake.js` y `ux/testUX.js`.