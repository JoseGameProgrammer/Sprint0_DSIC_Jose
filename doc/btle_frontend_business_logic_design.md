# btle_frontend_business_logic_design

## 1. Component Design

### 1.1 Lgica del Cliente (Proxy)
El cliente mvil expone llamadas equivalentes a la lgica de servidor para que la GUI las consuma sin preocuparse del transporte HTTP ni del servicio de Bluetooth interno que hace el POST.

```text
uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
```

## 2. Design Clarifications

- Esta capa de lgica encapsula la invocacin del Servicio HTTP (PeticionarioREST). 
- La GUI (`MainActivity`) llama a esta lgica sin tener dependencias directas de la red.

## 3. General Rules

- **Programming Language:** Java 11
- **Function/Method Headers:** Each function or method header must include its logical design inside a dedicated comment block bounded by dashed lines (`--------------------`).
- **Code Readability:** Code must be as clear and self-explanatory as possible, requiring minimal to no additional inline comments.
- **Automated Testing:** Unit/integration test cases must be generated for all critical methods and functions.
