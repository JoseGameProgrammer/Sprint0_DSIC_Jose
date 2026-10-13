# web_communication_design

## 1. Component Design

### 1.1 Endpoints
Los endpoints traducen HTTP a llamadas de la lógica. No contienen SQL.

```text
uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
```

## 2. Design Clarifications

- La separación entre lógica y transporte es estricta. Los endpoints se limitan a traducir de la petición web a tipos abstractos.
- Los endpoints no llevan SQL.

## 3. General Rules

- **Programming Language:** PHP 8 for backend components, JavaScript for the frontend.
- **Function/Method Headers:** Each function or method header must include its logical design inside a dedicated comment block bounded by dashed lines (`--------------------`).
- **Code Readability:** Code must be as clear and self-explanatory as possible, requiring minimal to no additional inline comments.
- **Automated Testing:** Unit/integration test cases must be generated for all critical methods and functions.
