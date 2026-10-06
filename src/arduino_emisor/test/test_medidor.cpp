// Pruebas de Medidor.h: comprueban que los valores simulados están en el rango que pide
// el enunciado, y sobre todo que los extremos SON ALCANZABLES.
//
// Esto último es lo importante. Medidor llama a random( 234, 237 ) y a
// random( -13, -10 ). Si alguien quita ese "+ 1", el código sigue compilando, sigue
// funcionando y no da ningún error... pero 236 y -11 ya nunca saldrían, y las medidas
// llegarían sesgadas al móvil. Estas pruebas lo detectan.
//
// Compilar y ejecutar:  ver ejecutar_tests.bat

#include "EntornoArduinoFalso.h"
#include "../HolaMundoIBeacon/Medidor.h"

int main() {

	empezarSuite( "Medidor (rangos de la simulacion)" );

	Medidor elMedidor;
	elMedidor.iniciarMedidor();

	// -----------------------------------------------------------------------------------
	// CO2: tiene que dar 234, 235 o 236
	// -----------------------------------------------------------------------------------

	std::set<int> valoresDeCO2;
	for ( int i = 0 ; i < 5000 ; ++i ) {
		valoresDeCO2.insert( elMedidor.medirCO2() );
	}

	printf( "  valores de CO2 obtenidos: " );
	for ( std::set<int>::iterator it = valoresDeCO2.begin() ; it != valoresDeCO2.end() ; ++it ) {
		printf( "%d ", *it );
	}
	printf( "\n" );

	comprobarIgual( (int) valoresDeCO2.size(), 3,
			"el CO2 sale de 3 valores distintos" );

	comprobar( valoresDeCO2.find( 234 ) != valoresDeCO2.end(),
			"el CO2 puede ser 234, que es el minimo del enunciado" );
	comprobar( valoresDeCO2.find( 235 ) != valoresDeCO2.end(),
			"el CO2 puede ser 235, el valor intermedio" );
	comprobar( valoresDeCO2.find( 236 ) != valoresDeCO2.end(),
			"el CO2 puede ser 236, que es el MAXIMO y solo sale gracias al '+ 1'" );

	comprobar( *valoresDeCO2.begin() >= 234,
			"ningun CO2 baja de 234" );
	comprobar( *valoresDeCO2.rbegin() <= 236,
			"ningun CO2 sube de 236" );

	// -----------------------------------------------------------------------------------
	// TEMPERATURA: tiene que dar -13, -12 o -11
	// -----------------------------------------------------------------------------------

	std::set<int> valoresDeTemperatura;
	for ( int i = 0 ; i < 5000 ; ++i ) {
		valoresDeTemperatura.insert( elMedidor.medirTemperatura() );
	}

	printf( "  valores de temperatura obtenidos: " );
	for ( std::set<int>::iterator it = valoresDeTemperatura.begin() ;
		  it != valoresDeTemperatura.end() ; ++it ) {
		printf( "%d ", *it );
	}
	printf( "\n" );

	comprobarIgual( (int) valoresDeTemperatura.size(), 3,
			"la temperatura sale de 3 valores distintos" );

	comprobar( valoresDeTemperatura.find( -13 ) != valoresDeTemperatura.end(),
			"la temperatura puede ser -13, que es el MINIMO" );
	comprobar( valoresDeTemperatura.find( -12 ) != valoresDeTemperatura.end(),
			"la temperatura puede ser -12, el valor intermedio" );
	comprobar( valoresDeTemperatura.find( -11 ) != valoresDeTemperatura.end(),
			"la temperatura puede ser -11, que es el MAXIMO y solo sale gracias al '+ 1'" );

	comprobar( *valoresDeTemperatura.begin() >= -13,
			"ninguna temperatura baja de -13" );
	comprobar( *valoresDeTemperatura.rbegin() <= -11,
			"ninguna temperatura sube de -11" );

	// -----------------------------------------------------------------------------------
	// La temperatura es NEGATIVA a propósito
	// -----------------------------------------------------------------------------------

	comprobar( *valoresDeTemperatura.rbegin() < 0,
			"toda la temperatura es negativa, como pide el enunciado" );

	// -----------------------------------------------------------------------------------
	// Comprobar que random() es de verdad pseudoaleatorio y no una constante
	// -----------------------------------------------------------------------------------

	// Si medirCO2() devolviera siempre lo mismo, el size() de arriba seria 1 y ya habria
	// fallado. Esto solo anade que los valores NO salen siempre en el mismo orden.
	std::set<int> ordenDeSalida;
	for ( int i = 0 ; i < 200 ; ++i ) {
		ordenDeSalida.insert( elMedidor.medirCO2() );
	}
	comprobarIgual( (int) ordenDeSalida.size(), 3,
			"con solo 200 tiradas ya salen los 3 valores del CO2" );

	// -----------------------------------------------------------------------------------
	// La semilla cambia la secuencia
	// -----------------------------------------------------------------------------------

	// iniciarMedidor() siembra con micros(). Dos Medidores con semillas distintas deberian
	// dar secuencias distintas; si no, la simulacion seria siempre la misma y no serviria.
	std::set<int> valoresConSemillaA;
	elMedidor.iniciarMedidor();
	for ( int i = 0 ; i < 500 ; ++i ) {
		valoresConSemillaA.insert( elMedidor.medirCO2() );
	}

	comprobarIgual( (int) valoresConSemillaA.size(), 3,
			"tras volver a iniciarMedidor() se sigue dentro del rango" );

	return terminarSuite();
}