# btle_frontend_business_logic_design

Componente: **lógica de negocio de cliente (Proxy)** en Android que encapsula la deduplicación y el transporte HTTP REST hacia el backend.

Código correspondiente: `src/btle_frontend_business_logic/`
Ficheros: `com/example/jmmarter/btle_definitivo_jose/{LogicaCliente, ServicioEscucharBeacons, PeticionarioREST}.java`

---

## 1. Component Design

### 1.1 Interfaz de la Lógica del Cliente (Proxy)
El cliente móvil expone llamadas con paridad de firma respecto a la lógica del servidor (`web_business_logic_design.md`) para que la GUI (`MainActivity`) las consuma sin preocuparse del transporte HTTP:

```text
uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
```

### 1.2 Clases y Encapsulación del Transporte

```text
                    --------- LogicaCliente ---------
                    |
                    | elServicio: ServicioEscucharBeacons
                    |
                    |
                    --> LogicaCliente() -->
                    |
                    |
  uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
                    --> guardarMedida() --> B
                    |
                    ---------------------------------
```

```text
                  --------- ServicioEscucharBeacons ---------
                  |
                  | ETIQUETA_LOG: Text
                  | tiempoDeEspera: N
                  | seguir: B
                  | primeraMedida: B
                  | ultimoMajorEnviado: N
                  | ultimoMinorEnviado: Z
                  |
                  |
          --> ServicioEscucharBeacons() -->
                  |
                  |
          --> parar() -->
                  |
                  |
          --> onDestroy() -->
                  |
                  |
  intent   --> onHandleIntent() -->
                  |
                  |
  uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
          --> enviarMedidaAlServidor() -->
                  |
                  |
  uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
          --> construirJSONMedida() --x
     Text <--
                  |
                  |
          --> probarEnviarMedidaAlServidor() --x
                  |
                  -------------------------------------------
```

```text
                    --------- PeticionarioREST ---------
                    |
                    | url: Text
                    | metodo: Text
                    | cuerpo: Text
                    | comoFue: B
                    | codigo: N
                    | respuesta: Text
                    |
                    |
                    --> PeticionarioREST() -->
                    |
                    |
                B <-- doInBackground() <--
                    |
                    |
  comoFue: B       --> onPostExecute() -->
                    |
                    ------------------------------------
```

---

## 2. Design Clarifications

- **Encapsulación del transporte:** `LogicaCliente` encapsula la invocación a `ServicioEscucharBeacons` y `PeticionarioREST`. La GUI (`MainActivity`) interactúa con `LogicaCliente.guardarMedida()` sin tener dependencias directas de la red.
- **Deduplicación de ráfagas BLE:** `enviarMedidaAlServidor()` descarta la medida cuando coincide con la inmediatamente anterior en `major` y `minor`, evitando enviar ráfagas repetidas de un mismo anuncio iBeacon.
- **Configuración del servidor:** `URL_BASE_SERVIDOR` define el endpoint HTTP hacia `GuardarMedida.php`.

---

## 3. General Rules

- **Programming Language:** Java 11
- **Function/Method Headers:** Each function or method header must include its logical design inside a dedicated comment block bounded by dashed lines (`--------------------`).
- **Code Readability:** Code must be as clear and self-explanatory as possible, requiring minimal to no additional inline comments.
- **Automated Testing:** Unit/integration test cases must be generated for all critical methods and functions.

