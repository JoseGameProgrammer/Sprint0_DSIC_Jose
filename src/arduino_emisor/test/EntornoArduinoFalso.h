// Entorno falso de Arduino para poder probar Medidor.h y Publicador.h en el PC.
//
// Las clases del sketch dependen de Arduino (random, micros, delay) y de la biblioteca
// Bluefruit, que solo existen en la placa. Aquí se sustituyen por lo mínimo que hace falta
// para compilar y ejecutar la LÓGICA en un ordenador normal con g++.
//
// LO IMPORTANTE: este random() imita el comportamiento REAL de Arduino, que es
// SEMIABIERTO. random(234, 236) en Arduino devuelve 234 y 235, NUNCA 236, porque el
// máximo queda fuera. Si el random() falso fuera cerrado, las pruebas de los rangos pasarían
// siempre y no valdrían para nada. Por eso está copiado a mano, en vez de usar el
// std::rand de C++, que tiene otra semántica.

#ifndef ENTORNO_ARDUINO_FALSO_H_INCLUIDO
#define ENTORNO_ARDUINO_FALSO_H_INCLUIDO

#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <set>

// ===========================================================================================
// SEMILLA Y GENERADOR
// ===========================================================================================

static unsigned long g_semillaDelAzar = 1;

// randomSeed(): en Arduino fija la semilla del generador global. Si se le pasa 0, Arduino
// usa 1, y por eso aquí también.
inline void randomSeed( unsigned long semilla ) {
	g_semillaDelAzar = ( semilla != 0UL ) ? semilla : 1UL;
}

// micros(): microsegundos desde que arrancó la placa. Devolvemos siempre lo mismo porque en
// un ordenador no hay nada que medir, y así las pruebas salen siempre iguales.
inline unsigned long micros() {
	return 20261003UL;
}

// random( minimo, maximo ): como el de Arduino, devuelve un entero en [minimo, maximo),
// es decir, el máximo queda FUERA. Es lo que hace que Medidor tenga que poner el "+ 1".
inline long random( long minimo, long maximo ) {

	if ( minimo >= maximo ) {
		return minimo;
	}

	long rango = maximo - minimo;

	// Generador lineal congruente, el mismo que usa el rand() clásico de C.
	g_semillaDelAzar = g_semillaDelAzar * 1103515245UL + 12345UL;

	long desplazamiento = (long) ( ( g_semillaDelAzar >> 16 ) % (unsigned long) rango );

	return minimo + desplazamiento;
}

// ===========================================================================================
// esperar(): el sketch la llama entre anuncio y anuncio. Aquí no esperamos de verdad,
// solo contamos cuántas veces se llama, que es lo que algunas pruebas comprueban.
// ===========================================================================================

static int g_vecesQueSeHaEsperado = 0;
static long g_milisegundosEsperados = 0;

inline void esperar( long milisegundos ) {
	++g_vecesQueSeHaEsperado;
	g_milisegundosEsperados += milisegundos;
}

// ===========================================================================================
// EMISORA BLE FALSA
// ===========================================================================================

// Sustituye a la EmisoraBLE de verdad. No toca el hardware: se queda con los valores que
// Publicador le pasa y los guarda, para que las pruebas puedan mirar qué se iba a anunciar.
class EmisoraBLE {

public:

	// Lo que se le pasó al construirla.
	std::string nombreEmisora;
	uint16_t fabricanteID;
	int8_t txPower;

	// Lo que se ha registrado de la última llamada a emitirAnuncioIBeacon().
	bool seHaAnunciado;
	uint8_t uuidRecibido[16];
	int16_t majorRecibido;
	int16_t minorRecibido;
	uint8_t rssiRecibido;

	// Contadores.
	int vecesEncendida;
	int vecesDetenido;

	EmisoraBLE( const char * nombre, uint16_t fabricante, int8_t potencia )
		:	nombreEmisora( nombre ),
			fabricanteID( fabricante ),
			txPower( potencia ),
			seHaAnunciado( false ),
			majorRecibido( 0 ),
			minorRecibido( 0 ),
			rssiRecibido( 0 ),
			vecesEncendida( 0 ),
			vecesDetenido( 0 )
	{
		memset( uuidRecibido, 0, 16 );
	}

	// La de verdad hace Bluefruit.begin(). Aquí solo se apunta.
	void encenderEmisora() {
		++vecesEncendida;
	}

	void detenerAnuncio() {
		++vecesDetenido;
	}

	// ESTA ES LA QUE IMPORTA: es donde Publicador mete major y minor, que es justo lo que
	// estas pruebas quieren comprobar.
	void emitirAnuncioIBeacon( uint8_t * beaconUUID, int16_t major, int16_t minor, uint8_t rssi ) {

		seHaAnunciado = true;
		memcpy( uuidRecibido, beaconUUID, 16 );
		majorRecibido = major;
		minorRecibido = minor;
		rssiRecibido = rssi;
	}

}; // class

// ===========================================================================================
// MINI-FRAMEWORK DE ASERTOS
// ===========================================================================================

static int g_numeroDePruebas = 0;
static int g_numeroDeFallos = 0;
static std::string g_nombreDeLaSuite = "";

inline void empezarSuite( const char * nombre ) {
	g_nombreDeLaSuite = nombre;
	g_numeroDePruebas = 0;
	g_numeroDeFallos = 0;
	printf( "\n=== %s ===\n", nombre );
}

inline void comprobar( bool condicion, const char * descripcion ) {

	++g_numeroDePruebas;

	if ( condicion ) {
		printf( "  [ok]   %s\n", descripcion );
	} else {
		++g_numeroDeFallos;
		printf( "  [FALLO] %s\n", descripcion );
	}
}

template <typename T>
inline void comprobarIgual( T obtenido, T esperado, const char * descripcion ) {

	++g_numeroDePruebas;

	if ( obtenido == esperado ) {
		printf( "  [ok]   %s\n", descripcion );
	} else {
		++g_numeroDeFallos;
		printf( "  [FALLO] %s  (obtenido %lld, esperado %lld)\n",
				descripcion,
				(long long) obtenido,
				(long long) esperado );
	}
}

// Devuelve 0 si todo fue bien, que es lo que espera el sistema operativo.
inline int terminarSuite() {

	printf( "  ---- %s: %d prueba(s), %d fallo(s)\n",
			g_nombreDeLaSuite.c_str(), g_numeroDePruebas, g_numeroDeFallos );

	if ( g_numeroDeFallos == 0 ) {
		printf( "  %s: TODAS LAS PRUEBAS PASAN\n", g_nombreDeLaSuite.c_str() );
		return 0;
	}

	printf( "  %s: HAY FALLOS\n", g_nombreDeLaSuite.c_str() );
	return 1;
}

#endif