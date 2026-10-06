// Pruebas de Publicador.h: comprueban cómo se codifica la medición en los dos campos del
// iBeacon, que es el CONTRATO entre la placa y el móvil.
//
// El truco del que todo depende:
//     major = (tipo de medición << 8) + número de muestra
//     minor = el valor medido, con signo
//
// Si el byte alto del major se mueve, el móvil deja de saber si lo que ve es una
// concentración de CO2 o una temperatura. Estas pruebas lo fijan.
//
// Compilar y ejecutar:  ver ejecutar_tests.bat

#include "EntornoArduinoFalso.h"
#include "../HolaMundoIBeacon/Publicador.h"

int main() {

	empezarSuite( "Publicador (codificacion del major y el minor)" );

	Publicador elPublicador;

	// -----------------------------------------------------------------------------------
	// La configuración de la emisora
	// -----------------------------------------------------------------------------------

	comprobarIgual( (int) elPublicador.laEmisora.nombreEmisora.size(), 11,
			"el nombre del emisor mide 11 caracteres" );
	comprobar( elPublicador.laEmisora.nombreEmisora == "GTI-3A-Jose",
			"el nombre del emisor es GTI-3A-Jose" );
	comprobarIgual( (int) elPublicador.laEmisora.fabricanteID, 0x004c,
			"el fabricante es Apple (0x004C), que es lo que hace que el movil lo reconozca" );
	comprobarIgual( (int) elPublicador.laEmisora.txPower, 4,
			"la potencia de emision es 4" );

	// -----------------------------------------------------------------------------------
	// CO2
	// -----------------------------------------------------------------------------------

	// CO2 = 11, muestra 1  ->  major = (11 << 8) + 1 = 2817
	elPublicador.publicarCO2( 234, 1, 0 );

	comprobar( elPublicador.laEmisora.seHaAnunciado,
			"publicarCO2() ha pedido anunciar" );
	comprobarIgual( (int) elPublicador.laEmisora.majorRecibido, 2817,
			"CO2 de la muestra 1 da major = 2817" );
	comprobarIgual( (int) elPublicador.laEmisora.minorRecibido, 234,
			"el valor de CO2 va entero en el minor" );

	// El byte alto del major es el tipo de medición:
	comprobarIgual( elPublicador.laEmisora.majorRecibido / 256, 11,
			"el byte alto del major es 11 = CO2" );
	comprobarIgual( elPublicador.laEmisora.majorRecibido % 256, 1,
			"el byte bajo del major es el numero de muestra" );

	// La muestra avanza: 2 -> 2818
	elPublicador.publicarCO2( 235, 2, 0 );
	comprobarIgual( (int) elPublicador.laEmisora.majorRecibido, 2818,
			"CO2 de la muestra 2 da major = 2818" );

	// El ultimo valor del rango de CO2, 236
	elPublicador.publicarCO2( 236, 3, 0 );
	comprobarIgual( (int) elPublicador.laEmisora.minorRecibido, 236,
			"el CO2 puede valer 236, el maximo" );

	// La muestra 255 es la ultima antes de que el contador (que es uint8_t) se desbordaria
	elPublicador.publicarCO2( 234, 255, 0 );
	comprobarIgual( (int) elPublicador.laEmisora.majorRecibido, 11 * 256 + 255,
			"CO2 de la muestra 255 da major = 3091" );

	// -----------------------------------------------------------------------------------
	// TEMPERATURA
	// -----------------------------------------------------------------------------------

	// TEMPERATURA = 12, muestra 1  ->  major = (12 << 8) + 1 = 3073
	elPublicador.publicarTemperatura( -13, 1, 0 );

	comprobar( elPublicador.laEmisora.seHaAnunciado,
			"publicarTemperatura() ha pedido anunciar" );
	comprobarIgual( (int) elPublicador.laEmisora.majorRecibido, 3073,
			"temperatura de la muestra 1 da major = 3073" );
	comprobarIgual( elPublicador.laEmisora.majorRecibido / 256, 12,
			"el byte alto del major es 12 = TEMPERATURA, distinto del CO2" );

	// Y AQUI ESTA LO IMPORTANTE: la temperatura es NEGATIVA y tiene que travel con el signo.
	comprobarIgual( (int) elPublicador.laEmisora.minorRecibido, -13,
			"una temperatura de -13 grados viaja en el minor como -13, con el signo" );

	// En el aire son 0xFFF3 (65531 sin signo), que es lo que le llegara al movil como bytes.
	// El movil lo vuelve a leer con Utilidades.bytesToInt(), que hace complemento a dos.
	uint16_t comoLoVeriaElMovilSinSigno = (uint16_t) elPublicador.laEmisora.minorRecibido;
	comprobarIgual( (int) comoLoVeriaElMovilSinSigno, 0xfff3,
			"-13 en complemento a dos de 16 bits es 0xFFF3" );
	comprobar( comoLoVeriaElMovilSinSigno > 32767,
			"el movil ve un numero grande sin signo, y por eso necesita interpretar con signo" );

	elPublicador.publicarTemperatura( -11, 2, 0 );
	comprobarIgual( (int) elPublicador.laEmisora.majorRecibido, 3074,
			"temperatura de la muestra 2 da major = 3074" );
	comprobarIgual( (int) elPublicador.laEmisora.minorRecibido, -11,
			"-11 grados es el maximo y tambien viaja con signo" );

	// -----------------------------------------------------------------------------------
	// Los dos tipos de medicion NO se confunden
	// -----------------------------------------------------------------------------------

	// Misma muestra, mismos digitos, distinta magnitud: los majors tienen que ser distintos.
	elPublicador.publicarCO2( 234, 1, 0 );
	int16_t majorDelCO2 = elPublicador.laEmisora.majorRecibido;
	elPublicador.publicarTemperatura( 234, 1, 0 );
	int16_t majorDeLaTemperatura = elPublicador.laEmisora.majorRecibido;

	comprobar( majorDelCO2 != majorDeLaTemperatura,
			"CO2 y temperatura tienen majors distintos aunque la muestra sea la misma" );
	comprobarIgual( majorDelCO2 - majorDeLaTemperatura, -256,
			"la diferencia entre CO2 y temperatura es exactamente 256" );

	// -----------------------------------------------------------------------------------
	// El UUID que identifica la baliza
	// -----------------------------------------------------------------------------------

	elPublicador.publicarCO2( 234, 1, 0 );

	std::string uuidLeido;
	for ( int i = 0 ; i < 16 ; ++i ) {
		uuidLeido += (char) elPublicador.laEmisora.uuidRecibido[i];
	}
	comprobar( uuidLeido == "EPSG-GTI-PROY-3A",
			"el uuid de la baliza es EPSG-GTI-PROY-3A" );

	// -----------------------------------------------------------------------------------
	// Lo que le llega al móvil dentro del RSSI
	// -----------------------------------------------------------------------------------

	// OJO: RSSI es -53, pero emitirAnuncioIBeacon() lo recibe como uint8_t, que no tiene
	// signo. Al pasar de int a uint8_t, -53 se guarda como 256 - 53 = 203. El móvil nunca
	// lee el RSSI del beacons (usa la intensidad que le da el sistema), asi que esto no
	// rompe nada. Pero queda escrito aqui para que no parezca un fallo.
	comprobarIgual( (int) elPublicador.RSSI, -53,
			"la constante RSSI del Publicador es -53" );
	comprobarIgual( (int) elPublicador.laEmisora.rssiRecibido, 203,
			"el -53 llega a la emisora como 203, por el truncamiento a uint8_t" );

	// -----------------------------------------------------------------------------------
	// Encendido y apagado del anuncio
	// -----------------------------------------------------------------------------------

	elPublicador.encenderEmisora();
	comprobarIgual( elPublicador.laEmisora.vecesEncendida, 1,
			"encenderEmisora() enciende la pila BLE una vez" );

	int detenidoAntes = elPublicador.laEmisora.vecesDetenido;
	int esperasAntes = g_vecesQueSeHaEsperado;
	long milisegundosAntes = g_milisegundosEsperados;
	elPublicador.publicarCO2( 234, 1, 400 );
	comprobar( elPublicador.laEmisora.vecesDetenido > detenidoAntes,
			"publicarCO2() detiene el anuncio cuando ha terminado de esperar" );

	// Cada publicación espera UNA vez, la que le pasa el bucle: ni se salta la espera ni
	// espera de más. Se compara con lo que había antes, y no con un número fijo, porque si
	// se añade otra publicación a esta prueba el número fijo ya no valdría.
	comprobarIgual( g_vecesQueSeHaEsperado - esperasAntes, 1,
			"publicarCO2() espera exactamente una vez, el tiempo que le pasan" );

	// Y espera el tiempo que le pasan, que es el que el bucle usa para que la trama quepa
	// entera en la ventana de anuncio. 400 ms son 128 intervalos de 0,625 ms.
	comprobarIgual( (int) ( g_milisegundosEsperados - milisegundosAntes ), 400,
			"publicarCO2() espera los 400 milisegundos que le han pasado" );

	return terminarSuite();
}