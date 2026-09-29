// -*- mode: c++ -*-

#ifndef SERVICIO_EMISORA_H_INCLUIDO
#define SERVICIO_EMISORA_H_INCLUIDO

// ==========================================================
// DISEÑO LÓGICO: servicio + características BLE de la emisora
// ----------------------------------------------------------
//   ServicioEnEmisora = ( uuidServicio: [Z]_16, elServicio: BLEService,
//                         lasCaracteristicas: [ Caracteristica ] )
//   Caracteristica = ( uuidCaracteristica: [Z]_16, laCaracteristica: BLECharacteristic )
//   CallbackCaracteristicaEscrita = ( connHandle: N, chr, data: [Z], len: N ) -->
//
// Funciones auxiliares:
//   p: [T]_n, n: N --> alReves() --> [T]_n           (invierte p en el sitio)
//   pString: Text, pUint: [Z]_16, tamMax: N --> stringAUint8AlReves() --> [Z]_16
//
// Un "servicio en la emisora" agrupa varias características BLE y
// permite registrarlas en la pila Bluefruit.
// ==========================================================
// ----------------------------------------------------
// ----------------------------------------------------
#include <vector>

// ----------------------------------------------------
// DISEÑO: p: [T]_n, n: N --> alReves() --> [T]_n
// Qué hace: utilidad genérica que invierte el orden de los
//           elementos de un array, "en el sitio" (sin copia),
//           y devuelve el mismo puntero. O(n/2) intercambios.
// ----------------------------------------------------
template< typename T >
T *  alReves( T * p, int n ) {
  T aux;

  for( int i=0; i < n/2; i++ ) {
	aux = p[i];
	p[i] = p[n-i-1];
	p[n-i-1] = aux;
  }
  return p;
} // ()

// ----------------------------------------------------
// DISEÑO: pString: Text, pUint: [Z]_16, tamMax: N
//                       --> stringAUint8AlReves() --> [Z]_16
// Qué hace: copia una cadena C dentro de un buffer de tamaño
//           tamMax pero escrita AL REVÉS (conforme a cómo la pila
//           Bluefruit espera los UUIDs de 16 bytes, primer byte el
//           menos significativo). Devuelve el buffer de destino.
// ----------------------------------------------------
uint8_t * stringAUint8AlReves( const char * pString, uint8_t * pUint, int tamMax ) {

	int longitudString =  strlen( pString );
	int longitudCopiar = ( longitudString > tamMax ? tamMax : longitudString );
	// copio nombreServicio -> uuidServicio pero al revés
	for( int i=0; i<=longitudCopiar-1; i++ ) {
	  pUint[ tamMax-i-1 ] = pString[ i ];
	} // for

	return pUint;
} // ()

// ----------------------------------------------------------
// clase ServicioEnEmisora
// ----------------------------------------------------------
class ServicioEnEmisora {

public:


  // .........................................................
  // Tipo del callback cuando un cliente BLE escribe en una
  // característica.
  // .........................................................
  using CallbackCaracteristicaEscrita = void ( uint16_t conn_handle,
											   BLECharacteristic * chr,
											   uint8_t * data, uint16_t len); 
  // .........................................................
  // Clase interna: una característica BLE dentro del servicio.
  // Caracteristica = ( uuidCaracteristica: [Z]_16, laCaracteristica )
  // .........................................................
  class Caracteristica {
  private:
	uint8_t uuidCaracteristica[16] = { // el uuid se copia aquí (al revés) a partir de un string-c
	  // least signficant byte, el primero
	  '0', '1', '2', '3', 
	  '4', '5', '6', '7', 
	  '8', '9', 'A', 'B', 
	  'C', 'D', 'E', 'F'
	};

	// 
	// Objeto BLE real gestionado por la biblioteca Bluefruit.
	// 
	BLECharacteristic laCaracteristica;

  public:

	// .........................................................
	// DISEÑO: nombre: Text --> Caracteristica() -->
	// Qué hace: construye la característica cuyo UUID se deriva del
	//           nombre dado (invertido). Sin propiedades definidas.
	// .........................................................
	Caracteristica( const char * nombreCaracteristica_ )
	  :
	  laCaracteristica( stringAUint8AlReves( nombreCaracteristica_, &uuidCaracteristica[0], 16 ) )
	{
	  
	} // ()

	// .........................................................
	// DISEÑO: nombre: Text, props: N, permisoRead: SecureMode_t,
	//         permisoWrite: SecureMode_t, tam: N --> Caracteristica() -->
	// Qué hace: construye la característica y le da propiedades,
	//           permisos de lectura/escritura y tamaño de datos.
	// .........................................................
	Caracteristica( const char * nombreCaracteristica_ ,
					uint8_t props,
					SecureMode_t permisoRead,
					SecureMode_t permisoWrite, 
					uint8_t tam ) 
	  :
	  Caracteristica( nombreCaracteristica_ ) // llamada al otro constructor
	{
	  (*this).asignarPropiedadesPermisosYTamanyoDatos( props, permisoRead, permisoWrite, tam );
	} // ()

  private:
	// .........................................................
	// DISEÑO: props: N --> asignarPropiedades() -->
	// Qué hace: fija las propiedades de la característica
	//           (CHR_PROPS_WRITE, CHR_PROPS_READ, CHR_PROPS_NOTIFY).
	// .........................................................
	void asignarPropiedades ( uint8_t props ) {
	  // no puedo escribir AUN si el constructor llama a esto: Serial.println( " laCaracteristica.setProperties( props ); ");
	  (*this).laCaracteristica.setProperties( props );
	} // ()

	// .........................................................
	// DISEÑO: permisoRead: SecureMode_t, permisoWrite: SecureMode_t
	//                              --> asignarPermisos() -->
	// Qué hace: fija los permisos de seguridad de acceso a la
	//           característica (SECMODE_OPEN, SECMODE_NO_ACCESS...).
	// .........................................................
	void asignarPermisos( SecureMode_t permisoRead, SecureMode_t permisoWrite ) {
	  // no puedo escribir AUN si el constructor llama a esto: Serial.println( "laCaracteristica.setPermission( permisoRead, permisoWrite ); " );
	  (*this).laCaracteristica.setPermission( permisoRead, permisoWrite );
	} // ()

	// .........................................................
	// DISEÑO: tam: N --> asignarTamanyoDatos() -->
	// Qué hace: fija la longitud máxima de datos de la característica.
	// .........................................................
	void asignarTamanyoDatos( uint8_t tam ) {
	  // no puedo escribir AUN si el constructor llama a esto: Serial.print( " (*this).laCaracteristica.setFixedLen( tam = " );
	  // no puedo escribir AUN si el constructor llama a esto: Serial.println( tam );
	  // (*this).laCaracteristica.setFixedLen( tam );
	  (*this).laCaracteristica.setMaxLen( tam );
	} // ()

  public:
	// .........................................................
	// DISEÑO: props: N, permisoRead, permisoWrite, tam: N
	//                          --> asignarPropiedadesPermisosYTamanyoDatos() -->
	// Qué hace: configura de una vez propiedades + permisos + tamaño.
	// .........................................................
	void asignarPropiedadesPermisosYTamanyoDatos( uint8_t props,
												 SecureMode_t permisoRead,
												 SecureMode_t permisoWrite, 
												 uint8_t tam ) {
	  asignarPropiedades( props );
	  asignarPermisos( permisoRead, permisoWrite );
	  asignarTamanyoDatos( tam );
	} // ()
												 

	// .........................................................
	// DISEÑO: str: Text --> escribirDatos() --> N
	// Qué hace: escribe la cadena dada como valor de la
	//           característica. Devuelve el nº de bytes escritos.
	// .........................................................
	uint16_t escribirDatos( const char * str ) {
	  // Serial.print( " return (*this).laCaracteristica.write( str  = " );
	  // Serial.println( str );

	  uint16_t r = (*this).laCaracteristica.write( str );

	  // Serial.print( ">>>Escritos " ); Serial.print( r ); Serial.println( " bytes con write() " );

	  return r;
	} // ()

	// .........................................................
	// DISEÑO: str: Text --> notificarDatos() --> N
	// Qué hace: envía la cadena a los clientes BLE suscritos a las
	//           notificaciones (CCCD). Devuelve el nº de bytes.
	// .........................................................
	uint16_t notificarDatos( const char * str ) {
	  
	  uint16_t r = laCaracteristica.notify( &str[0] );

	  return r;
	} //  ()

	// .........................................................
	// DISEÑO: cb: CallbackCaracteristicaEscrita
	//              --> instalarCallbackCaracteristicaEscrita() -->
	// Qué hace: registra el callback que se invoca cuando un cliente
	//           BLE escribe en esta característica.
	// .........................................................
	void instalarCallbackCaracteristicaEscrita( CallbackCaracteristicaEscrita cb ) {
	  (*this).laCaracteristica.setWriteCallback( cb );
	} // ()

	// .........................................................
	// DISEÑO: --> activar() -->
	// Qué hace: registra la característica en la pila Bluefruit
	//           (begin()) y notifica el error por pantalla.
	// .........................................................
	void activar() {
	  err_t error = (*this).laCaracteristica.begin();
	  Globales::elPuerto.escribir(  " (*this).laCaracteristica.begin(); error = " );
	  Globales::elPuerto.escribir(  error );
	} // ()

  }; // class Caracteristica
  
  // --------------------------------------------------------
  // --------------------------------------------------------
private:
  
  uint8_t uuidServicio[16] = { // el uuid se copia aquí (al revés) a partir de un string-c
	// least signficant byte, el primero
	'0', '1', '2', '3', 
	'4', '5', '6', '7', 
	'8', '9', 'A', 'B', 
	'C', 'D', 'E', 'F'
  };

  // Objeto BLE real del servicio.
  //
  BLEService elServicio;

  // Lista de las características del servicio.
  //
  std::vector< Caracteristica * > lasCaracteristicas;

public:
  
  // .........................................................
  // DISEÑO: nombre: Text --> ServicioEnEmisora() -->
  // Qué hace: construye el servicio cuyo UUID se deriva del nombre
  //           (escrito al revés para la pila Bluefruit).
  // .........................................................
  ServicioEnEmisora( const char * nombreServicio_ )
	:
	elServicio( stringAUint8AlReves( nombreServicio_, &uuidServicio[0], 16 ) )
  {
	
  } // ()
  
  // .........................................................
  // DISEÑO: --> escribeUUID() -->
  // Qué hace: (depuración) vuelca por el puerto serie los 16 bytes
  //           del UUID del servicio.
  // .........................................................
  void escribeUUID() {
	Serial.println ( "**********" );
	for (int i=0; i<= 15; i++) {
	  Serial.print( (char) uuidServicio[i] );
	}
	Serial.println ( "\n**********" );
  } // ()

  // .........................................................
  // DISEÑO: car: Caracteristica --> anyadirCaracteristica() -->
  // Qué hace: añade una característica a la lista interna del
  //           servicio (aún no la activa en la pila).
  // .........................................................
  void anyadirCaracteristica( Caracteristica & car ) {
	(*this).lasCaracteristicas.push_back( & car );
  } // ()

  // .........................................................
  // DISEÑO: --> activarServicio() -->
  // Qué hace: registra el servicio y todas sus características en la
  //           pila Bluefruit (begin() de cada una).
  // .........................................................
  void activarServicio( ) {
	// entiendo que al llegar aquí ya ha sido configurado
	// todo: características y servicio

	err_t error = (*this).elServicio.begin();
	Serial.print( " (*this).elServicio.begin(); error = " );
	Serial.println( error );

	for( auto pCar : (*this).lasCaracteristicas ) {
	  (*pCar).activar();
	} // for

  } // ()

  // .........................................................
  // DISEÑO: --> operator BLEService&() <-- BLEService
  // Qué hace: conversión de tipo: cuando en un sitio se necesite
  //           un BLEService, esta clase se convierte automáticamente.
  // .........................................................
  operator BLEService&() {
	// "conversión de tipo": si pongo esta clase en un sitio donde necesitan un BLEService
	return elServicio;
  } // ()
	
}; // class

#endif

// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------

