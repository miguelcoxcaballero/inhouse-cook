# Grabación → implementación

Referencia: `Screen_Recording_20261003_130223_Instagram.mp4`, 720 × 1560, duración 24,705 s. Extracción cada 0,5 s; revisión de configuración, generación, lista semanal y receta, incluidos controles pequeños y estados posteriores.

| Momento aproximado | Función visible | Implementación |
|---|---|---|
| 0–3 s | Elegir supermercado | Mercadona/DIA; código postal manual o ubicación Android; catálogo real de la zona |
| 3–4 s | Personas, controles +/− | 1–8 personas; cantidades y coste ajustados |
| 4–6 s | Presupuesto semanal y slider | 15–200 €; 3/5/7 comidas; cálculo por envase completo |
| 6–8 s | Estilos con fotografías y selección múltiple | Rápidas, ligeras, familiares, cuchara, comidas de restaurante, vegetal, proteínas |
| 8–10 s | Dietas | Vegana, vegetariana, pescetariana, sin gluten y sin lácteos; exclusiones de huevo/pescado/soja |
| 10–13 s | Cocina ilustrada con utensilios seleccionables | Placa, horno, freidora, microondas, batidora, olla lenta, olla a presión, hervidor; selección gráfica y lista accesible |
| 13–16 s | Generación de recetas | Consulta real del catálogo y selección del recetario por restricciones, variedad y presupuesto; mensajes de progreso |
| 16–19 s | Semana, presupuesto, compra, regenerar y guardar | Plan completo; coste de cesta y progreso de compra; regeneración con alternativas; guardar/cargar/eliminar semanas |
| 19–22 s | Receta: fuente, tiempo, cocinar, compra, plan, nutrición | Detalle funcional; enlace original de importaciones; modo cocina; compra agregada; reemplazo de cualquier día; macros estimados |
| 22–24,7 s | Ingredientes, raciones, conversión, checks, terminado, guardado | Escalado 1–12 raciones, métricas/medidas domésticas, checklist persistente, finalización mediante deslizador/botón, favoritos e historial |

## Continuación de las acciones

Las acciones que abre el vídeo tienen recorridos completos: pasos de cocina con temporizador y valoración, cesta con productos/precios/enlaces/compra marcada, despensa y ajustes de envases, semanas guardadas, favoritos, importación de fuentes y copias JSON.

La generación social global mostrada por Albo depende de un servicio privado. Aquí se implementa selección real de 36 recetas locales y el importador de blogs con datos estructurados; las recetas de redes se guardan pegando enlace, ingredientes y pasos. Nunca se simula una búsqueda en cuentas o bases privadas.

Las cuatro familias de platos mostradas están representadas: sopa de kimchi/cerdo/tofu, pollo con soja/limón, udon con kimchi y tortitas de tofu/papel de arroz. Son adaptaciones propias, sin atribuir al creador del vídeo una receta que no se ha podido recuperar.

## Verificación realizada

- Recorrido automatizado a 390 × 844: configuración de siete días vegetarianos; generación/regeneración; sustitución; guardado; raciones; checks; favoritos; compra; cocina/temporizador/valoración; precio manual; despensa; importación; recarga; restauración de semana; exportación/importación; búsqueda. Sin errores JavaScript ni desbordamiento horizontal.
- Motor: agregación, envases completos, multipacks y peso escurrido, despensa, precios desconocidos, cambios manuales, todas las dietas con distintos utensilios, presupuesto insuficiente, regeneración y restricciones de recetas importadas.
- Cliente Java que usa el APK: catálogos reales de Mercadona y DIA en 28001 y 08001, verificando ubicación, productos, cantidades y precios positivos.
- Compilación Android: APK firmado, versión 2.0.0, `com.inhouse.cook`.
- Las pruebas de UI se ejecutan en Chromium; no se ha ejecutado un emulador ni un teléfono físico. Los permisos, la geolocalización y los selectores de archivos/compartir requieren la comprobación final en el dispositivo.
