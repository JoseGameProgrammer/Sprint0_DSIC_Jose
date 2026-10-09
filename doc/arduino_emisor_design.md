# arduino_emisor_design

Componente: **emisor BLE iBeacon del nRF52840** (placa Adafruit Bluefruit nRF52).

Código correspondiente: `src/arduino_emisor/HolaMundoIBeacon/`
Ficheros: `HolaMundoIBeacon.ino`, `LED.h`, `PuertoSerie.h`, `Medidor.h`, `Publicador.h`, `EmisoraBLE.h`, `ServicioEnEmisora.h`

---

## 1. Component Design

### 1.1 Tipos fundamentales

| Símbolo | Tipo lógico | Representación en el código |
| :--- | :--- | :--- |
| `N` | Número natural | `unsigned int`, `uint8_t`, `uint16_t` |
| `Z` | Entero con signo | `int`, `int16_t` |
| `B` | Booleano | `bool` |
| `Text` | Cadena | `const char *`, `String` |

### 1.2 Enumeración

```text
MedicionesID = { CO2 = 11, TEMPERATURA = 12, RUIDO = 13 }
```

### 1.3 Agregaciones

```text
LED               = ( numero: N, encendido: B )
PuertoSerie       = ( baudios: N )
Medidor           = ()
Publicador        = ( beaconUUID: [Z]_16, laEmisora: EmisoraBLE, RSSI: Z )
EmisoraBLE        = ( nombreEmisora: Text, fabricanteID: N, txPower: Z )
Caracteristica    = ( uuidCaracteristica: [Z]_16, laCaracteristica )
ServicioEnEmisora = ( uuidServicio: [Z]_16, elServicio,
                      lasCaracteristicas: [ Caracteristica ] )

CallbackConexionEstablecida = ( connHandle: N ) -->
CallbackConexionTerminada   = ( connHandle: N, reason: N ) -->
CallbackCaracteristicaEscrita = ( connHandle: N, chr, data: [Z], len: N ) -->
```

### 1.4 Estructura global del programa

El programa se organiza en dos espacios de nombres:

```text
Globales = (
    elLED: LED,            // pin 7
    elPuerto: PuertoSerie, // 115200 baudios
    elMedidor: Medidor,
    elPublicador: Publicador
)

Loop = ( cont: N )
```

Encadenamiento de `setup()`:

```text
--> setup() -->
        elPublicador.encenderEmisora() -->
        elMedidor.iniciarMedidor() -->
```

Encadenamiento de `loop()`:

```text
        --> loop() -->
                elMedidor.medirCO2() <-- Z
                elPublicador.publicarCO2( valor: Z, cont: N, t: N ) -->
                elMedidor.medirTemperatura() <-- Z
                elPublicador.publicarTemperatura( valor: Z, cont: N, t: N ) -->
                esperar( t: N ) -->
                elPublicador.laEmisora.detenerAnuncio() -->
```

### 1.5 Utilidad de espera

```text
t: N --> esperar() -->
```

### 1.6 Utilidades de servicio

```text
p: [T]_n, n: N --> alReves() --> [T]_n

pString: Text, pUint: [Z]_16, tamMax: N --> stringAUint8AlReves() --> [Z]_16
```

### 1.7 Clases

```text
                        --------- LED ---------
                        |
                        | numero: N
                        | encendido: B
                        |
                        |
    n: N                --> LED() -->
                        |
                        |
                        --> encender() -->
                        |
                        |
                        --> apagar() -->
                        |
                        |
                        --> alternar() -->
                        |
                        |
    t: N                --> brillar() -->
                        |
                        ------------------------------
```

```text
                 --------- PuertoSerie ---------
                 |
                 | baudios: N
                 |
                 |
                 --> PuertoSerie() -->
                 |
                 |
                 --> esperarDisponible() -->
                 |
                 |
    mensaje: T    --> escribir() -->
                 |
                 ------------------------------
```

```text
                      --------- Medidor ---------
                      |
                      |
                      |
              --> Medidor() -->
                      |
                      |
              --> iniciarMedidor() -->
                      |
                      |
                Z <-- medirCO2() <--
                      |
                      |
                Z <-- medirTemperatura() <--
                      |
                      ------------------------------
```

```text
                      --------- Publicador ---------
                      |
                      | beaconUUID: [Z]_16
                      | laEmisora: EmisoraBLE
                      | RSSI: Z
                      |
                      |
                    --> Publicador() -->
                      |
                      |
                  --> encenderEmisora() -->
                      |
                      |
  valor: Z, cont: N, t: N --> publicarCO2() -->
                      |
                      |
  valor: Z, cont: N, t: N --> publicarTemperatura() -->
                      |
                      ------------------------------
```

```text
                      --------- EmisoraBLE ---------
                      |
                      | nombreEmisora: Text
                      | fabricanteID: N
                      | txPower: Z
                      |
                      |
  n: Text, fab: N, tx: Z --> EmisoraBLE() -->
                      |
                      |
                  --> encenderEmisora() -->
                      |
                      |
  cbce, cbct         --> encenderEmisora() -->
                      |
                      |
                  --> detenerAnuncio() -->
                      |
                      |
                B <-- estaAnunciando() <--
                      |
                      |
  uuid: [Z]_16, major: Z, minor: Z, rssi: Z
                      --> emitirAnuncioIBeacon() -->
                      |
                      |
  carga: Text, tam: N --> emitirAnuncioIBeaconLibre() -->
                      |
                      |
  servicio           --> anyadirServicio() --> B
                      |
                      |
  servicio, ...carac --> anyadirServicioConSusCaracteristicas() --> B
                      |
                      |
  servicio, ...carac --> anyadirServicioConSusCaracteristicasYActivar() --> B
                      |
                      |
  cb                 --> instalarCallbackConexionEstablecida() -->
                      |
                      |
  cb                 --> instalarCallbackConexionTerminada() -->
                      |
                      |
  connHandle: N     --> getConexion() --> Conexion
                      |
                      ------------------------------
```

```text
                  --------- Caracteristica ---------
                  |
                  | uuidCaracteristica: [Z]_16
                  | laCaracteristica
                  |
                  |
  nombre: Text    --> Caracteristica() -->
                  |
                  |
  nombre, props, permisoRead, permisoWrite, tam
                  --> Caracteristica() -->
                  |
                  |
  props: N        --> asignarPropiedades() -->
                  |
                  |
  permisoRead, permisoWrite --> asignarPermisos() -->
                  |
                  |
  tam: N          --> asignarTamanyoDatos() -->
                  |
                  |
  props, permisoRead, permisoWrite, tam
                  --> asignarPropiedadesPermisosYTamanyoDatos() -->
                  |
                  |
  str: Text       --> escribirDatos() --> N
                  |
                  |
  str: Text       --> notificarDatos() --> N
                  |
                  |
  cb              --> instalarCallbackCaracteristicaEscrita() -->
                  |
                  |
                  --> activar() -->
                  |
                  ------------------------------
```

```text
                --------- ServicioEnEmisora ---------
                |
                | uuidServicio: [Z]_16
                | elServicio
                | lasCaracteristicas: [ Caracteristica ]
                |
                |
  nombre: Text  --> ServicioEnEmisora() -->
                |
                |
                --> escribeUUID() -->
                |
                |
  car           --> anyadirCaracteristica() -->
                |
                |
                --> activarServicio() -->
                |
                |
                --> operator BLEService() --> BLEService
                |
                ------------------------------
```

### 1.8 Codificación del anuncio iBeacon

El campo `major` de 2 bytes codifica dos cosas: el tipo de medición en el byte alto y el
número de muestra en el byte bajo.

```text
major = ( valor( MedicionesID ) << 8 ) + cont

Ejemplos:
    cont = 1, CO2        ->  ( 11 << 8 ) + 1 = 2817
    cont = 1, TEMPERATURA -> ( 12 << 8 ) + 1 = 3073
```

El campo `minor` de 2 bytes transporta el valor medido, **con signo** (complemento a dos):

```text
    CO2 234        -> 0x00EA
    CO2 236        -> 0x00EC
    temperatura -12 -> 0xFFF4
```

---

## 2. Design Clarifications

- **`ServicioEnEmisora` está implementado pero no se usa.** Ninguna clase lo instancia; solo se
  llega a él por el `#include` que hace `EmisoraBLE.h`. Se conserva porque forma parte del
  componente y podría usarse para exponer un servicio GATT propio. No interviene en el
  funcionamiento actual.

- **`emitirAnuncioIBeaconLibre()` no se usa en el flujo normal.** Emite los 21 bytes libres del
  hueco del beacon metiendo texto arbitrario. Está comentada en `loop()` porque el móvil
  interpreta esos 21 bytes siempre como `uuid(16) + major(2) + minor(2) + txPower(1)`, de modo que
  un texto libre se lee como números basura y contaminaba las medidas reales.

- **`ScanResponse.clearData()` antes de `ScanResponse.addName()` es obligatorio.**
  `addName()` *añade* al final en lugar de sustituir. Como `emitirAnuncioIBeacon()` se llama dos
  veces por vuelta de bucle, sin limpiar el nombre se amontonaba (13, 26, 39 bytes…) hasta
  desbordar el buffer de 31 bytes del nRF52840. Al desbordar, la placa deja de anunciar del todo
  y en silencio. La llamada a `emitirAnuncioIBeaconLibre()` enmascaraba el problema porque
  llamaba a `clearData()` en su propio cuerpo.

- **El resultado de `Advertising.start()` se registra y se comprueba.** Si devuelve `false` el
  anuncio no ha arrancado y hay que saberlo, porque el síntoma observable (el móvil no recibe
  nada) es idéntico al de un fallo de reception.

- **`cont` es `N` y se pasa como `uint8_t`,** por lo que el número de muestra envuelve a 0 cada
  256 vueltas. Es intencionado: `major` sigue siendo un número natural de 2 bytes.

- **`random()` es semiabierto** (`random(min, max)` excluye `max`). Para hacer los rangos
  cerrados se usa `random( min, max + 1 )`: CO2 entre 234 y 236, temperatura entre -13 y -11.

- **`Medidor` no muta estado.** Es una simulación: `medirCO2()` y `medirTemperatura()` son
  lecturas puras sobre el generador global de Arduino, del que se ocupa `iniciarMedidor()` con
  `randomSeed(micros())`.

- **`esperar()` es `delay()` de Arduino,** es decir, bloqueo puro. Por eso el componente no es
  concurrente: el bucle principal se detiene 1 s emitiendo CO2, 1 s emitiendo temperatura y 2 s
  en pausa.

---

## 3. General Rules
- **Programming Language:** Java 11

- **Lenguaje de programación objetivo:** C++ para Arduino (nRF52840, Adafruit Bluefruit nRF52,
  framework Arduino con la biblioteca `Bluefruit`). Los ficheros de clase usan extensión `.h` y el
  programa principal `.ino`. No se usa `malloc`, ni punteros inteligentes, ni excepciones.

- **Encabezados de funciones y métodos:** cada declaración debe llevar inmediatamente encima su
  diseño lógico dentro de un bloque de comentario delimitado por líneas discontinuas
  (`--------------------`), con el formato `entradas --> nombre() --> salidas`. Ese bloque explica
  qué hace la función ("Qué hace"), no cómo está implementada.

  ```cpp
  // ------------------------------------------------------------------------------------
  // DISEÑO: tiempo: N --> esperar() -->
  // Qué hace: pausa el programa los milisegundos indicados
  // ------------------------------------------------------------------------------------
  ```

- **Legibilidad del código:** el código debe ser lo más claro y autoexplicativo posible, de modo
  que prácticamente no requiera comentarios adicionales dentro del cuerpo. Solo se comentan
  aquello que no se deduce de la lectura: el porqué de una decisión, el sentido de un número
  mágico y el aviso de una trampa. Los números mágicos que sí aparecen en el anuncio (el
  prefijo de Apple, el identificador de fabricante) se documentan en la cabecera de la función,
  no línea a línea.

- **Pruebas automatizadas:** hay que generar casos de prueba para todas las funciones y métodos
  críticos. En este componente los críticos son:

  - `Publicador::publicarCO2()` y `publicarTemperatura()`: comprobar que `major` se calcula como
    `( tipo << 8 ) + cont`, con los valores 2817 y 3073 para `cont = 1`.
  - `Medidor::medirCO2()`: comprobar que devuelve valores dentro de `[234, 236]` ambos incluidos.
  - `Medidor::medirTemperatura()`: comprobar que devuelve valores dentro de `[-13, -11]`, y que
    la codificación a `minor` de -12 produce `0xFFF4`.
  - `EmisoraBLE::estaAnunciando()` y `detenerAnuncio()`: comprobar que tras `encenderEmisora()`
    el anuncio puede arrancarse y pararse repetidamente sin acumular datos en la respuesta de
    escaneo.
  - `LED::alternar()`: comprobar que dos llamadas sucesivas devuelven el pin a su estado inicial.
  - `ServicioEnEmisora::stringAUint8AlReves()`: comprobar que invierte la cadena y trunca al
    tamaño máximo.

  La estrategia es doble: (a) tests de lógica pura que se ejecutan en el ordenador, que es donde
  se puede verificar el cálculo de `major` y los rangos de `random()`; (b) un test de integración
  sobre la placa que comprueba que `estaAnunciando()` es verdadero tras cada `start()` durante
  muchas vueltas seguidas.

  #### Los tests que hay escritos

  En `test/`, y se ejecutan sin placa con `test\ejecuta_pruebas.bat` (compila con `g++`):

  | Fichero | Qué comprueba | Pruebas |
  | :--- | :--- | ---: |
  | `EntornoArduinoFalso.h` | No es un test: es el doble de Arduino y de `EmisoraBLE` que permite compilar la lógica en el PC. Su `random()` imita el de Arduino, que es **semiabierto**. | — |
  | `test_medidor.cpp` | Que `medirCO2()` da 234, 235 y 236, y `medirTemperatura()` da -13, -12 y -11. Comprueba sobre todo que los **máximos son alcanzables**, que es lo que depende del `+ 1`. | 15 |
  | `test_publicador.cpp` | Que `major` vale 2817 para CO2 y 3073 para temperatura con `cont = 1`, que los dos tipos no se confunden, que el `minor` conserva el signo de la temperatura y que cada publicación espera y para el anuncio. | 29 |

  Se han comprobado rompiendo el código a propósito: si se quita el `+ 1` de `Medidor`, fallan
  6 pruebas; y estas 44 pruebas pasan con el código correcto.

  #### Lo que no se puede probar en el PC

  `EmisoraBLE` y `ServicioEnEmisora` dependen de la biblioteca Bluefruit y del hardware, así que
  no tienen test automático: solo se pueden comprobar en la placa. En particular, el fallo que
  más costó tiempo (el `ScanResponse.addName()` sin `clearData()`, que hacía que la placa
  dejara de anunciar en silencio) **no** se puede detectar con un test de este tipo, porque
  depende del búfer de 31 bytes de la respuesta de escaneo del nRF52840. Por eso `EmisoraBLE`
  imprime por el puerto serie el resultado de `Advertising.start()` y de `estaAnunciando()`
  en cada anuncio: esa es la forma de detectarlo en la placa.