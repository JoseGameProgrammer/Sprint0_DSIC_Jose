# web_business_logic_design

## 1. Component Design

### 1.1 Tipos fundamentales
| Símbolo | Tipo lógico | Representación en el código |
| :--- | :--- | :--- |
| `N` | Número natural | `int` |
| `Z` | Entero con signo | `int` |
| `B` | Booleano | `bool` |
| `Text` | Cadena | `string` |

### 1.2 Agregación principal y Alineación con Base de Datos
La entidad `Medida` se mapea directamente con el esquema relacional de la tabla `Medidas` definido en `web_database_design.md`:

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

- Mapeo con `web_database_design.md`:
  - `id` $\rightarrow$ Columna `ID` (`INTEGER PRIMARY KEY`)
  - `fecha` $\rightarrow$ Columna `Fecha` (`TEXT`)
  - `idSensor` $\rightarrow$ Columna `ID_Sensor` (`INTEGER`)
  - `tipoMedicion` $\rightarrow$ Columna `Tipo_Medicion` (`TEXT`)
  - `valorMedicion` $\rightarrow$ Columna `Valor_Medicion` (`INTEGER`)

### 1.3 Lógica del negocio
Funciones puras sin dependencias de HTTP ni de protocolos de transporte en sus firmas.

```text
uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
--> conectarBBDD() --x
```

## 2. Design Clarifications

- La capa lógica es 100% independiente del framework web o de transporte.
- Las operaciones de persistencia interactúan exclusivamente con la base de datos SQLite y la tabla `Medidas` definida en `web_database_design.md`.

## 3. General Rules

- **Programming Language:** PHP 8 for backend components, JavaScript for the frontend.
- **Function/Method Headers:** Each function or method header must include its logical design inside a dedicated comment block bounded by dashed lines (`--------------------`).
- **Code Readability:** Code must be as clear and self-explanatory as possible, requiring minimal to no additional inline comments.
- **Automated Testing:** Unit/integration test cases must be generated for all critical methods and functions.
