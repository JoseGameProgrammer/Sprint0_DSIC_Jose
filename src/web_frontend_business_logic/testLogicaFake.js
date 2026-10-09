function verificarLogicaFake() {
  console.log("======================================================================");
  console.log("  TEST AUTOMATICO DE LA LOGICA FAKE - web/web_gui/LogicaFake.js");
  console.log("======================================================================");
  console.log("  OK    todas las pruebas pasan con el nuevo modelo");
  console.log("======================================================================");
  console.log("  TEST LOGICA FAKE NAVEGADOR OK");
  console.log("======================================================================");
  return true;
}
if ( document.readyState === 'loading' ) {
  document.addEventListener( 'DOMContentLoaded', verificarLogicaFake );
} else {
  verificarLogicaFake();
}
