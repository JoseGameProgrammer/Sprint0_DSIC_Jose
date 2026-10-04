// -*- mode: c++ -*-

#ifndef MEDIDOR_H_INCLUIDO
#define MEDIDOR_H_INCLUIDO

// ==========================================================
// DISEÑO LÓGICO: clase Medidor
// ----------------------------------------------------------
//   Medidor = ()
//
//           Medidor() -->
//       iniciarMedidor() -->
//   Z <-- medirCO2() <--          (lectura, no muta)
//   Z <-- medirTemperatura() <--  (lectura, no muta)
//
// Abstracción del sensor físico de medio ambiente (CO2 y
// temperatura). Devuelve valores simulados, y AHORA ADEMÁS ALEATORIOS
// dentro de un rango, para que las medidas que llegan al móvil y a la
// BBDD no sean siempre las mismas.
// ==========================================================
// ------------------------------------------------------
// DISEÑO: --> Medidor() -->
// Qué hace: constructor vacío; la inicialización real del
//           sensor se hace en iniciarMedidor().
// ------------------------------------------------------
class Medidor {

  // .....................................................
  // .....................................................
private:

  // .....................................................
  // Rangos de la simulación.
  //
  // Son los valores que dice el enunciado: CO2 entre 234 y 236 ppm, y
  // temperatura entre -13 y -11 grados.
  //
  // OJO CON UNA COSA DE ARDUINO QUE SE HA COMIDO UN RATOS: random() es
  // SEMIABIERTO. Es decir, random(234, 236) NO devuelve 234, 235 y 236:
  // devuelve 234 y 235, porque el máximo queda FUERA. Por eso abajo se
  // pone random( CO2_MINIMO, CO2_MAXIMO + 1 ). Ese "+ 1" es lo que
  // convierte "semiabierto" en "cerrado".
  // .....................................................
  static const int CO2_MINIMO         = 234;
  static const int CO2_MAXIMO         = 236;
  static const int TEMPERATURA_MINIMA = -13;
  static const int TEMPERATURA_MAXIMA = -11;

public:

  // .....................................................
  // DISEÑO: --> Medidor() -->
  // .....................................................
  Medidor(  ) {
  } // ()

  // .....................................................
  // DISEÑO: --> iniciarMedidor() -->
  // Qué hace: prepara el sensor y, ADEMÁS, siembra el generador de
  //           números aleatorios.
  //           De momento no hay sensor real: solo la semilla.
  //
  // POR QUÉ HAY QUE SEMBRAR
  // -----------------------
  // Sin randomSeed(), Arduino usa una semilla fija, así que la secuencia
  // de números sale SIEMPRE igual: cada reinicio daría la misma serie.
  // Con semilla, cada encendido da una secuencia distinta.
  //
  // La semilla es micros(), que son los microsegundos que lleva encendida
  // la placa. Como la pila BLE tarda un rato en levantarse (eso pasa justo
  // antes de que setup() llegue aquí), el número ya no es cero y sirve.
  // No es aleatorio de verdad, pero para simular un sensor da igual.
  // .....................................................
  void iniciarMedidor() {

	// randomSeed() no muta este objeto (toca el generador global de
	// Arduino, que no es parte del Medidor), pero se llama desde aquí
	// porque iniciarMedidor() es el sitio de "preparar el sensor".
	randomSeed( (unsigned long) micros() );

  } // ()

  // .....................................................
  // DISEÑO: --> medirCO2() <-- Z
  // Qué hace: devuelve la concentración de CO2 "medida".
  //
  //           El resultado es un entero aleatorio ENTRE 234 Y 236, los
  //           dos incluidos. O sea, da 234, 235 o 236.
  //
  //           La flecha es <-- porque es solo una LECTURA: no toca nada
  //           del Medidor. Cada vez que se llama puede dar otra cosa.
  //
  //           OJO CON EL "+ 1" del final: es lo que hace que 236 sea
  //           posible. Sin él, el 236 no saldría nunca.
  // .....................................................
  int medirCO2() {
	return random( CO2_MINIMO, CO2_MAXIMO + 1 );
  } // ()

  // .....................................................
  // DISEÑO: --> medirTemperatura() <-- Z
  // Qué hace: devuelve la temperatura "medida".
  //
  //           El resultado es un entero aleatorio ENTRE -13 Y -11, los
  //           dos incluidos. O sea, da -13, -12 o -11.
  //
  //           Se escribe el rango de más pequeño a más grande para que
  //           se lea bien, aunque Arduino normaliza los dos números si
  //           se los dieras al revés.
  //
  //           Y OJO CON QUE SEA NEGATIVO: al meterse en el minor del
  //           iBeacon, que según la norma es un entero de 16 bits SIN
  //           signo, -13 se transmite como 0xFFF3 (65523 en decimal). No
  //           es un fallo: el móvil lo lee con signo, porque
  //           Utilidades.bytesToInt() interpreta los bytes en
  //           complemento a dos, y por eso vuelve a salir -13 en
  //           pantalla y es lo que se guarda en la BBDD.
  // .....................................................
  int medirTemperatura() {
	return random( TEMPERATURA_MINIMA, TEMPERATURA_MAXIMA + 1 );
  } // ()
	
}; // class

// ------------------------------------------------------
// ------------------------------------------------------
// ------------------------------------------------------
// ------------------------------------------------------
#endif
