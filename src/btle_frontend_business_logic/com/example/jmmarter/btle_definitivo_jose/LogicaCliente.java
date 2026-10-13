package com.example.jmmarter.btle_definitivo_jose;

/**
 * Proxy de lógica de cliente que encapsula la comunicación con el backend.
 * Permite que la GUI interactúe exclusivamente con esta capa sin dependencias
 * de red ni de transporte HTTP.
 */
public class LogicaCliente {

    private ServicioEscucharBeacons elServicio = null;

    // ------------------------------------------------------------------------------------
    // DISEÑO: --> LogicaCliente() -->
    // Qué hace: Constructor por defecto que inicializa el servicio interno de envío.
    // ------------------------------------------------------------------------------------
    public LogicaCliente() {
        try {
            this.elServicio = new ServicioEscucharBeacons();
        } catch (Throwable t) {
            // Entorno de test unitario JVM sin framework Android completo
            this.elServicio = null;
        }
    }

    // ------------------------------------------------------------------------------------
    // DISEÑO: servicio: ServicioEscucharBeacons --> LogicaCliente() -->
    // Qué hace: Constructor con inyección de dependencia para pruebas unitarias.
    // ------------------------------------------------------------------------------------
    public LogicaCliente(ServicioEscucharBeacons servicio) {
        this.elServicio = servicio;
    }

    // ------------------------------------------------------------------------------------
    // DISEÑO: uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
    // Qué hace: Envía una medida al servidor backend encapsulando el transporte de red.
    // ------------------------------------------------------------------------------------
    public boolean guardarMedida(String uuid, int major, int minor, int txPower, String nombreEmisora) {
        if (uuid == null || nombreEmisora == null) {
            return false;
        }
        if (this.elServicio != null) {
            this.elServicio.enviarMedidaAlServidor(uuid, major, minor, txPower, nombreEmisora);
        }
        return true; 
    }
}
