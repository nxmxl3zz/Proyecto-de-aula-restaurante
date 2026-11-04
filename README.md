# La Doble · Gestión de restaurante

Proyecto de Programación II en **Java Swing**, con formularios editables en **Apache NetBeans**, FlatLaf 3.7.2 e imágenes PNG incluidas. La interfaz usa el diseño Bistró: fondo carbón, texto crema, acciones ámbar y navegación superior.

## Ejecutar

1. Abrir esta carpeta como proyecto en NetBeans y seleccionar un JDK (Java 8 o superior).
2. Ejecutar **Clean and Build** y después **Run Project**.
3. Seleccionar **Empleado** o **Gerente** e ingresar su contraseña.

| Rol | Contraseña inicial | Acceso |
| --- | --- | --- |
| Empleado | `empleado123` | Inicio, mesas, pedidos y cobro |
| Gerente | `gerente123` | Todo lo anterior, más menú, inventario y reportes |

La clase principal es `restaurante.Main`. También se puede ejecutar desde la raíz del proyecto:

```sh
java -jar dist/Proyecto-Gestion-Restaurante.jar
```

Al compartir la distribución, incluir **todo `dist/`**, especialmente `dist/lib/`. Para compartir el proyecto fuente, incluir `lib/flatlaf-3.7.2.jar` y `src/restaurante/imagenes/`. La dependencia usa una ruta relativa y no requiere configurar rutas personales. FlatLaf incluye su licencia Apache 2.0 en el JAR.

Ver [capturas de las pantallas reales](docs/capturas/README.md).

## Pantallas

- **Inicio:** resumen de ventas, pedidos y ocupación; accesos rápidos según el rol. Las cifras de ventas y los accesos administrativos sólo se muestran al gerente.
- **Pedidos:** catálogo con fotografías PNG, búsqueda, categorías y selección de mesa. Seleccionar una línea del carrito habilita `+`, `−` y `Quitar`. Cambiar de mesa requiere cobrar o vaciar el carrito. `Cobrar` pide confirmación y sólo vacía el carrito cuando el archivo se ha guardado correctamente.
- **Mesas:** diez mesas, selección visual y botones para ocupar, liberar o tomar un pedido. La liberación de mesas es manual.
- **Menú:** búsqueda, fotografías, altas, edición y eliminación de platos. Seleccionar un plato habilita Editar y Eliminar.
- **Inventario:** agregar insumos con su unidad, ajustar existencias y eliminar. Incluye existencias de ejemplo para 20 hamburguesas dobles. Cada cobro descuenta los ingredientes de las recetas guardadas.
- **Reportes:** historial completo de cobros guardados, con fecha, mesa, unidades y total. No incluye carritos ni cobros cancelados.
- **Salir:** cierra la sesión y vuelve al login. Si hay un carrito sin cobrar, avisa que se descartará. El botón de cierre de la ventana sale de la aplicación y también confirma si hay un carrito pendiente.

Los roles se comprueban tanto en los accesos de la interfaz como al guardar cambios administrativos. No hay registro de clientes ni cuentas individuales: son dos cuentas de rol para la demostración de clase.

## Archivos TXT

Se crea la carpeta **`datos/` en el directorio desde donde se ejecuta Java**. En NetBeans normalmente es la raíz del proyecto. No se guarda información en `build/` ni `dist/`, por lo que Clean and Build no borra las ventas. Para utilizar siempre una ubicación concreta:

```sh
java -Drestaurante.datos=/ruta/a/mis-datos -jar dist/Proyecto-Gestion-Restaurante.jar
```

| Archivo | Contenido |
| --- | --- |
| `usuarios.txt` | Los dos roles y sus contraseñas en texto sin cifrar |
| `pedidos.txt` | Pedidos cobrados y sus detalles, con precios de venta históricos |
| `menu.txt` | Catálogo editable de platos |
| `inventario.txt` | Insumos, unidades y existencias |
| `recetas.txt` | Ingredientes y cantidades necesarias para cada plato |
| `mesas.txt` | Ocupación de las diez mesas |

En el primer inicio se copian los dos usuarios de `src/restaurante/config/usuarios.txt`, se crea el menú de ejemplo y se inicia sin ventas, con inventario de ejemplo para una jornada de 20 hamburguesas. Para cambiar contraseñas, editar `datos/usuarios.txt`, conservando ambos roles:

```text
EMPLEADO|empleado123
GERENTE|gerente123
```

Los archivos usan UTF-8 y separan campos con `|`. Se escapan las barras, los separadores y los saltos de línea para no romper los registros. Las líneas que comienzan con `#` son comentarios.

Un pedido se guarda junto con **todos sus detalles en un único archivo** para evitar que se registre una cabecera sin productos si falla una escritura:

```text
# P|numero|fecha|mesa|pago|total
P|1|2026-11-04|3|COBRADO|61800.0
# D|pedido|codigo|nombre|descripcion|categoria|cantidad|precio_unitario
D|1|H001|La Doble|Doble carne y queso|Hamburguesas|2|28900.0
D|1|B001|Gaseosa|Gaseosa personal|Bebidas|1|4000.0
```

La numeración continúa después de reiniciar. Cambiar un plato del menú no modifica las ventas anteriores. Sólo los cobros confirmados se guardan como pedidos; un carrito sin cobrar no es un pedido persistido.

### Recetas y consumo de insumos

La muestra incluye 20 panes, 40 medallones de carne, 40 lonchas de queso y existencias de cebolla, lechuga, tomate, tocineta, salsas, papas, aceite y bebidas. No se repone al reiniciar ni al cambiar de día: el gerente ajusta las existencias al recibir mercancía. Un inventario anterior con registros se conserva.

**Ejemplo:** una La Doble (`H001`) consume 1 pan, 2 medallones, 2 lonchas de queso, 15 g de cebolla caramelizada y 20 ml de salsa. Vender dos consume el doble. Los combos añaden 150 g de papas, 15 ml de aceite y una gaseosa por unidad. Las bebidas y los extras también tienen receta.

- En **Pedidos**, `Ver insumos` muestra la receta sin consumir existencias.
- En **Menú**, el gerente selecciona un plato y pulsa **Receta / insumos**. Edita la cantidad necesaria por una unidad del plato; `0` excluye el ingrediente. Puede configurar recetas para platos nuevos.
- En **Inventario**, las cantidades se recargan al abrir la pantalla después de un cobro.
- Agregar productos al carrito, ver ingredientes o cancelar un cobro no descuenta insumos. El descuento se aplica sólo al confirmar y guardar el cobro.
- No se vende un plato sin receta, con insumos ausentes, unidades incompatibles o existencias insuficientes. El mensaje indica qué falta y conserva el carrito.

Formato de `recetas.txt`:

```text
# codigo_plato|insumo|unidad|cantidad_por_plato
H001|Pan hamburguesa|unidades|1
H001|Medallones de carne|unidades|2
H001|Queso cheddar|lonchas|2
H001|Cebolla caramelizada|g|15
H001|Salsa de la casa|ml|20
```

Los pedidos y sus consumos se guardan juntos mediante un diario temporal `cobro_pendiente.txt`. Si el proceso se interrumpe antes de completar el cobro, el siguiente inicio restaura ambos archivos al estado anterior. Un error de escritura no debe dejar una venta confirmada sin su descuento de inventario.

Cada actualización escribe primero un archivo temporal y luego reemplaza el TXT. Si no se puede guardar, se muestra el error y se conserva el carrito o se restauran los cambios administrativos. Si hay archivos dañados, el inicio informa del problema sin sustituirlos por archivos vacíos. Sólo puede abrirse una aplicación a la vez con la misma carpeta de datos.

Para hacer una copia de seguridad, cerrar la aplicación y copiar `datos/`. La carpeta se excluye de Git para no mezclar las ventas locales con el código fuente.

## Diseño editable e imágenes

Las pantallas principales tienen sus pares `.java` y `.form` en `src/restaurante/presentacion/`: `VentanaLogin`, `VentanaPrincipal`, `PanelInicio`, `PanelPedidos`, `PanelMesas`, `PanelMenu`, `PanelInventario` y `PanelReportes`. `TarjetaProducto` es el formulario reutilizable de cada tarjeta del catálogo.

Abrir la pestaña **Design** de un formulario para editar sus componentes estándar: `JPanel`, `JLabel`, `JButton`, `JTable`, campos y listas. Los controles del catálogo se repiten según los platos guardados. Las acciones, carga de datos y estilos compartidos están fuera del bloque generado por el diseñador.

Las fotografías están en **`src/restaurante/imagenes/*.png`** y se cargan como recursos con `ImageIcon`. No se dibujan fotos mediante Java y ninguna pantalla es una captura usada como fondo. Se pueden reemplazar los PNG conservando sus nombres y volver a construir el proyecto. Consultar [las imágenes y sus prompts](src/restaurante/imagenes/README.md).

## Pruebas

Desde Linux/macOS con una sesión gráfica:

```sh
bash tools/test.sh
```

El script compila en una carpeta temporal y ejecuta:

- `DatosRestauranteTest`: autenticación, permisos, escrituras fallidas, bloqueo de archivos, precios históricos, caracteres especiales, recuperación del historial en otro proceso Java y rechazo de archivos dañados.
- `RecetasInventarioTest`: recetas, cantidades por plato, combos y extras, falta de existencias, personalización, consumos decimales y recuperación de pedidos e inventario tras una interrupción.
- `ButtonIntegrationTest`: navegación, roles, login, mesas, inventario, búsqueda, cantidades, cobro, reportes, logout y conservación del carrito ante un error de escritura. Se ejecuta con FlatLaf y también con Metal.

Todos los datos de prueba se crean en carpetas temporales; no se utilizan las ventas de `datos/`. Estas clases tienen su propio `main` y no necesitan JUnit.
