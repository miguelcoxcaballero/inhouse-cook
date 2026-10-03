# Inhouse Cook

Planifica la semana, cocina y prepara la compra con productos de supermercados españoles. Android 7 o posterior. Estética Inhouse: Comfortaa, tonos crema, negro y cobre.

**[Descargar el APK 2.0](https://raw.githubusercontent.com/miguelcoxcaballero/inhouse-cook/main/dist/Inhouse-Cook.apk)** · [Funciones del vídeo](docs/video-flows.md) · [Capturas](docs/screenshots/)

## Funciones

- Configuración en seis pasos: supermercado y código postal, personas, presupuesto, estilos, dietas y utensilios interactivos.
- Plan de 3, 5 o 7 comidas principales. Generación según filtros, cantidades, productos disponibles y coste de envases. Regeneración, cambio de platos y semanas guardadas.
- 36 recetas completas, ingredientes ajustables por raciones, medidas domésticas aproximadas, nutrición estimada y favoritos. Incluye adaptaciones de los platos del vídeo.
- Modo cocina paso a paso con temporizador, progreso, pantalla encendida y valoración al terminar.
- Compra agregada por ingrediente: envases, cantidades, productos, precios, enlaces originales, marcas de comprado, ajustes y despensa.
- Importación de recetas de blogs con `Recipe` JSON-LD y entrada de texto/enlace para recetas de Instagram, TikTok o Pinterest.
- Historial, preferencias y planes persistentes. Exportación/restauración JSON y compartir la compra mediante el selector de Android.

## Precios y ubicación

El APK consulta los catálogos públicos de **Mercadona y DIA**. No requiere cuenta ni API key.

- Mercadona confirma código postal y almacén mediante su servicio de ubicación. Las categorías se consultan con ese almacén. Se usa el precio del envase, peso escurrido cuando el catálogo lo facilita y peso aproximado cuando procede.
- DIA configura el código postal en una sesión anónima propia. Cada respuesta debe confirmar el mismo código antes de aceptar precios. Las cantidades y multipacks proceden de los nombres del catálogo.
- Se muestran supermercado, código postal y fecha de consulta. Una consulta fallida conserva el último catálogo; si no existe para esa zona, la referencia incluida de Madrid **28001** se identifica como tal. Nunca se presenta como un precio confirmado de otro lugar.
- Un ingrediente sin producto compatible aparece **sin precio** y el total se marca incompleto. Puedes introducir un precio y tamaño de envase observado por ti. Los productos preparados no sustituyen automáticamente a sus ingredientes frescos.
- El presupuesto incluye envases completos de las comidas elegidas, descontando lo que marcas en la despensa. No incluye entrega, otras comidas ni descuentos personales. La disponibilidad, el peso final y los precios pueden cambiar al comprar. DIA publica el peso indicado en el nombre, que puede ser neto en conservas; comprueba el peso escurrido y corrige el envase si es necesario.

Las APIs de los supermercados pueden cambiar o limitar cobertura. Las dietas filtran ingredientes conocidos; revisa etiquetas y trazas para alergias. La nutrición y las medidas domésticas son estimaciones.

## Referencia de la grabación

Se revisó el vídeo completo de 24,7 segundos, extraído en 50 fotogramas. [El desglose](docs/video-flows.md) relaciona cada pantalla con sus funciones implementadas. La app original utiliza su propio servicio de búsqueda social; este proyecto usa un recetario local y un importador real. No accede a bases privadas de TikTok o Instagram ni realiza compras automáticamente.

## Desarrollo

Proyecto Android Java, Gradle 8.9 y Android Gradle Plugin 8.7.2. UI local empaquetada con el APK; tipografías y fotografías disponibles sin red. El puente Android consulta HTTPS, ubicación opcional, archivos y el selector de compartir. Los enlaces externos se abren en el navegador, separado de la WebView local.

```sh
# JDK 17 o 21; SDK plataforma 35 y build-tools 35.0.0
./gradlew assembleDebug
# Salida: app/build/outputs/apk/debug/app-debug.apk

# Refrescar la referencia incluida
python scripts/catalog.py --postal 28001 --out app/src/main/assets/catalog-seed.json
# Generar el módulo del catálogo después de refrescar
python scripts/bundle_catalog.py

# Regenerar el recetario a partir de su fuente
python scripts/recipes.py
```

### Comprobaciones

```sh
node tests/engine.cjs
# Servir app/src/main/assets en localhost:8765 y usar Python Playwright con Chromium
python tests/flows.py
# El cliente Java del APK se prueba directamente contra Madrid y Barcelona:
# Compilar tests/CatalogIntegration.java y CatalogClient.java con org.json en el classpath.
```

El flujo de GitHub Actions ejecuta los cálculos y compila el APK. El APK descargable es una compilación firmada para instalación directa, no una distribución de Google Play.
