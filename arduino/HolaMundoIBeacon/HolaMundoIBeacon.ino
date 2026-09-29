// -*-c++-*-

// https://learn.sparkfun.com/tutorials/nrf52840-development-with-arduino-and-circuitpython

// https://stackoverflow.com/questions/29246805/can-an-ibeacon-have-a-data-payload

// ==============================================================
// DISEÑO LÓGICO DEL MÓDULO EMISOR iBeacon (Arduino)
// --------------------------------------------------------------
// Este programa convierte una placa NRF52840 en un emisor de
// balizas iBeacon que "publica" en el aire mediciones de sensores.
//
//   -> setup() -->  inicia puerto serie, emisora BLE y medidor
//   -> loop()  -->  bucle infinito: mide CO2 y temperatura y los
//                   difunde como anuncios iBeacon
//
// Objetos globales (namespace Globales):
//   elLED: LED, elPuerto: PuertoSerie, elPublicador: Publicador,
//   elMedidor: Medidor, y contador Loop::cont
// ==============================================================
// --------------------------------------------------------------
#include <bluefruit.h>

#undef min // vaya tela, están definidos en bluefruit.h y  !
#undef max // colisionan con los de la biblioteca estándar

// --------------------------------------------------------------
// --------------------------------------------------------------
#include "LED.h"
#include "PuertoSerie.h"

// --------------------------------------------------------------
// --------------------------------------------------------------
namespace Globales {
  
  LED elLED ( /* NUMERO DEL PIN LED = */ 7 );

  PuertoSerie elPuerto ( /* velocidad = */ 115200 ); // 115200 o 9600 o ...

  // Serial1 en el ejemplo de Curro creo que es la conexión placa-sensor 
};

// --------------------------------------------------------------
// --------------------------------------------------------------
#include "EmisoraBLE.h"
#include "Publicador.h"
#include "Medidor.h"


// --------------------------------------------------------------
// --------------------------------------------------------------
namespace Globales {

  Publicador elPublicador;

  Medidor elMedidor;

}; // namespace

// --------------------------------------------------------------
// --------------------------------------------------------------
// DISEÑO: --> inicializarPlaquita() -->
// Qué hace: preparación adicional de la placa antes del arranque.
//           De momento no hace nada (punto de extensión).
// --------------------------------------------------------------
void inicializarPlaquita () {

  // de momento nada

} // ()

// --------------------------------------------------------------
// DISEÑO: --> setup() -->
// Qué hace: arranque del sistema. Espera al puerto serie,
//           enciende la emisora BLE, inicializa el medidor y da
//           un margen de tiempo para que todo se estabilice.
// --------------------------------------------------------------
// setup()
// --------------------------------------------------------------
void setup() {

  //Globales::elPuerto.esperarDisponible(); // Lo comento para que funcione sin abrir el serial

  // 
  // 
  // 
  inicializarPlaquita();

  // Suspend Loop() to save power
  // suspendLoop();

  // 
  // 
  // 
  Globales::elPublicador.encenderEmisora();

  // Globales::elPublicador.laEmisora.pruebaEmision();
  
  // 
  // 
  // 
  Globales::elMedidor.iniciarMedidor();

  // 
  // 
  // 
  esperar( 1000 );

  Globales::elPuerto.escribir( "---- setup(): fin ---- \n " );

} // setup ()

// --------------------------------------------------------------
// DISEÑO: --> lucecitas() -->
// Qué hace: rutina de "señal de vida" por el LED: parpadea varias
//           veces encendiendo/apagando con diferentes tiempos.
// --------------------------------------------------------------
inline void lucecitas() {
  using namespace Globales;

  elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  Globales::elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  Globales::elLED.brillar( 1000 ); // 1000 encendido
  esperar ( 1000 ); //  100 apagado
} // ()

// --------------------------------------------------------------
// loop ()
// --------------------------------------------------------------
namespace Loop {
  uint8_t cont = 0;
};

// ..............................................................
// DISEÑO: --> loop() -->
// Qué hace: bucle principal infinito. Incrementa el contador,
//           hace la señal de vida (LED), mide CO2 y temperatura y
//           las publica como iBeacon, y finalmente prueba el envío
//           de una carga libre de 21 bytes (sin formato iBeacon).
// ..............................................................
void loop () {

  using namespace Loop;
  using namespace Globales;

  cont++;

  elPuerto.escribir( "\n---- loop(): empieza " );
  elPuerto.escribir( cont );
  elPuerto.escribir( "\n" );


  lucecitas();

  // 
  // mido y publico
  // 
  int valorCO2 = elMedidor.medirCO2();
  
  elPublicador.publicarCO2( valorCO2,
							cont,
							1000 // intervalo de emisión
							);
  
  // 
  // mido y publico
  // 
  int valorTemperatura = elMedidor.medirTemperatura();
  
  elPublicador.publicarTemperatura( valorTemperatura, 
									cont,
									1000 // intervalo de emisión
									);

  // 
  // prueba para emitir un iBeacon y poner
  // en la carga (21 bytes = uuid 16 major 2 minor 2 txPower 1 )
  // lo que queramos (sin seguir dicho formato)
  // 
  // Al terminar la prueba hay que hacer Publicador::laEmisora privado
  // 
  // elPublicador.laEmisora.emitirAnuncioIBeaconLibre ( "MolaMolaMolaMolaMolaM", 21 );
  elPublicador.laEmisora.emitirAnuncioIBeaconLibre ( "MolaMolaMolaMolaMolaM", 21 );

  esperar( 2000 );

  elPublicador.laEmisora.detenerAnuncio();
  
  // 
  // 
  // 
  elPuerto.escribir( "---- loop(): acaba **** " );
  elPuerto.escribir( cont );
  elPuerto.escribir( "\n" );
  
} // loop ()
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
