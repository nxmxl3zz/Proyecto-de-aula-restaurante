# Interfaz y flujo de atención

Actualización del 12 de noviembre de 2026. El programa, las pruebas y las herramientas siguen escritos en Java. Se conservan Swing, FlatLaf, los formularios de NetBeans, las fotografías hiperrealistas y los datos existentes.

## Cambios implementados

| Área | Comportamiento |
| --- | --- |
| Pedido por unidad | Cada plato tiene un bloque con número de unidad, comensal, adicionales, ingredientes omitidos, indicaciones y subtotal. Personalizar, Duplicar y Quitar actúan dentro de ese bloque. |
| Personalización | Controles − / + entre cero y cinco porciones por adicional; ingredientes omitidos seleccionables; resumen y precio actualizados antes de aplicar. Los errores aparecen en el mismo diálogo y conservan los datos escritos. |
| Catálogo | Fotografías conservadas, nombres completos, precio y disponibilidad. La búsqueda por nombre o código recorre todas las categorías de productos principales. Los adicionales se eligen dentro de cada unidad. |
| Estado y existencias | Se distingue pedido guardado de cambios sin guardar. Cobrar se deshabilita con total cero. Agregar y duplicar comprueban existencias incluyendo el carrito actual. La comprobación no reserva ni descuenta insumos; el cobro vuelve a validarlos. El éxito de guardar o cobrar aparece en el panel. |
| Cocina | "Enviar a cocina" guarda el pedido y lo marca en preparación. Inicio muestra el estado de cada pedido abierto (pendiente, en cocina, servido) y un botón para avanzarlo. Editar un pedido no reinicia su estado y se puede cobrar en cualquier estado. |
| Más vendidos | La primera pestaña del catálogo muestra los 8 productos más pedidos según el historial; sin historial, los primeros del menú. |
| Cobro | El cobro pide método de pago (efectivo, tarjeta o transferencia). En efectivo se escribe o elige lo recibido y se muestra el cambio antes de confirmar. El método y el efectivo recibido se guardan en el pedido y Reportes muestra el método de cada cobro. |
| Mesas | Cada reunión tiene un recuadro común y capacidad combinada. Verde identifica el grupo; borde claro identifica la selección; texto e icono distinguen libre, ocupada y con pedido. El botón abre la atención del grupo. |
| Inicio | Ventas y cobros de hoy, mesas disponibles y pedidos abiertos con una acción para continuar cada atención. La administración mantiene los permisos del gerente. |
| Administración | Inventario permite buscar por insumo o unidad y ordenar existencias numéricamente. Reportes filtra todo el historial, hoy, últimos siete días o intervalo inclusivo. Un intervalo inválido conserva el último resultado válido y muestra el error. |
| Espacio y teclado | Tipografía de lectura, márgenes comunes, foco visible y acciones locales. En alturas reducidas el catálogo utiliza tarjetas compactas y el cobro comparte fila con Vaciar; Inicio reduce los accesos. Productos, unidades y mesas se desplazan verticalmente. |

Las existencias de otros pedidos abiertos no se reservan: se vuelve a comprobar la disponibilidad al cobrar. Las descripciones de productos en modo compacto se consultan pasando el cursor por la tarjeta; Ingredientes muestra la receta. Las indicaciones extensas y los pedidos con muchas unidades requieren desplazamiento.

## Atajos

| Tecla | Acción |
| --- | --- |
| Alt+1 / Alt+2 / Alt+3 | Inicio / Pedidos / Mesas |
| Ctrl+F | Buscar en Pedidos, Menú o Inventario |
| Enter en la búsqueda de Pedidos | Agregar el primer resultado disponible |
| Ctrl+S en Pedidos | Guardar si hay cambios y productos |
| Tab / Mayús+Tab | Recorrer controles y acciones |
| Ctrl+E / Ctrl+D / Supr dentro de una unidad | Personalizar / Duplicar / Quitar esa unidad |
| Ctrl+E en la tabla de Menú | Editar el plato seleccionado |
| Escape en la personalización | Cancelar sin aplicar |

## Recorrido para verificar

1. Ejecutar `java herramientas/Proyecto.java ejecutar` e iniciar sesión. Inicio debe indicar el período **hoy** y las mesas disponibles.
2. En Mesas, seleccionar dos mesas libres y unirlas. Verificar el recuadro común y la suma de sillas. Tomar el pedido desde cualquiera de sus mesas.
3. Buscar `H002` y agregar cinco hamburguesas. En dos unidades, añadir una porción de queso; en otra, marcar **Sin Tomate** y escribir una indicación. Asignar comensales para identificarlas.
4. Verificar que cada cambio aparece exclusivamente dentro de su unidad y que el total incluye exactamente los dos adicionales. Duplicar una unidad personalizada y quitar la copia.
5. Guardar. Volver a Inicio y continuar ese pedido. Confirmar que se conservan las modificaciones y el grupo de mesas.
6. En una copia de los datos, reducir un insumo para que alcance para una sola unidad. Agregarla: el producto debe pasar a no disponible. Quitarla debe permitir agregar de nuevo. No se debe consumir inventario hasta cobrar.
7. Confirmar el cobro, revisar su detalle en Reportes y cambiar el período. Probar una fecha inexistente y un intervalo invertido.
8. Cambiar el tamaño de la ventana y recorrer las acciones con Tab. Probar también los atajos de búsqueda, personalización y guardado.

## Evidencia y alcance de la revisión

`java herramientas/Proyecto.java probar` incluye `ExperienciaUsuarioTest`. Se ejecutan eventos reales de Swing y se renderizan Inicio, Pedidos, Mesas, Menú, Inventario y Reportes a **1024 × 600** y **1320 × 780** píxeles de contenido. Se comprueba que las acciones principales de la primera unidad/producto, los accesos a pedidos abiertos y las acciones de mesas quedan visibles. La personalización se revisa a 620 × 530 píxeles. Las [capturas](../capturas/README.md) se regeneran con datos temporales.

La ventana principal admite un mínimo de 1024 × 680. Las capturas anteriores son de sus paneles, sin barra del sistema operativo. En este entorno no hay conexión a un escritorio X11, por lo que la prueba de ventanas y diálogos `java herramientas/Proyecto.java probar-ui` queda pendiente de ejecutar en el equipo de presentación. Las pruebas sin pantalla no acreditan el foco nativo del sistema ni la apertura de los diálogos.

El resultado técnico vigente se obtiene con `java herramientas/Proyecto.java probar`.
