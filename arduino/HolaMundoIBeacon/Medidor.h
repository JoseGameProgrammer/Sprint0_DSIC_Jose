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
// temperatura). Devuelve valores simulados.
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

public:

  // .....................................................
  // DISEÑO: --> Medidor() -->
  // .....................................................
  Medidor(  ) {
  } // ()

  // .....................................................
  // DISEÑO: --> iniciarMedidor() -->
  // Qué hace: prepara el sensor (calibración, pines...).
  //           De momento vacío.
  // .....................................................
  void iniciarMedidor() {
	// las cosas que no se puedan hacer en el constructor, if any
  } // ()

  // .....................................................
  // DISEÑO: --> medirCO2() <-- Z
  // Qué hace: devuelve la concentración de CO2 medida (ficticia).
  // .....................................................
  int medirCO2() {
	return 235;
  } // ()

  // .....................................................
  // DISEÑO: --> medirTemperatura() <-- Z
  // Qué hace: devuelve la temperatura medida (ficticia).
  // .....................................................
  int medirTemperatura() {
	return -12; // qué frío !
  } // ()
	
}; // class

// ------------------------------------------------------
// ------------------------------------------------------
// ------------------------------------------------------
// ------------------------------------------------------
#endif
