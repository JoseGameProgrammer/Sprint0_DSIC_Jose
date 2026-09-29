// -*- mode: c++ -*-

#ifndef PUBLICADOR_H_INCLUIDO
#define PUBLICADOR_H_INCLUIDO
// ==========================================================
// DISEÑO LÓGICO: clase Publicador
// ----------------------------------------------------------
//   Publicador = ( beaconUUID: [Z]_16, laEmisora: EmisoraBLE,
//                  RSSI: Z )
//   MedicionesID = { CO2=11, TEMPERATURA=12, RUIDO=13 }
//
//   valor: Z, contador: N, tiempo: N --> publicarCO2() -->
//   valor: Z, contador: N, tiempo: N --> publicarTemperatura() -->
//                                encenderEmisora() -->
//                                        Publicador() -->
//
// Pone una medición "en el aire": emite un anuncio iBeacon cuyo
// campo major codifica (tipo de medición, nº de muestra) y cuyo
// campo minor contiene el valor medido.
// ==========================================================
// --------------------------------------------------------------
// --------------------------------------------------------------
class Publicador {

  // ............................................................
  // ............................................................
private:

  // UUID de 16 bytes de la baliza (identifica este emisor).
  uint8_t beaconUUID[16] = { 
	'E', 'P', 'S', 'G', '-', 'G', 'T', 'I', 
	'-', 'P', 'R', 'O', 'Y', '-', '3', 'A'
	};

  // ............................................................
  // ............................................................
public:
  // La emisora BLE que posee el publicador
  EmisoraBLE laEmisora {
	"GTI-3A", //  nombre emisora
	  0x004c, // fabricanteID (Apple)
	  4 // txPower
	  };
  
  const int RSSI = -53; // por poner algo, de momento no lo uso

  // ............................................................
  // ............................................................
public:

  // ............................................................
  // Enumeración interna: identificadores de magnitud que se
  // codifican en el byte alto del campo "major".
  // ............................................................
  enum MedicionesID  {
	CO2 = 11,
	TEMPERATURA = 12,
	RUIDO = 13
  };

  // ............................................................
  // DISEÑO: --> Publicador() -->
  // Qué hace: constructor. NO enciende la emisora aquí (el
  //           encendido se hará desde setup() con encenderEmisora).
  // ............................................................
  Publicador( ) {
	// ATENCION: no hacerlo aquí. (*this).laEmisora.encenderEmisora();
	// Pondremos un método para llamarlo desde el setup() más tarde
  } // ()

  // ............................................................
  // DISEÑO: --> encenderEmisora() -->
  // Qué hace: arranca la pila BLE (Bluefruit.begin()).
  // ............................................................
  void encenderEmisora() {
	(*this).laEmisora.encenderEmisora();
  } // ()

  // ............................................................
  // DISEÑO: valorCO2: Z, contador: N, tiempoEspera: N
  //                               --> publicarCO2() -->
  // Qué hace: publica la medición de CO2 como anuncio iBeacon.
  //   major = (CO2<<8) | contador  (tipo de medición + nº muestra)
  //   minor = valorCO2
  //   espera `tiempoEspera` ms y detiene el anuncio.
  // ............................................................
  void publicarCO2( int16_t valorCO2, uint8_t contador,
					long tiempoEspera ) {

	//
	// 1. empezamos anuncio
	//
	uint16_t major = (MedicionesID::CO2 << 8) + contador;
	(*this).laEmisora.emitirAnuncioIBeacon( (*this).beaconUUID, 
											major,
											valorCO2, // minor
											(*this).RSSI // rssi
									);
	//
	// 2. esperamos el tiempo que nos digan
	//
	esperar( tiempoEspera );

	//
	// 3. paramos anuncio
	//
	(*this).laEmisora.detenerAnuncio();
  } // ()

  // ............................................................
  // DISEÑO: valorTemperatura: Z, contador: N, tiempoEspera: N
  //                               --> publicarTemperatura() -->
  // Qué hace: igual que publicarCO2 pero para la temperatura
  //           (major codifica TEMPERATURA y el nº de muestra).
  // ............................................................
  void publicarTemperatura( int16_t valorTemperatura,
							uint8_t contador, long tiempoEspera ) {

	uint16_t major = (MedicionesID::TEMPERATURA << 8) + contador;
	(*this).laEmisora.emitirAnuncioIBeacon( (*this).beaconUUID, 
											major,
											valorTemperatura, // minor
											(*this).RSSI // rssi
									);
	esperar( tiempoEspera );

	(*this).laEmisora.detenerAnuncio();
  } // ()
	
}; // class

// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
#endif
