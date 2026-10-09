# web_communication_design

## 1. Component Design

### 1.1 Endpoints
Los endpoints traducen HTTP a llamadas de la lgica. No contienen SQL.

```text
uuid (opcional): Text, major: N, minor: Z, txPower (opcional): Z, nombreEmisora (opcional): Text --> guardarMedida() --> B        (escribe)

cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
nombre: Text, password: Text --> hacerLogin() --> VoF
usuario: Text --> diHola() --> ( nombre: Text, saludo: Text )
```

## 2. Design Clarifications

- La separacin entre lgica y transporte es estricta. Los endpoints se limitan a traducir de la peticin web a tipos abstractos.
- Los endpoints no llevan SQL.

## 3. General Rules

- **Programming Language:** PHP 8 for backend components, JavaScript for the frontend.
- **Function/Method Headers:** Each function or method header must include its logical design inside a dedicated comment block bounded by dashed lines (`--------------------`).
- **Code Readability:** Code must be as clear and self-explanatory as possible, requiring minimal to no additional inline comments.
- **Automated Testing:** Unit/integration test cases must be generated for all critical methods and functions.
