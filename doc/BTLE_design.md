# BTLE_design

Componente: **aplicación Android** que escanea beacons BLE iBeacon, los desempaqueta y publica
cada medición nueva en el servidor REST.

Código correspondiente: `src/BTLE/`
Ficheros: `app/src/main/java/com/example/jmmarter/btle_definitivo_jose/{MainActivity, ServicioEscucharBeacons, PeticionarioREST, TramaIBeacon, Utilidades}.java`

Estructura: es un proyecto de Android Studio completo y se abre directamente la carpeta
`src/BTLE/`. Contiene `settings.gradle.kts`, `build.gradle.kts`, el catálogo `gradle/libs.versions.toml`,
el *wrapper* y el módulo `app/` con `build.gradle.kts`, `AndroidManifest.xml`, `res/` y el código
en la disposición estándar `app/src/{main,test,androidTest}/java`.

El paquete Java sigue siendo `com.example.jmmarter.btle_definitivo_jose`, igual que el
`namespace` y el `applicationId` de Gradle. No se renombra porque Android exige que las tres
coincidan, y cambiarlas obligaría a reinstallar la aplicación en el móvil.

---

## 1. Diseño del Componente

### 1.1 Tipos fundamentales

| Símbolo | Tipo lógico | Representación en el código |
| :--- | :--- | :--- |
| `N` | Número natural | `int` para índices y contadores, `long` para marcas de tiempo |
| `Z` | Entero con signo | `int`, `long` |
| `B` | Booleano | `boolean` |
| `Text` | Cadena | `String` |
| `[Z]` | Lista de enteros de 1 byte | `byte[]` |

### 1.2 Agregaciones

```text
TramaIBeacon = (
    prefijo: [Z]_9,
    uuid: [Z]_16,
    major: [Z]_2,
    minor: [Z]_2,
    txPower: Z,
    losBytes: [Z],
    advFlags: [Z]_3,
    advHeader: [Z]_2,
    companyID: [Z]_2,
    iBeaconType: Z,
    iBeaconLength: Z
)
```

`MainActivity` no tiene un tipo de datos propio; su estado son referencias a los objetos de
plataforma y a los componentes:

```text
MainActivity = (
    ETIQUETA_LOG: Text,
    ETIQUETA_LOG_REST: Text,
    CODIGO_PETICION_PERMISOS: N,
    NOMBRE_NUESTRO_DISPOSITIVO_BTLE: Text,
    SEGUNDOS_SIN_RESPUESTA_ANTES_DE_REINICIAR: N,
    elEscanner,
    callbackDelEscaneo,
    elIntentDelServicio,
    enviarAlServidorCadaMedida: B,
    servicioParaEnviarAlServidor,
    nombreDelDispositivoQueBuscamos: Text,
    dispositivosAjenosYaAvisados: [ Text ],
    elHandlerDelVigilante,
    ultimoAnuncioRecibido: N,
    laEtiquetaEstado,
    laEtiquetaUltimaMedicion,
    elVigilanteDelEscaneo
)
```

`ServicioEscucharBeacons` mantiene el estado de deduplicación:

```text
ServicioEscucharBeacons = (
    ETIQUETA_LOG: Text,
    tiempoDeEspera: N,
    seguir: B,
    primeraMedida: B,
    ultimoMajorEnviado: N,
    ultimoMinorEnviado: Z
)
```

### 1.3 Composición del anuncio iBeacon

```text
prefijo(9) = advFlags(3) + advHeader(2) + companyID(2) + iBeaconType(1) + iBeaconLength(1)
trama(30)  = prefijo(9) + uuid(16) + major(2) + minor(2) + txPower(1)
```

### 1.4 Utilidades

```text
texto: Text   --> stringToBytes() --> [Z]
uuid: Text    --> stringToUUID() --> UUID
uuid: UUID    --> uuidToString() --> Text
uuid: UUID    --> uuidToHexString() --> Text
bytes: [Z]    --> bytesToString() --> Text
bytes: [Z]    --> bytesToInt() --> Z
bytes: [Z]    --> bytesToIntOK() --> Z
bytes: [Z]    --> bytesToLong() --> Z
bytes: [Z]    --> bytesToHexString() --> Text
ms: Z, ls: Z  --> dosLongToBytes() --> [Z]_16
```

`bytesToInt()` interpreta los bytes **en complemento a dos**, con el byte más significativo primero.
Es lo que hace que la temperatura `-12`, que viaja como `0xFFF4`, vuelva a leerse como `-12`.

### 1.5 Despiece de la trama

```text
bytes: [Z]                --> TramaIBeacon() -->
[Z]_9                     <-- getPrefijo() <--
[Z]_16                    <-- getUUID() <--
[Z]_2                     <-- getMajor() <--
[Z]_2                     <-- getMinor() <--
Z                         <-- getTxPower() <--
[Z]                       <-- getLosBytes() <--
[Z]_3                     <-- getAdvFlags() <--
[Z]_2                     <-- getAdvHeader() <--
[Z]_2                     <-- getCompanyID() <--
Z                         <-- getiBeaconType() <--
Z                         <-- getiBeaconLength() <--
```

El constructor lanza `IllegalArgumentException` si la trama tiene menos de 30 bytes o si los
bytes no cumplen la forma de un iBeacon. `MainActivity` captura esa excepción para que un
anuncio BLE cualquiera no tumbe la aplicación.

### 1.6 Cliente REST

`PeticionarioREST` es una tarea asíncrona parametrizada por la petición:

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
                    ------------------------------
```

`doInBackground()` devuelve `B` indicando si la petición llegó a completarse, y deja el código
HTTP y el cuerpo en `codigo` y `respuesta` para que los lea `onPostExecute()`.

### 1.7 Envío de medidas y deduplicación

```text
uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
                --> enviarMedidaAlServidor() -->

uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text
                --> construirJSONMedida() --> Text

                              --> probarEnviarMedidaAlServidor() --x
```

`enviarMedidaAlServidor()` descarta la medida cuando coincide con la anterior en `major` y
`minor`. La comparación es contra **la última medida enviada**, no contra un histórico: el
filtro evita ráfagas de un mismo anuncio repetido, no repeticiones acumuladas.

### 1.8 Encadenamiento principal

```text
botonNuestroDispositivoPulsado() -->
        arrancarElServicio() --> startService() -->
        buscarEsteDispositivoBTLE( dispositivoBuscado: Text ) -->

callbackDelEscaneo.onScanResult() -->
        mostrarInformacionDispositivoBTLE( resultado ) -->
                --> enviarLaMedidaAlServidor() -->
                        --> enviarMedidaAlServidor() -->

botonPruebaPOSTPulsado()  --> probarEnviarPOST() -->
botonPruebaGETPulsado()   --> probarEnviarGET() -->
botonPruebaGET2Pulsado()  --> probarEnviarGET_Otra() -->
```

### 1.9 Clases

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
  uuid, major, minor, txPower, nombreEmisora
          --> enviarMedidaAlServidor() -->
                  |
                  |
  uuid, major, minor, txPower, nombreEmisora
          --> construirJSONMedida() --x
                  |
                  |
          --> probarEnviarMedidaAlServidor() --x
                  |
                  ------------------------------
```

`MainActivity` es una Activity cuyos métodos son todos manejadores de `android:onClick`, el
ciclo de vida de la Activity y el filtrado de resultados de escaneo. Su caja de estado es la
declarada en 1.2.

---

## 2. Aclaraciones del Diseño

- **El filtro por nombre se hace en código, no con `ScanFilter`.** Antes se armaba un
  `ScanFilter` con `setDeviceName("GTI-3A-Jose")` y se pasaba a `startScan()`. El resultado medido
  fue que el móvil **no recibía nada**: el escáner arrancaba correctamente
  (`onScannerRegistered() - status=0`) y no entraba ni un solo `onScanResult()` en 14 segundos.
  El problema es que Android solo puede comparar ese filtro con el nombre que venga en el anuncio
  o en la respuesta de escaneo; si la placa lo emite en otro sitio o con otro formato, el filtro
  descarta todos los resultados aunque la placa esté a dos metros. Ahora se escanea sin filtros
  (`startScan(null, settings, callback)`) y el nombre se compara en
  `mostrarInformacionDispositivoBTLE()` con `getName()`, que devuelve el valor real.

- **Consecuencia de quitar el filtro: hay que silenciar el ruido.** Al llegar todo lo que hay
  alrededor, `dispositivosAjenosYaAvisados` recuerda las direcciones ya anotadas y cada
  dispositivo ajeno se avisa **una sola vez por sesión de escaneo**. Sin eso el Logcat se llena de
  miles de líneas y no se ve ni el beacon propio.

- **Hay un vigilante del escaneo.** El Bluetooth del móvil puede dejar de escanear sin dar ningún
  error: no llega ni `onScanFailed()` ni `onScanResult()`, y la app se queda esperando para
  siempre creyendo que escanea. `elVigilanteDelEscaneo` comprueba cada
  `SEGUNDOS_SIN_RESPUESTA_ANTES_DE_REINICIAR` segundos si ha entrado algún anuncio y, si no,
  reinicia el escaneo llamando a `buscarEsteDispositivoBTLE()`.

- **`enviarAlServidorCadaMedida` separa los dos modos de escaneo.** El botón de buscar el
  dispositivo propio la pone a `true` (cada iBeacon propio se manda al servidor); el de buscar
  todos los dispositivos la deja en `false`, porque ese botón es solo de diagnóstico y no debe
  publicar medidas de beacons ajenos.

- **`servicioParaEnviarAlServidor()` no arranca el servicio.** Es una instancia creada
  únicamente para poder llamar a `enviarMedidaAlServidor()`. El servicio de verdad lo arranca el
  `Intent` del botón. Es el mismo mecanismo que usa el test automático del botón de verificación.

- **`las medidas que fallan al enviarse no se reintentan.** El filtro actual marca la medida como
  última antes de saber si el POST ha funcionado, así que si la petición falla, ese mismo valor no
  vuelve a intentarse hasta que llegue otro distinto.

- **El escaneo sigue ligados al ciclo de vida de la Activity.** `onDestroy()` detiene el escaneo
  y el servicio. Para que el escaneo sobreviva a la app en segundo plano de verdad haría falta un
  servicio en primer plano con el escaneo dentro, lo que además exigiría los permisos
  `BLUETOOTH_SCAN` en el manifiesto. Hoy el componente cumple "arranca el servicio" pero el
  escaneo no es autónomo.

- **La URL del servidor está fijada en el código**
  (`http://192.168.1.137:8080/rest/`). Con datos móviles no se alcanza una IP privada; la forma
  fiable de probar es un punto de acceso wireless del propio móvil con el PC conectado, o ambos
  equipos en la misma red local.

---

## 3. Reglas Generales

- **Lenguaje de programación objetivo:** Java 11 sobre Android (API mínima 24, `compileSdk` 36),
  con el SDK de AndroidX. La interfaz se declara en XML (`activity_main.xml`). No se usa ninguna
  biblioteca de terceros más allá de AndroidX AppCompat, Material y JUnit.

- **Encabezados de funciones y métodos:** cada declaración debe llevar inmediatamente encima su
  diseño lógico dentro de un bloque de comentario delimitado por líneas discontinuas
  (`--------------------`), con el formato `entradas --> nombre() --> salidas`, seguido de una línea
  "Qué hace". Ese bloque describe el comportamiento observable, no la implementación.

  ```java
  // ------------------------------------------------------------------------------------
  // DISEÑO: texto: Text --> actualizarEstado() -->
  // Qué hace: pone un texto en la etiqueta de estado de la pantalla.
  // ------------------------------------------------------------------------------------
  ```

- **Legibilidad del código:** el código debe ser lo más claro y autoexplicativo posible, de modo
  que prácticamente no requiera comentarios adicionales dentro del cuerpo. Se comentan solo
  tres cosas: el porqué de una decisión no deducible, el sentido de un número mágico y los
  bugs ya corregidos (con su síntoma, para que no se reintroduzcan). Los nombres de los métodos
  van en español y son verbos en imperativo.

- **Pruebas automatizadas:** hay que generar casos de prueba para todas las funciones y métodos
  críticos. Los críticos de este componente son:

  - `TramaIBeacon()`: construir una trama iBeacon válida de 30 bytes y comprobar que `getUUID()`,
    `getMajor()`, `getMinor()` y `getTxPower()` devuelven los bytes esperados; comprobar que una
    trama de menos de 30 bytes lanza `IllegalArgumentException`.
  - `Utilidades.bytesToInt()`: verificar el caso con signo, que es el que falla si se implementa
    mal. En concreto `0xFFF4` debe devolver `-12` y `0x00EA` debe devolver `234`.
  - `Utilidades.bytesToHexString()` y `bytesToString()`: comprobar el formato exacto de la salida.
  - `Utilidades.stringToUUID()`: comprobar que `"EPSG-GTI-PROY-3A"` se convierte al UUID esperado.
  - `ServicioEscucharBeacons.enviarMedidaAlServidor()`: verificar que dos llamadas seguidas con el
    mismo `major` y `minor` lanzan una sola petición, y que una tercera con `minor` distinto sí
    lanza la segunda.
  - `MainActivity.enviarLaMedidaAlServidor()`: comprobar que construye la `TramaIBeacon` a partir
    de un `ScanResult` y que extrae `major` y `minor` con signo.

  La estrategia es doble: (a) tests unitarios JUnit en JVM para `Utilidades` y `TramaIBeacon`, que
  no dependen del dispositivo; (b) tests de instrumentación con `Espresso` para el recorrido de
  la interfaz. Los tests de red, que son los que verifican el envío real, se cubren además con
  las pruebas del lado servidor (`src/web/rest/testServidorREST.php`).

  #### Los tests que hay escritos

  | Fichero | Tipo | Qué comprueba | Pruebas |
  | :--- | :--- | :--- | ---: |
  | `app/src/test/.../UtilidadesTest.java` | JUnit, en el PC | Las 8 conversiones de `Utilidades`, con atención al signo: que `0xFFF3` devuelve -13, que `0x0B01` devuelve 2817 (CO2) y que `0x0C01` devuelve 3073 (temperatura). También los tres rechazos: texto que no mide 16, texto no hexadecimal y más de 4 bytes en `bytesToIntOK()`. | 32 |
  | `app/src/test/.../TramaIBeaconTest.java` | JUnit, en el PC | Que el troceado saca cada campo de su posición exacta, con una trama de 30 bytes montada igual que la que arma la placa. Incluye el contrato entero: `major / 256` da el tipo de medición y el `minor` negativo sale con signo. | 20 |
  | `app/src/androidTest/.../PruebasEnElDispositivo.java` | Instrumentación, **necesita el móvil** | Que los permisos `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` e `INTERNET` estén concedidos, y que el dispositivo tenga hardware BLE. El permiso de escaneo es el que más caro sale: sin él el escáner arranca y no entra ni un `onScanResult()`. | 7 |

  Los 52 tests de JUnit se ejecutan sin móvil con `.\gradlew :app:testDebugUnitTest`. Los de
  instrumentación necesitan el teléfono conectado: `.\gradlew :app:connectedDebugAndroidTest`.

  Se han comprobado rompiendo el código a propósito: si el `minor` se trocea desde la posición
  26 en vez de la 27, fallan 3 pruebas de `TramaIBeaconTest`; y las 52 pasan con el código
  correcto.

  #### Limitación conocida que dej documentada

  `Utilidades.bytesToIntOK()` calcula el signo con un `cast` a `byte`, que solo conserva los 8
  bits bajos. Acierta de -1 a -128, pero de -129 en adelante se equivoca: `-32768` devuelve `0`.
  No afecta al proyecto porque `MainActivity` usa `bytesToInt()`, que interpreta bien todo el
  rango de 16 bits, y las temperaturas del enunciado van de -13 a -11. La prueba
  `bytesToIntOK_limitaElSignoAUnByte()` documenta el comportamiento real, no el deseado.

  #### Lo que no se puede probar automáticamente

  El escaneo BLE en sí. `MainActivity` y `ServicioEscucharBeacons` dependen de Android, así que
  sus pruebas locales no pueden cubrir la llegada de un anuncio de verdad; eso solo se
  comprueba con el móvil en la mano, y para eso están los `Log.d()` con la etiqueta
  `MainActivity` y el watchdog del escaneo, que reinicia el escáner si pasan 6 segundos sin
  entrar ni un anuncio.