
// -*- mode: c++ -*-

#ifndef PUERTO_SERIE_H_INCLUIDO
#define PUERTO_SERIE_H_INCLUIDO

// ==========================================================
// DISEÑO LÓGICO: clase PuertoSerie
// ----------------------------------------------------------
//   PuertoSerie = ( baudios: N )
//
//   baudios: N --> PuertoSerie() -->   (abre el Serial)
//                     esperarDisponible() -->
//   mensaje: T -->    escribir() -->
//
// Abstracción del puerto serie del Arduino: oculta Serial.
// ==========================================================
// ----------------------------------------------------------
// DISEÑO: baudios: N --> PuertoSerie() -->
// Qué hace: constructor. Abre el puerto serie a la velocidad
//           indicada (p. ej. 115200).
// ----------------------------------------------------------
class PuertoSerie  {

public:
  // .........................................................
  // .........................................................
  PuertoSerie (long baudios) {
	Serial.begin( baudios );
	// mejor no poner esto aquí: while ( !Serial ) delay(10);   
  } // ()

  // .........................................................
  // DISEÑO: --> esperarDisponible() -->
  // Qué hace: bloquea hasta que el puerto serie esté listo
  //           (evita perder los primeros mensajes al arrancar).
  // .........................................................
  void esperarDisponible() {

	while ( !Serial ) {
	  delay(10);   
	}

  } // ()

  // .........................................................
  // DISEÑO: mensaje: T --> escribir() -->
  // Qué hace: escribe un mensaje por el puerto serie.
  //           Plantilla: sirve para textos y números.
  // .........................................................
  template<typename T>
  void escribir (T mensaje) {
	Serial.print( mensaje );
  } // ()
  
}; // class PuertoSerie

// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
#endif
