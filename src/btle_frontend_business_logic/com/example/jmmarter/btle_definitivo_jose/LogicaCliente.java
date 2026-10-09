package com.example.jmmarter.btle_definitivo_jose;

/**
 * Proxy de lgica de cliente que encapsula la comunicacin con el backend.
 */
public class LogicaCliente {

    // ------------------------------------------------------------------------------------
    // DISEO: uuid: Text, major: N, minor: Z, txPower: Z, nombreEmisora: Text --> guardarMedida() --> B
    // Qu hace: Enva una medida al servidor backend de forma transparente.
    // ------------------------------------------------------------------------------------
    public boolean guardarMedida(String uuid, int major, int minor, int txPower, String nombreEmisora) {
        // Aqu internamente se llamara a PeticionarioREST para hacer el HTTP POST
        // Ocultando los detalles de red a la GUI.
        return true; 
    }
}
