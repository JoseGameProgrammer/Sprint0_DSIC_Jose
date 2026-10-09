import os

path = r'C:\Users\joseg\Documents\Sprint0_Entrega_Final\Sprint0_DSIC_Jose\src\BTLE\app\src\main\res\layout\activity_main.xml'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

new_btn = """
    <Button
        android:id="@+id/botonSimularBeacon"
        android:text="Simular recepción de Beacon (CO2)"
        android:onClick="botonSimularBeaconPulsado"
        android:backgroundTint="#FF9800"
        android:textColor="#FFFFFF"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
"""

text = text.replace('<!-- ================= MEDIDA AL SERVIDOR REST ================= -->', '<!-- ================= MEDIDA AL SERVIDOR REST ================= -->\n' + new_btn)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)

path2 = r'C:\Users\joseg\Documents\Sprint0_Entrega_Final\Sprint0_DSIC_Jose\src\BTLE\app\src\main\java\com\example\jmmarter\btle_definitivo_jose\MainActivity.java'
with open(path2, 'r', encoding='utf-8') as f:
    text2 = f.read()

new_method = """
    public void botonSimularBeaconPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton simular beacon pulsado");
        this.actualizarEstado("Simulando recepción de un iBeacon...");

        // Simulamos un iBeacon: UUID conocido, major 2817 (tipo 11 -> CO2), minor aleatorio (400-800 ppm), txPower -53
        String uuid = "EPSG-GTI-PROY-3A";
        int major = 2817; // 11 << 8 + 1
        int minor = 400 + (int)(Math.random() * 400); // random val 400-800
        int txPower = -53;
        String nombreEmisora = "GTI-Jose-Simulado";

        // Mismo proceso que cuando recibimos uno real
        if (this.servicioParaEnviarAlServidor == null) {
            this.servicioParaEnviarAlServidor = new ServicioEscucharBeacons();
        }
        
        this.servicioParaEnviarAlServidor.enviarMedidaAlServidor(uuid, major, minor, txPower, nombreEmisora);
        
        // Y actualizamos la UI (tal y como lo hace mostrarInformacionDispositivoBTLE y actualizarUltimaMedicionEnPantalla)
        String textoMedicion = String.format(
            "Última medición recibida:\nTipo: %d, Valor: %d\nEmisora: %s",
            major, minor, nombreEmisora
        );
        this.laEtiquetaUltimaMedicion.setText(textoMedicion);
    }

"""

# Insert new_method after botonTestEnviarMedidaPulsado
text2 = text2.replace(
    'ServicioEscucharBeacons.probarEnviarMedidaAlServidor();\n    } // ()',
    'ServicioEscucharBeacons.probarEnviarMedidaAlServidor();\n    } // ()\n\n' + new_method
)

with open(path2, 'w', encoding='utf-8') as f:
    f.write(text2)
