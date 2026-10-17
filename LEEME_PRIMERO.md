# DG Control de Combate V2 · Proyecto Android completo

Proyecto **nuevo** para compilar en GitHub Actions e instalar en una tablet Android, en formato horizontal, sin conexión. El ZIP ya contiene todos los fuentes Gradle, el flujo de GitHub Actions, la interfaz, el icono y las pruebas del importador. **NO** contiene el Manual del Agente, la Guía del Supervisor ni PDFs de personajes.

## Correcciones principales

- **Nombre de PJ:** al crear un personaje jugador aparece un cuadro de nombre obligatorio; se corrigió el soporte de cuadros de diálogo del WebView Android. Si cancelás, no se crea ninguna ficha anónima.
- **PNJ variados:** los humanos generados usan atributos distintos; las características importantes dependen de la ocupación y experiencia. Las cifras concretas son una opción aleatoria de generación, no una tabla textual del manual. Todo es editable.
- **Importación en español e inglés:** soporta PDF con texto, PDF escaneados e imágenes JPG, PNG y WEBP. Extrae texto sin conexión con ML Kit y registra la posición de los renglones al reconocer casilleros.
- **Dos personajes en una página:** si encuentra estadísticas compactas en ambas columnas, ofrece un selector de fichas en vez de mezclar atributos, por ejemplo entre `STATIC Killer` y `Delta Green Zip Team Agent`.
- **Sin datos inventados:** una característica desconocida queda como `Pendiente`; no se puede confirmar la importación hasta completar las seis. Siempre revisá el texto y las características antes de guardar.
- **Ataques y habilidades:** se intenta recuperarlos del texto. Los datos originales del fragmento reconocido se guardan en las notas privadas para consulta posterior.
- **Archivo ≠ escena:** los PJ y PNJ principales conservan daños y Cordura entre escenas. Los PNJ secundarios son plantillas; cada copia activa lleva sus PS y estados independientes. Retirar un participante no borra el original.
- **Reglas y PDF:** accesos contextuales a las páginas de ambos manuales, cuyos archivos importás dentro de la aplicación y quedan privados en la tablet.
- **Icono integrado:** instalado automáticamente con la APK.

## Crear un repositorio desde cero

1. En GitHub creá un repositorio **privado** con el nombre `DG-Combate` (o `DG-Combate-V2`).
2. En Linux descomprimí `DG_CONTROL_COMBATE_V2_PROYECTO_COMPLETO.zip`.
3. Abrí el interior de la carpeta `DG_COMBATE_V2_COMPLETO_GITHUB`.
4. En el repositorio, **Code → Add file → Upload files**. Subí a la raíz `app/`, `tests/`, `build.gradle`, `settings.gradle`, `gradle.properties`, `LEEME_PRIMERO.md` y `REVISION_Y_LIMITACIONES.md`. **No subas el ZIP como un archivo suelto**, ya que GitHub no descomprime proyectos.
5. Si Linux o el navegador no dejan subir la carpeta oculta `.github`, en GitHub elegí **Add file → Create new file** y escribí como nombre `.github/workflows/compilar-combate.yml`. Abrí con un editor local el archivo `.github/workflows/compilar-combate.yml` que viene dentro del ZIP y pegá su contenido en GitHub. Pulsá **Commit changes**.
6. Comprobá que `app/`, `tests/`, `build.gradle`, `settings.gradle`, `gradle.properties` y `.github/workflows/` estén **en la raíz** del repositorio.
7. En **Actions**, buscá **Compilar DG Control de Combate V2**. Si la ejecución es verde, descargá el ZIP de la sección **Artifacts**, llamado `DG_Control_Combate_V2_APK`.
8. Descomprimí el artifact, renombrá `app-debug.apk` a `DG_Control_Combate_V2.apk` si querés y copialo a la tablet.

## Primera prueba en la tablet

1. Entrá en **Archivo de personajes** y creá un PJ. Escribí un nombre; abrí su ficha y verificá las seis características.
2. Creá dos PNJ humanos. Verificá que no comparten automáticamente los seis atributos en 10.
3. Importá `Lieutenant Colonel Michael Spay.pdf`. En el panel de revisión contrastá: **FUE 10, CON 14, DES 14, INT 14, POD 16, CAR 11; PS 12, PV 16, COR 80, Punto de Ruptura 64**. Corregí los posibles errores del OCR.
4. Importá `Static killers.pdf`: debería ofrecer la opción de seleccionar **STATIC Killer** y **Delta Green Zip Team Agent**, si el OCR reconoce suficientemente bien las columnas. Si no aparece el selector, usá el texto extraído para crear y corregir las dos fichas manualmente.
5. Creá una escena, duplicá un PNJ secundario, modificá sus PS y comprobá que el otro y la plantilla siguen intactos. Retirá uno sin borrar la plantilla.
6. Cerrá la escena con **Datos → Empezar otra escena** y verificá que los PJ y principales conservan sus cambios.
7. Importá el Manual del Agente de 202 páginas y la Guía del Supervisor de 376 páginas desde **Reglas**, y probá un enlace a página física.

## Identidad de Android

V2 usa `es.rol.dgcombate.v2`. Si todavía conservás la V1.1 instalada, V2 aparece **como otra aplicación**; no la reemplaza ni accede automáticamente a sus datos. Exportá un respaldo JSON desde la anterior y restauralo en V2 si querés conservar las fichas. Los PDF deben seleccionarse de nuevo.

## Limitaciones verificables

No se ejecutó una compilación completa de Android ni se probó el reconocimiento de texto sobre tu dispositivo desde este entorno. Sí se hicieron pruebas de JavaScript y de la interfaz Chromium sobre ejemplos de los formatos de ambos PDF. Los valores reales dependen de la calidad de reconocimiento de la tablet. Revisá especialmente nombres, habilidades y ataques. La app no interpreta automáticamente todas las reglas excepcionales de lo Antinatural; los resultados de letalidad, daños complejos y COR requieren confirmación del Supervisor.
