# web_business_logic_design

## 1. Component Design

### 1.1 Tipos fundamentales
| Smbolo | Tipo lgico | Representacin en el cdigo |
| :--- | :--- | :--- |
| `N` | Nmero natural | `int` |
| `Z` | Entero con signo | `int` |
| `B` | Booleano | `bool` |
| `Text` | Cadena | `string` |
| `VoF` | Valor de verdad | `true` / `false` |

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

### 1.3 Lgica del negocio
Funciones puras sin dependencias de HTTP ni de bases de datos de forma directa en su firma.

```text
uuid (opcional): Text, major: N, minor: Z, txPower (opcional): Z, nombreEmisora (opcional): Text --> guardarMedida() --> B
cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
--> conectarBBDD() --x
nombre: Text, password: Text --> hacerLogin() --> VoF
usuario: Text --> diHola() --> ( nombre: Text, saludo: Text )
```

## 2. Design Clarifications

- La capa lgica es 100% independiente del framework web o de transporte.

## 3. General Rules

- **Programming Language:** PHP 8 for backend components, JavaScript for the frontend.
- **Function/Method Headers:** Each function or method header must include its logical design inside a dedicated comment block bounded by dashed lines (`--------------------`).
- **Code Readability:** Code must be as clear and self-explanatory as possible, requiring minimal to no additional inline comments.
- **Automated Testing:** Unit/integration test cases must be generated for all critical methods and functions.
