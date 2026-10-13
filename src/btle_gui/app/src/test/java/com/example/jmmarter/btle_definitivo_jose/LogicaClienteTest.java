package com.example.jmmarter.btle_definitivo_jose;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias de la lógica del proxy de cliente (LogicaCliente).
 * Se ejecutan en la máquina virtual local (JVM) sin necesidad de dispositivo físico.
 */
public class LogicaClienteTest {

    @Test
    public void guardarMedida_conParametrosValidos_devuelveTrue() {
        LogicaCliente proxy = new LogicaCliente();
        boolean resultado = proxy.guardarMedida("EPSG-GTI-PROY-3A", 2817, 235, -53, "GTI-Jose");
        assertTrue("Una medida válida debe ser aceptada por el proxy", resultado);
    }

    @Test
    public void guardarMedida_conUUIDNulo_devuelveFalse() {
        LogicaCliente proxy = new LogicaCliente();
        boolean resultado = proxy.guardarMedida(null, 2817, 235, -53, "GTI-Jose");
        assertFalse("Una medida con UUID nulo debe ser rechazada", resultado);
    }

    @Test
    public void guardarMedida_conNombreEmisoraNulo_devuelveFalse() {
        LogicaCliente proxy = new LogicaCliente();
        boolean resultado = proxy.guardarMedida("EPSG-GTI-PROY-3A", 2817, 235, -53, null);
        assertFalse("Una medida con nombre de emisora nulo debe ser rechazada", resultado);
    }
}
