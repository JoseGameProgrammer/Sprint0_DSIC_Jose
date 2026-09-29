// -*- mode: c++ -*-

#ifndef EMISORA_H_INCLUIDO
#define EMISORA_H_INCLUIDO

// Buena introducción: https://learn.adafruit.com/introduction-to-bluetooth-low-energy/gap
// https://os.mbed.com/blog/entry/BLE-Beacons-URIBeacon-AltBeacons-iBeacon/

// fuente: https://www.instructables.com/id/Beaconeddystone-and-Adafruit-NRF52-Advertise-Your-/
// https://github.com/nkolban/ESP32_BLE_Arduino/blob/master/src/BLEBeacon.h

// https://os.mbed.com/blog/entry/BLE-Beacons-URIBeacon-AltBeacons-iBeacon/
// https://learn.adafruit.com/bluefruit-nrf52-feather-learning-guide/bleadvertising

// ==========================================================
// DISEÑO LÓGICO: clase EmisoraBLE
// ----------------------------------------------------------
//   EmisoraBLE = ( nombreEmisora: Text, fabricanteID: N, txPower: Z )
//   CallbackConexionEstablecida = ( connHandle: N ) -->
//   CallbackConexionTerminada   = ( connHandle: N, reason: N ) -->
//
//   nombre: Text, fab: N, tx: Z --> EmisoraBLE() -->
//                          encenderEmisora() -->
//   cbce, cbct                    --> encenderEmisora() -->
//                            detenerAnuncio() -->
//                     B <-- estaAnunciando() <--
//   uuid, major, minor, rssi     --> emitirAnuncioIBeacon() -->
//   carga, tamanyo               --> emitirAnuncioIBeaconLibre() -->
//   servicio             --> B <-- anyadirServicio() -->
//   servicio, ...carac   --> B <-- anyadirServicioConSusCaracteristicas() -->
//   servicio, ...carac   --> B <-- anyadirServicioConSusCaracteristicasYActivar() -->
//   cb                           --> instalarCallbackConexionEstablecida() -->
//   cb                           --> instalarCallbackConexionTerminada() -->
//   connHandle -> BLEConnection <-- getConexion() <--
//
// Envuelve la biblioteca Bluefruit de Adafruit para emitir
// anuncios BLE, tanto iBeacon estándar como "carga libre".
// ==========================================================
// ----------------------------------------------------------
// ----------------------------------------------------------
#include "ServicioEnEmisora.h"

// ----------------------------------------------------------
// clase EmisoraBLE 
// ----------------------------------------------------------
class EmisoraBLE {
private:

  const char * nombreEmisora;
  const uint16_t fabricanteID;
  const int8_t txPower;

public:

  // .........................................................
  // Tipos de callback que gestionan la conexión GATT.
  // .........................................................
  using CallbackConexionEstablecida = void ( uint16_t connHandle );
  using CallbackConexionTerminada = void ( uint16_t connHandle, uint8_t reason);

  // .........................................................
  // DISEÑO: nombre: Text, fab: N, tx: Z --> EmisoraBLE() -->
  // Qué hace: guarda la configuración de la emisora.
  // .........................................................
  EmisoraBLE( const char * nombreEmisora_, const uint16_t fabricanteID_,
			  const int8_t txPower_ ) 
	:
	nombreEmisora( nombreEmisora_ ) ,
	fabricanteID( fabricanteID_ ) ,
	txPower( txPower_ )
  {
	// no encender ahora la emisora, tal vez sea por el println()
	// que hace que todo falle si lo llamo en el contructor
	// ( = antes que configuremos Serial )
	// No parece que sea por el println,
	// por tanto NO_encenderEmisora();
  } // ()

  // .........................................................
  // DISEÑO: --> encenderEmisora() -->
  // Qué hace: inicializa la pila de Bluetooth (Bluefruit.begin())
  //           y se asegura de que no quede ningún anuncio previo.
  // .........................................................
  void encenderEmisora() {
	// Serial.println ( "Bluefruit.begin() " );
	 Bluefruit.begin(); 

	 // por si acaso:
	 (*this).detenerAnuncio();
  } // ()

  // .........................................................
  // DISEÑO: cbce: CallbackConexionEstablecida,
  //         cbct: CallbackConexionTerminada --> encenderEmisora() -->
  // Qué hace: enciende la emisora y, además, instala los callbacks
  //           de conexión/desconexión antes de anunciar.
  // .........................................................
  void encenderEmisora( CallbackConexionEstablecida cbce,
						CallbackConexionTerminada cbct ) {

	encenderEmisora();

	instalarCallbackConexionEstablecida( cbce );
	instalarCallbackConexionTerminada( cbct );

  } // ()

  // .........................................................
  // DISEÑO: --> detenerAnuncio() -->
  // Qué hace: detiene el anuncio BLE actual (si se está emitiendo).
  // .........................................................
  void detenerAnuncio() {

	if ( (*this).estaAnunciando() ) {
	  // Serial.println ( "Bluefruit.Advertising.stop() " );
	  Bluefruit.Advertising.stop(); 
	}

  }  // ()
  
  // .........................................................
  // DISEÑO: --> estaAnunciando() <-- B
  // Qué hace: devuelve true si la emisora está emitiendo anuncios.
  // .........................................................
  bool estaAnunciando() {
	return Bluefruit.Advertising.isRunning();
  } // ()

  // .........................................................
  // DISEÑO: beaconUUID: [Z]_16, major: Z, minor: Z, rssi: Z
  //                                   --> emitirAnuncioIBeacon() -->
  // Qué hace: construye y difunde un anuncio iBeacon estándar de
  //           30 bytes: prefijo Apple(9) + uuid(16) + major(2) +
  //           minor(2) + txPower(1). Major/minor transportan aquí
  //           la medición del emisor.
  // .........................................................
  void emitirAnuncioIBeacon( uint8_t * beaconUUID, int16_t major, int16_t minor, uint8_t rssi ) {

	//
	//
	//
	(*this).detenerAnuncio();
	
	//
	// creo el beacon 
	//
	BLEBeacon elBeacon( beaconUUID, major, minor, rssi );
	elBeacon.setManufacturer( (*this).fabricanteID );

	//
	// parece que esto debe ponerse todo aquí
	//

	Bluefruit.setTxPower( (*this).txPower );
	Bluefruit.setName( (*this).nombreEmisora );
	Bluefruit.ScanResponse.addName(); // para que envíe el nombre de emisora (?!)

	//
	// pongo el beacon
	//
	Bluefruit.Advertising.setBeacon( elBeacon );

	//
	// ? qué valorers poner aquí
	//
	Bluefruit.Advertising.restartOnDisconnect(true); // no hace falta, pero lo pongo
	Bluefruit.Advertising.setInterval(100, 100);    // in unit of 0.625 ms

	//
	// empieza el anuncio, 0 = tiempo indefinido (ya lo pararán)
	//
	Bluefruit.Advertising.start( 0 ); 
	
  } // ()

  // .........................................................
  //
  // Ejemplo de Beacon (31 bytes)
  //
  // https://os.mbed.com/blog/entry/BLE-Beacons-URIBeacon-AltBeacons-iBeacon/
  //
  // The iBeacon Prefix contains the hex data : 0x0201061AFF004C0215. This breaks down as follows:
  //
  // 0x020106 defines the advertising packet as BLE General Discoverable and BR/EDR high-speed incompatible.
  // Effectively it says this is only broadcasting, not connecting.
  //
  // 0x1AFF says the following data is 26 bytes long and is Manufacturer Specific Data.
  //
  // 0x004C is Apple’s Bluetooth Sig ID and is the part of this spec that makes it Apple-dependent.
  //
  // 0x02 is a secondary ID that denotes a proximity beacon, which is used by all iBeacons.
  //
  // 0x15 defines the remaining length to be 21 bytes (16+2+2+1).
  //
  // Por ejemmplo:
  //
  // 1. prefijo: 9bytes
  //       0x02, 0x01, 0x06,       // advFlags 3bytes
  //       0x1a, 0xff,             // advHeader 2 (0x1a = 26 = 25(lenght de 0x4c a 0xca)+1)   0xFF -> BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA
  //       0x4c, 0x00,             // companyID 2bytes
  //       0x02,                   // ibeacon type 1 byte
  //       0x15,                   // ibeacon length 1 byte (dec=21 lo que va a continuación: desde  la 'f' hasta 0x01)
  // 
  // 2. uuid: 16bytes
  // 'f', 'i', 's', 't', 'r', 'o', 'f', 'i', 's', 't', 'r', 'o', 0xa7, 0x10, 0x96, 0xe0
  // 
  // 2 major: 2bytes
  // 0x04, 0xd2,
  // 
  // minor: 2bytes
  // 0x10, 0xe1,
  // 
  // 0xca, // tx power : 1bytes
  //
  // 0x01, // este es el byte 31 = BLE_GAP_ADV_SET_DATA_SIZE_MAX, parece que sobra
  //
  // .........................................................
  // DISEÑO: carga: Text, tamanyoCarga: N
  //                                   --> emitirAnuncioIBeaconLibre() -->
  // Qué hace: emite como un iBeacon (con el prefijo Apple fijo)
  //           una "carga libre" de hasta 21 bytes en el hueco que
  //           normalmente ocuparían uuid+major+minor+txPower.
  //           Se usa para probar qué bytes transporta el paquete
  //           (los 21 últimos bytes del anuncio de 31 bytes).
  // .........................................................
  void emitirAnuncioIBeaconLibre( const char * carga, const uint8_t tamanyoCarga ) {

	(*this).detenerAnuncio(); 

	Bluefruit.Advertising.clearData();
	Bluefruit.ScanResponse.clearData(); // hace falta?

	// Bluefruit.setTxPower( (*this).txPower ); creo que no lo pongo porque es uno de los bytes de la parte de carga que utilizo
	Bluefruit.setName( (*this).nombreEmisora );
	Bluefruit.ScanResponse.addName();

	Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);

	// con este parece que no va  !
	// Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE);

	//
	// hasta ahora habrá, supongo, ya puestos los 5 primeros bytes. Efectivamente.
	// Falta poner 4 bytes fijos (company ID, beacon type, longitud) y 21 de carga
	//
	uint8_t restoPrefijoYCarga[4+21] = {
	  0x4c, 0x00, // companyID 2
	  0x02, // ibeacon type 1byte
	  21, // ibeacon length 1byte (dec=21)  longitud del resto // 0x15 // ibeacon length 1byte (dec=21)  longitud del resto
	  '-', '-', '-', '-', 
	  '-', '-', '-', '-', 
	  '-', '-', '-', '-', 
	  '-', '-', '-', '-', 
	  '-', '-', '-', '-', 
	  '-'
	};

	//
	// addData() hay que usarlo sólo una vez. Por eso copio la carga
	// en el anterior array, donde he dejado 21 sitios libres
	//
	memcpy( &restoPrefijoYCarga[4], &carga[0], ( tamanyoCarga > 21 ? 21 : tamanyoCarga ) ); 

	//
	// copio la carga para emitir
	//
	Bluefruit.Advertising.addData( BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA,
								   &restoPrefijoYCarga[0],
								   4+21 );

	//
	// ? qué valores poner aquí ?
	//
	Bluefruit.Advertising.restartOnDisconnect(true);
	Bluefruit.Advertising.setInterval(100, 100);    // in unit of 0.625 ms

	Bluefruit.Advertising.setFastTimeout( 1 );      // number of seconds in fast mode
	//
	// empieza el anuncio, 0 = tiempo indefinido (ya lo pararán)
	//
	Bluefruit.Advertising.start( 0 ); 

	Globales::elPuerto.escribir( "emitiriBeacon libre  Bluefruit.Advertising.start( 0 );  \n");
  } // ()

  // .........................................................
  // DISEÑO: servicio: ServicioEnEmisora --> B
  // Qué hace: añade al contenido del anuncio un servicio GATT
  //           (se convierte implícitamente a BLEService). Devuelve
  //           si se pudo añadir.
  // .........................................................
  bool anyadirServicio( ServicioEnEmisora & servicio ) {

	Globales::elPuerto.escribir( " Bluefruit.Advertising.addService( servicio ); \n");

	bool r = Bluefruit.Advertising.addService( servicio );

	if ( ! r ) {
	  Serial.println( " SERVICION NO AÑADIDO \n");
	}
	

	return r;
	 // nota: uso conversión de tipo de servicio (ServicioEnEmisora) a BLEService
	 // para addService()
  } // ()

  
  // .........................................................
  // DISEÑO (caso base): servicio: ServicioEnEmisora --> B
  // Qué hace: caso base de la recursión variádica: añade el
  //           servicio sin características extra.
  // .........................................................
  bool anyadirServicioConSusCaracteristicas( ServicioEnEmisora & servicio ) { 
	return (*this).anyadirServicio( servicio );
  } // 

  // .........................................................
  // DISEÑO (caso recursivo): servicio: ServicioEnEmisora,
  //                          ...caract: Caracteristica --> B
  // Qué hace: añade una característica al servicio y sigue con el
  //           resto de la lista variádica.
  // .........................................................
  template <typename ... T>
  bool anyadirServicioConSusCaracteristicas( ServicioEnEmisora & servicio,
											 ServicioEnEmisora::Caracteristica & caracteristica,
											 T& ... restoCaracteristicas) {

	servicio.anyadirCaracteristica( caracteristica );

	return anyadirServicioConSusCaracteristicas( servicio, restoCaracteristicas... );
	
  } // ()

  // .........................................................
  // DISEÑO: servicio: ServicioEnEmisora, ...caract: Caracteristica --> B
  // Qué hace: igual que el anterior pero, al terminar, activa el
  //           servicio (lo registra en la pila BLE).
  // .........................................................
  template <typename ... T>
  bool anyadirServicioConSusCaracteristicasYActivar( ServicioEnEmisora & servicio,
													 // ServicioEnEmisora::Caracteristica & caracteristica,
													 T& ... restoCaracteristicas) {

	bool r = anyadirServicioConSusCaracteristicas( servicio, restoCaracteristicas... );

	servicio.activarServicio();

	return r;
	
  } // ()

  // .........................................................
  // DISEÑO: cb: CallbackConexionEstablecida
  //                --> instalarCallbackConexionEstablecida() -->
  // Qué hace: registra el callback que se invocará cuando un
  //           cliente BLE se conecte a la emisora.
  // .........................................................
  void instalarCallbackConexionEstablecida( CallbackConexionEstablecida cb ) {
	Bluefruit.Periph.setConnectCallback( cb );
  } // ()

  // .........................................................
  // DISEÑO: cb: CallbackConexionTerminada
  //                --> instalarCallbackConexionTerminada() -->
  // Qué hace: registra el callback que se invocará cuando un
  //           cliente BLE se desconecte de la emisora.
  // .........................................................
  void instalarCallbackConexionTerminada( CallbackConexionTerminada cb ) {
	Bluefruit.Periph.setDisconnectCallback( cb );
  } // ()

  // .........................................................
  // DISEÑO: connHandle: N --> getConexion() <-- BLEConnection
  // Qué hace: devuelve el objeto de conexión BLE asociado a un
  //           identificador de conexión dado.
  // .........................................................
  BLEConnection * getConexion( uint16_t connHandle ) {
	return Bluefruit.Connection( connHandle );
  } // ()

}; // class

#endif

// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------

