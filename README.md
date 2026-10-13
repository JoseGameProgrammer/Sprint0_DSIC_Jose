# SPRINT 0 - Monitorización Medioambiental IoT (GTI - DSIC)

**Grado en Tecnologías Interactivas (Semestre 3A)**  
**Departamento de Sistemas Informáticos y Computación (DSIC)**  
**Autor:** Jose Manuel Martin Terre  

---

## 📖 1. Descripción del Proyecto

Este proyecto implementa el flujo integral del **Sprint 0** para un sistema distribuido de monitorización medioambiental (CO2 y temperatura), abarcando desde el hardware embebido hasta la visualización en navegador web:

1. **Emisor BLE (Arduino / nRF52840):** La placa Adafruit Bluefruit nRF52840 simula lecturas de CO2 (234–236 ppm) y temperatura con signo (-13 a -11 °C), codificándolas en tramas iBeacon (`major` y `minor`) y emitiéndolas al aire con UUID `EPSG-GTI-PROY-3A`.
2. **Receptor y Gateway Móvil (Android):** Una aplicación en Java escanea paquetes de anuncios Bluetooth Low Energy, desempaqueta la trama iBeacon y, a través de su **proxy de cliente (`LogicaCliente`)**, reenvía las mediciones mediante peticiones HTTP POST al servidor REST.
3. **Servidor Backend y Persistencia (PHP & SQLite):**
   - **Capa de Comunicación:** Endpoint `GuardarMedida.php` (recibe POST con JSON) y `ObtenerMedidas.php` (atiende GET con límite).
   - **Capa de Lógica del Negocio:** `LogicaMedidas.php`, 100% independiente de HTTP, procesa las magnitudes y gestiona la persistencia.
   - **Capa de Base de Datos:** Base de datos relacional SQLite (`bdd.sqlite`) estructurada con una **clave primaria compuesta natural `(ID_Sensor, Fecha)`**.
4. **Visualizador Web (HTML5 & JavaScript):** Interfaz gráfica responsive (`index.html` y `gui.js`) que consulta periódicamente las medidas al servidor a través de su proxy cliente (`LogicaFake.js`) y muestra la última medida y el histórico en tablas sincronizadas.

---

## 🏛️ 2. Arquitectura del Sistema y Desacoplamiento

El sistema sigue una arquitectura por capas estrictamente desacoplada:

* **Separación de la Triada en Backend:**
  - `doc/web_communication_design.md` $\rightarrow$ `src/web_communication/`
  - `doc/web_business_logic_design.md` $\rightarrow$ `src/web_business_logic/`
  - `doc/web_database_design.md` $\rightarrow$ `src/web_database/`
* **Patrón Proxy (Lógica Fake) en Clientes:**
  - En la Web: `src/web_gui/` nunca hace llamadas directas de transporte (`fetch`/`XMLHttpRequest`); delega en `src/web_frontend_business_logic/LogicaFake.js`.
  - En Android: `src/btle_gui/` (MainActivity) no realiza peticiones de red directas; delega en `src/btle_frontend_business_logic/LogicaCliente.java`.
* **Clave Primaria Compuesta Natural:**
  - Se prescinde de clave subrogada artificial (`ID` autoincremental) en la tabla `Medidas`.
  - Clave primaria: `PRIMARY KEY (ID_Sensor, Fecha)`.
  - Mapeo de sensor: `1` para CO2, `2` para TEMPERATURA y `3` para RUIDO.

---

## 🚀 3. Cómo Desplegar el Proyecto

### 3.1 Emisor Arduino (Hardware nRF52840)
1. Abrir **Arduino IDE**.
2. Instalar el soporte de placas **Adafruit Bluefruit nRF52** y la biblioteca `Adafruit BluefruitLE nRF52 Libraries`.
3. Abrir el sketch: [`src/arduino_emisor/HolaMundoIBeacon/HolaMundoIBeacon.ino`](file:///c:/Users/joseg/Documents/Sprint0_Entrega_Final/Sprint0_DSIC_Jose/src/arduino_emisor/HolaMundoIBeacon/HolaMundoIBeacon.ino).
4. Seleccionar la placa **Adafruit Bluefruit Feather nRF52840 Express** y el puerto COM correspondiente.
5. Compilar y subir el programa. La placa comenzará a emitir balizas iBeacon con el nombre `GTI-3A-Jose`.

### 3.2 Servidor Backend y Base de Datos (PHP & SQLite)
1. Requiere **PHP 8** con la extensión `pdo_sqlite` habilitada.
2. Iniciar el servidor embebido desde la carpeta raíz del proyecto:
   ```powershell
   php -S 127.0.0.1:8080 -t src
   ```
3. Verificar que la base de datos responde accediendo desde el navegador o mediante consola a:
   ```text
   http://127.0.0.1:8080/web_database/VerTabla.php
   ```

### 3.3 Aplicación Android
1. Abrir el proyecto [`src/btle_gui`](file:///c:/Users/joseg/Documents/Sprint0_Entrega_Final/Sprint0_DSIC_Jose/src/btle_gui) en **Android Studio** (Koala / Ladybug o superior).
2. Asegurarse de que el dispositivo Android tiene el Bluetooth encendido y permisos de ubicación/escaneo BLE concedidos.
3. Si se prueba en dispositivo físico, configurar la IP del servidor en `ServicioEscucharBeacons.java` (o dejar `10.0.2.2` si se utiliza el emulador de Android Studio).
4. Ejecutar la aplicación en el dispositivo y pulsar **"Buscar nuestro dispositivo"** para recibir emisiones y enviarlas al backend.

### 3.4 Interfaz Web
1. Con el servidor PHP en ejecución, abrir en el navegador:
   ```text
   http://127.0.0.1:8080/web_gui/index.html
   ```
2. La vista mostrará la última medición registrada y se refrescará automáticamente cada 3 segundos.

---

## 🧪 4. Cómo Ejecutar los Tests Automáticos

El proyecto cuenta con **139 pruebas automáticas reproducibles** que cubren todos los componentes del sistema:

### 4.1 Pruebas de la Placa Arduino (44 tests C++)
No requieren la placa física conectada; se ejecutan sobre el doble de simulación de entorno:
```powershell
cd src\arduino_emisor\test
.\ejecuta_pruebas.bat
```
* **Medidor:** 15 pruebas (rangos alcanzables de CO2 [234, 236] y temperatura [-13, -11]).
* **Publicador:** 29 pruebas (codificación de `major` y `minor` con signo, UUID, temporización).

### 4.2 Pruebas de la Aplicación Android (55 tests unitarios JUnit)
Desde la consola en el directorio de la aplicación Android:
```powershell
cd src\btle_gui
.\gradlew.bat :app:testDebugUnitTest
```
* **UtilidadesTest:** 32 pruebas (conversión de tramas, complemento a 2 y números con signo).
* **TramaIBeaconTest:** 20 pruebas (desempaquetado del formato Apple iBeacon de 30 bytes).
* **LogicaClienteTest:** 3 pruebas (contrato de delegación y validación del proxy).

### 4.3 Pruebas de Base de Datos SQLite (5 tests PHP)
```powershell
php src/web_database/testBD.php
```
* Verifica el esquema SQLite, 4 columnas canónicas, clave compuesta `(ID_Sensor, Fecha)` y rechazo de duplicados.

### 4.4 Pruebas de Lógica de Negocio (9 tests PHP)
```powershell
php src/web_business_logic/testLogicaMedidas.php
```
* Valida persistencia de CO2, temperatura negativa, asignación de sensores físicos y sanitización de límites.

### 4.5 Pruebas de Comunicación REST (7 tests PHP)
```powershell
php src/web_communication/testServidorREST.php
```
* Arranca un servidor HTTP embebido temporal y comprueba peticiones POST, respuestas GET y la regla de desacoplamiento de SQL.

### 4.6 Pruebas de Frontend y Experiencia de Usuario (19 tests JavaScript)
* Abrir `src/web_gui/index.html` en el navegador (ejecuta automáticamente las comprobaciones DOM de `testUX.js` y contrato de `testLogicaFake.js` en la consola).

---

## 🤖 5. Uso Responsable de Herramientas de IA

De acuerdo con las directrices de la asignatura, el uso de asistentes de IA se ha guiado por una metodología rigurosa:
1. **Diseño previo:** Todos los componentes se diseñaron previamente de forma manual en notación formal abstracta dentro de la carpeta `doc/` antes de generar código.
2. **Prompts estructurados:** Se utilizaron prompts basados estrictamente en los contratos, tipos de datos y firmas definidos en los documentos de diseño.
3. **Revisión y verificación:** Todo el código generado por IA fue auditado, refactorizado para eliminar deuda técnica, y validado mediante suites de tests automatizados independientes con 100% de cobertura.
