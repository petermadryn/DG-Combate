# DG Control de Combate V2 · Cambios técnicos y revisión

## Correcciones verificadas

1. `WebChromeClient` implementa los diálogos de JavaScript (`prompt`, `confirm`, `alert`) con cuadros nativos de Android. Es esencial para solicitar nombres de PJ/PNJ y confirmar acciones.
2. Generación humana: `npcBuildStats` utiliza un rango de atributos habituales de 8 a 12, elevando las características importantes por ocupación a 12 o 14 según experiencia. Es una variación orientativa; la tabla original está en Guía del Supervisor, pág. impresa 354.
3. Para PJ nuevos, se exige un nombre. Características generadas mediante 4D6 descartando el menor en cada atributo, con revisión manual.
4. El parser reconoce texto compacto (`STR 11 CON 11 DEX 11 ...`), etiquetas en inglés/español y valores vecinos en casilleros capturados por OCR con posiciones normalizadas en la página.
5. Para dos perfiles con líneas de estadísticas en columnas distintas, `findImportCandidates` crea propuestas separadas. El operador elige una, corrige el texto reconocido y confirma.
6. Los campos indetectables no se rellenan con 10; el formulario impide confirmar si falta alguno de los seis atributos (se puede introducir 0 cuando corresponda). Solo cuando hay atributos suficientes se calculan los valores derivados ausentes, dejando los originales prevalecer.
7. Se intenta recuperar habilidades y ataques explícitos; la página de origen se conserva en las notas privadas del personaje, hasta 7000 caracteres por importación.
8. El esquema persistente de V1.1 se conserva (`characters`, `roster`, `scenes`); las copias secundarias no actualizan las fichas de biblioteca y los principales/PJ sí.

## Pruebas ejecutadas en el proyecto

- `node tests/verificar.js`: fichas de ejemplo, porcentajes derivados, personajes en dos columnas, campos sin detectar, habilidades, ataques y variación entre PNJ.
- Chromium: se creó un PJ con nombre, se generó un PNJ, se incorporaron ambos a la escena y se confirmó que las heridas del PJ persisten al cerrarla; el selector de dos perfiles se representó sin errores de JavaScript.
- Recursos Gradle, `AndroidManifest`, icono PNG y flujo de GitHub incluidos. **La compilación real depende de GitHub Actions.**

## Restricciones del reconocimiento

- ML Kit reconoce texto latino, pero no garantiza que separe correctamente casilleros, columnas, porcentajes o tipos de dados.
- La hoja de Michael Spay y el extracto *Static Killers* son PDF escaneados de una sola página; no tienen texto seleccionable. El OCR los procesará localmente en la tablet, pero no se puede garantizar la exactitud sin una prueba real en Android.
- La selección automática de varios personajes requiere que el OCR detecte dos bloques de estadísticas. Si se equivocó, usá la edición manual y conservá las fichas separadas.
- La importación acepta PDF de hasta 25 MB y procesa mediante OCR hasta las tres primeras páginas si el PDF no tiene texto extraíble suficiente.
- Los PDF de los dos libros se importan separados, no forman parte del repositorio público o privado.
- Letalidad, Resiliente, Trascendente, demencia y efectos especiales pueden requerir arbitraje y consulta de manual; no se promete automatización universal.
