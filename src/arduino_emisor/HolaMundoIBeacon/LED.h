// -*- mode: c++ -*-

#ifndef LED_H_INCLUIDO
#define LED_H_INCLUIDO

// ----------------------------------------------------------
// ==========================================================
// DISEÑO LÓGICO: clase LED y utilidad esperar()
// ----------------------------------------------------------
//   LED = ( numero: N, encendido: B )
//
//   n: N --> LED() -->
//             encender() -->       (mutación ->)
//               apagar() -->
//             alternar() -->
//   t: N --> brillar() -->
//
//   t: N --> esperar() -->   <- delay(t) del Arduino
// ==========================================================
// ----------------------------------------------------------
// DISEÑO: tiempo: N --> esperar() -->
// Qué hace: pausa el programa los milisegundos indicados
// ----------------------------------------------------------
void esperar (long tiempo) {
  delay (tiempo);
}

// ----------------------------------------------------------
//  clase LED Gestiona un pin digitalde salida como si fuera una 
//  luz encendida/apagada.
// ----------------------------------------------------------
class LED {
private:
  int numeroLED;
  bool encendido;
public:

  // .........................................................
  // DISEÑO: n: N --> LED() -->
  // Qué hace: constructor. Configura el pin como salida y lo
  //           deja apagado.
  // .........................................................
  LED (int numero)
	: numeroLED (numero), encendido(false)
  {
	pinMode(numeroLED, OUTPUT);
	apagar ();
  }

  // .........................................................
  // DISEÑO: --> encender() -->
  // Qué hace: pone el pin a HIGH (enciende) y recuerda el estado.
  // .........................................................
  void encender () {
	digitalWrite(numeroLED, HIGH); 
	encendido = true;
  }

  // .........................................................
  // DISEÑO: --> apagar() -->
  // Qué hace: pone el pin a LOW (apaga) y recuerda el estado.
  // .........................................................
  void apagar () {
	  digitalWrite(numeroLED, LOW);
	  encendido = false;
  }

  // .........................................................
  // DISEÑO: --> alternar() -->
  // Qué hace: invierte el estado actual (si estaba encendido lo
  //           apaga y viceversa).
  // .........................................................
  void alternar () {
	if (encendido) {
	  apagar();
	} else {
	  encender ();
	}
  } // ()

  // .........................................................
  // DISEÑO: tiempo: N --> brillar() -->
  // Qué hace: enciende el LED, espera `tiempo` ms y lo apaga.
  // .........................................................
  void brillar (long tiempo) {
	encender ();
	esperar(tiempo); 
	apagar ();
  }
}; // class

// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
#endif
