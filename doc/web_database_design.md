# web_database_design

## 1. Component Design

```text
====================================================================================
TABLE: Medidas

DESCRIPTION: Almacena una fila por cada medicin recibida por el servidor.
El tipo de magnitud se codifica en el campo Tipo_Medicion y el valor medido
en Valor_Medicion, simplificando el modelo original basado en el estndar iBeacon.

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

## 2. Design Clarifications

- La base de datos es un fichero SQLite.
- Las fechas las genera SQLite por defecto.

## 3. General Rules

- **Programming Language:** PHP 8 for backend components, JavaScript for the frontend.
- **Function/Method Headers:** Each function or method header must include its logical design inside a dedicated comment block bounded by dashed lines (`--------------------`).
- **Code Readability:** Code must be as clear and self-explanatory as possible, requiring minimal to no additional inline comments.
- **Automated Testing:** Unit/integration test cases must be generated for all critical methods and functions.
