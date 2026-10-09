# web_frontend_business_logic_design

## 1. Component Design

### 1.1 Lgica del Cliente (Proxy)
El cliente expone llamadas equivalentes a la lgica de servidor para que la GUI las consuma sin preocuparse de `fetch()` ni del transporte HTTP.

```text
uuid (opcional): Text, major: N, minor: Z, txPower (opcional): Z, nombreEmisora (opcional): Text --> guardarMedida() --> B
cuantasComoMaximo: N --> obtenerMedidas() <-- [ Medida ]
```

## 2. Design Clarifications

- Esta capa encapsula por completo el transporte (HTTP requests). La vista (GUI) no debe usar objetos de red.

## 3. General Rules

- **Programming Language:** PHP 8 for backend components, JavaScript for the frontend.
- **Function/Method Headers:** Each function or method header must include its logical design inside a dedicated comment block bounded by dashed lines (`--------------------`).
- **Code Readability:** Code must be as clear and self-explanatory as possible, requiring minimal to no additional inline comments.
- **Automated Testing:** Unit/integration test cases must be generated for all critical methods and functions.
