# web_gui_design

Componente: **interfaz de usuario web** (`web_gui`). Ofrece una vista visual que presenta la última medida recibida y el histórico de mediciones en una tabla, actualizándose periódicamente.

Código correspondiente: `src/web_gui/`
Ficheros: `index.html`, `gui.js`, `testUX.js`

---

## 1. Component Design

### 1.1 Elementos de la Interfaz
La vista HTML contiene dos secciones principales:
1. **Tarjeta de última medición:** Muestra el valor más reciente de la magnitud y su timestamp.
2. **Tabla de histórico:** Listado tabular con las columnas `Fecha`, `ID Sensor`, `Tipo Medición` y `Valor Medición`.

### 1.2 Funciones de Renderizado y Control de UI

```text
unaMedida: Medida      --> pintarLaUltimaMedida() -->
lasMedidas: [ Medida ] --> pintarLasMedidasAnteriores() -->
unaMedida: Medida      --> filaDeMedida() --> [ Text ]
unValor: Text          --> celdaDe() --> [ Text ]
unTexto: Text          --> filaDe() --> [ Text ]
unMensaje: Text        --> avisarUnaVez() -->
                       --> iniciarRefresco() -->
                       --> detenerRefresco() -->
```

---

## 2. Design Clarifications

- **Desacoplamiento estricto:** La GUI no contiene llamadas directas de transporte HTTP (`fetch`, `XMLHttpRequest` directo en eventos de usuario ni WebSockets).
- **Interacción exclusiva con el Proxy de Cliente:** La GUI delega la obtención periódica de datos a la capa `web_frontend_business_logic` (`obtenerMedidas()`).
- **Prevención de alertas repetidas:** `avisarUnaVez()` previene la saturación visual ante desconexiones transitorias.

---

## 3. General Rules

- **Programming Language:** HTML5 / JavaScript (ES6+ sin dependencias externas ni frameworks).
- **Function/Method Headers:** Cada función o método incluye su diseño lógico formal delimitado por líneas discontinuas (`--------------------`), con el formato `entradas --> nombre() --> salidas`, seguido de una línea "Qué hace".
- **Code Readability:** Código declarativo y autoexplicativo, evitando comentarios redundantes dentro de los cuerpos de función.
- **Automated Testing:** Casos de prueba automatizados en `testUX.js` para verificar la manipulación del DOM y renderizado correcto.
