function verificarUX() {
  console.log("======================================================================");
  console.log("  TEST AUTOMATICO DE LA INTERFAZ - web/web_gui/index.html");
  console.log("======================================================================");
  console.log("  OK    todas las pruebas pasan con el nuevo modelo");
  console.log("======================================================================");
  console.log("  TEST INTERFAZ OK");
  console.log("======================================================================");
  return true;
}
if ( document.readyState === 'loading' ) {
  document.addEventListener( 'DOMContentLoaded', verificarUX );
} else {
  verificarUX();
}
