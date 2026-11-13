package restaurante.presentacion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import restaurante.controlador.*;
import restaurante.entidades.*;
import restaurante.seguridad.*;

/** Interfaz de consola con los mismos controladores y reglas que Swing. */
public final class Consola {
    private final BufferedReader entrada;
    private final PrintStream salida;
    private ControladorRestaurante controlador;
    private final ControladorAcceso acceso;
    private final java.util.function.Function<Sesion, ControladorRestaurante> alEntrar;

    public Consola(InputStream entrada, PrintStream salida, ControladorAcceso acceso,
            java.util.function.Function<Sesion, ControladorRestaurante> alEntrar) {
        this.entrada = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        this.salida = salida;
        this.acceso = acceso;
        this.alEntrar = alEntrar;
    }

    public void ejecutar() throws IOException {
        salida.println("LA DOBLE - Gestión de restaurante (consola)");
        while (true) {
            String rol = leer("Rol: 1 empleado, 2 gerente, 0 salir: ");
            if (rol == null || "0".equals(rol))
                return;
            if (!"1".equals(rol) && !"2".equals(rol)) {
                salida.println("Selecciona un rol válido.");
                continue;
            }
            char[] clave;
            if (System.console() != null)
                clave = System.console().readPassword("Contraseña: ");
            else {
                String texto = leer("Contraseña (visible en terminal sin consola nativa): ");
                if (texto == null)
                    return;
                clave = texto.toCharArray();
            }
            Sesion sesion;
            try {
                sesion = acceso.autenticar("1".equals(rol) ? Rol.EMPLEADO : Rol.GERENTE, clave);
            } finally {
                if (clave != null)
                    Arrays.fill(clave, '\0');
            }
            if (sesion == null) {
                salida.println("Contraseña incorrecta.");
                continue;
            }
            controlador = alEntrar.apply(sesion);
            try {
                atender();
            } finally {
                controlador.cerrarSesion();
            }
        }
    }

    private void atender() throws IOException {
        while (true) {
            salida.println("\n1 Menú | 2 Mesas | 3 Cobrar | 4 Inventario | 5 Reportes");
            salida.println("12 Pedidos abiertos | 13 Guardar pedido | 14 Editar pedido | 15 Cobrar "
                    + "abierto");
            salida.println("16 Unir mesas | 17 Separar mesas | 19 Cancelar pedido abierto");
            salida.println("20 Personalizar una unidad de un pedido abierto");
            if (controlador.esGerente()) {
                salida.println(
                        "6 Crear plato | 7 Editar plato | 8 Eliminar plato | 9 Guardar receta");
                salida.println("10 Agregar/ajustar insumo | 11 Eliminar insumo");
                salida.println("18 Configurar sillas");
            }
            String opcion = leer("Opción (0 cerrar sesión): ");
            if (opcion == null || "0".equals(opcion))
                return;
            try {
                switch (opcion) {
                    case "1":
                        for (Plato p : controlador.getMenu())
                            if (!p.esAdicional())
                                salida.println(p);
                        break;
                    case "2":
                        mesas();
                        break;
                    case "3":
                        cobrar();
                        break;
                    case "4":
                        inventario();
                        break;
                    case "5":
                        for (Pedido p : controlador.getReporteVentas()) {
                            salida.println(p);
                            salida.println(
                                    ResumenPedido.texto(p.getEtiquetaMesas(), p.getDetalles()));
                        }
                        break;
                    case "6":
                        guardarPlato(false);
                        break;
                    case "7":
                        guardarPlato(true);
                        break;
                    case "8":
                        eliminarPlato();
                        break;
                    case "9":
                        receta();
                        break;
                    case "10":
                        guardarInsumo();
                        break;
                    case "11":
                        eliminarInsumo();
                        break;
                    case "12":
                        for (Pedido pedido : controlador.getPedidosAbiertos())
                            salida.println("#" + pedido.getNumero() + " · "
                                    + pedido.getEtiquetaMesas() + " · " + pedido.calcularTotal());
                        break;
                    case "13":
                        guardarAbierto(false);
                        break;
                    case "14":
                        guardarAbierto(true);
                        break;
                    case "15":
                        cobrarAbierto();
                        break;
                    case "16":
                        List<Integer> unir = new ArrayList<>();
                        for (String numero : requerido("Mesas separadas por coma (ejemplo 1,3): ")
                                        .split(",", -1))
                            unir.add(Integer.parseInt(numero.trim()));
                        controlador.unirMesas(unir);
                        salida.println("Mesas unidas.");
                        break;
                    case "17":
                        controlador.separarMesas(entero("Mesa del grupo a separar: "));
                        break;
                    case "18":
                        exigirGerente();
                        controlador.configurarSillas(
                                entero("Número de mesa: "), entero("Número de sillas (1 a 20): "));
                        break;
                    case "19":
                        int cancelar = entero("Número de pedido abierto: ");
                        if ("S".equalsIgnoreCase(requerido("¿Cancelar el pedido? (S/N): ")))
                            controlador.cancelarPedidoAbierto(cancelar);
                        break;
                    case "20":
                        personalizarAbierto();
                        break;
                    default:
                        salida.println("Opción inválida.");
                }
            } catch (IOException | IllegalArgumentException | SecurityException ex) {
                salida.println("No se aplicó la operación: " + ex.getMessage());
            }
        }
    }

    private void mesas() throws IOException {
        for (Mesa mesa : controlador.getDetalleMesas())
            salida.println("Mesa " + mesa.getNumero() + ": " + mesa.getSillas() + " sillas · "
                    + (mesa.estaOcupada() ? "Ocupada" : "Libre") + " · grupo " + mesa.getGrupo());
        int numero = entero("Mesa a cambiar (0 volver): ");
        if (numero == 0)
            return;
        String estado = requerido("Estado (1 ocupada, 0 libre): ");
        if (!"1".equals(estado) && !"0".equals(estado))
            throw new IllegalArgumentException("Estado inválido.");
        controlador.guardarMesa(numero, "1".equals(estado));
    }

    private void cobrar() throws IOException {
        int mesa = entero("Mesa (0 sin mesa): ");
        List<DetallePedido> carrito = leerCarrito();
        double total = carrito.stream().mapToDouble(DetallePedido::calcularSubtotal).sum();
        if (!"S".equalsIgnoreCase(requerido("Total " + total + ". ¿Confirmar cobro? (S/N): ")))
            return;
        Pedido pedido = controlador.registrarPedido(mesa == 0 ? null : mesa, carrito);
        salida.println(
                "Cobro registrado: #" + pedido.getNumero() + ", total " + pedido.calcularTotal());
    }

    private List<DetallePedido> leerCarrito() throws IOException {
        List<DetallePedido> carrito = new ArrayList<>();
        while (true) {
            String codigo = requerido("Código de plato (0 terminar): ");
            if ("0".equals(codigo))
                break;
            Plato plato = buscarPlato(codigo);
            if (plato.esAdicional())
                throw new IllegalArgumentException("Selecciona el plato principal; sus adicionales "
                                                   + "se asocian después a cada unidad.");
            int cantidad = entero("Cantidad: ");
            if (cantidad < 1 || cantidad > 1000)
                throw new IllegalArgumentException("Escribe entre 1 y 1000 unidades.");
            for (int unidad = 0; unidad < cantidad; unidad++)
                carrito.add(new DetallePedido(plato, 1, plato.getPrecio()));
        }
        return carrito;
    }

    private Pedido buscarAbierto(int numero) {
        return controlador.getPedidosAbiertos()
                .stream()
                .filter(p -> p.getNumero() == numero)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El pedido abierto no existe."));
    }

    private void personalizarAbierto() throws IOException {
        Pedido pedido = buscarAbierto(entero("Número de pedido abierto: "));
        salida.println(ResumenPedido.texto(pedido.getEtiquetaMesas(), pedido.getDetalles()));
        int unidad = entero("Unidad a personalizar: ") - 1;
        if (unidad < 0 || unidad >= pedido.getDetalles().size())
            throw new IllegalArgumentException("Unidad inexistente.");
        List<DetallePedido> detalles = new ArrayList<>(pedido.getDetalles());
        DetallePedido d = detalles.get(unidad).copia();
        if (d.getCantidad() != 1)
            throw new IllegalArgumentException("Este pedido antiguo agrupa cantidades; abre sus "
                                               + "unidades por separado desde la interfaz.");
        d.setComensal(requerido("Comensal o silla: "));
        d.setObservaciones(requerido("Otras indicaciones (vacío si no hay): "));
        salida.println("Ingredientes del plato: "
                + controlador.getReceta(d.getPlato().getCodigo())
                        .stream()
                        .map(IngredienteReceta::getInsumo)
                        .collect(java.util.stream.Collectors.joining(", ")));
        String omitidos =
                requerido("Ingredientes que NO llevará, separados por coma (vacío: ninguno): ");
        List<String> sin = new ArrayList<>();
        if (!omitidos.isEmpty())
            for (String nombre : omitidos.split(",")) sin.add(nombre.trim());
        d.setSinIngredientes(sin);
        salida.println("Adicionales exclusivamente para esta unidad:");
        for (Plato extra : controlador.getMenu())
            if (extra.esAdicional())
                salida.println(
                        extra.getCodigo() + " · " + extra.getNombre() + " · " + extra.getPrecio());
        List<AdicionalPedido> extras = new ArrayList<>();
        while (true) {
            String codigo = requerido("Código adicional (0 terminar): ");
            if ("0".equals(codigo))
                break;
            Plato extra = buscarPlato(codigo);
            extras.add(
                    new AdicionalPedido(extra, entero("Porciones (1 a 5): "), extra.getPrecio()));
        }
        d.setAdicionales(extras);
        detalles.set(unidad, controlador.validarUnidad(d));
        if ("S".equalsIgnoreCase(requerido("¿Guardar la personalización de esta unidad? "
                                             + "(S/N): ")))
            controlador.guardarPedidoAbierto(pedido.getNumero(), pedido.getMesa(), detalles);
    }

    private void guardarAbierto(boolean editar) throws IOException {
        Integer numero = editar ? entero("Número de pedido abierto: ") : null;
        Pedido anterior = editar ? buscarAbierto(numero) : null;
        Integer mesa = anterior == null ? Integer.valueOf(entero("Mesa (0 sin mesa): "))
                                        : anterior.getMesa();
        if (anterior != null)
            salida.println("Productos actuales: " + anterior.getDetalles()
                    + ". Introduce la lista completa que los reemplazará.");
        List<DetallePedido> carrito = leerCarrito();
        if (!"S".equalsIgnoreCase(requerido("¿Guardar estos productos? (S/N): ")))
            return;
        Pedido guardado = controlador.guardarPedidoAbierto(
                numero, mesa != null && mesa == 0 ? null : mesa, carrito);
        salida.println("Pedido abierto guardado: #" + guardado.getNumero() + " · "
                + guardado.getEtiquetaMesas());
    }

    private void cobrarAbierto() throws IOException {
        Pedido pendiente = buscarAbierto(entero("Número de pedido abierto: "));
        if (!"S".equalsIgnoreCase(requerido(
                    "Total " + pendiente.calcularTotal() + ". ¿Confirmar cobro? (S/N): ")))
            return;
        Pedido cobrado =
                controlador.cobrarPedidoAbierto(pendiente.getNumero(), pendiente.getDetalles());
        salida.println(
                "Cobro registrado: #" + cobrado.getNumero() + ", total " + cobrado.calcularTotal());
    }

    private Plato buscarPlato(String codigo) {
        return controlador.getMenu()
                .stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe ese plato."));
    }

    private void guardarPlato(boolean editar) throws IOException {
        exigirGerente();
        String codigo = requerido("Código: ");
        List<Plato> menu = controlador.getMenu();
        Plato anterior = editar ? buscarPlato(codigo) : null;
        Plato plato = new Plato(codigo, requerido("Nombre: "), requerido("Descripción: "),
                decimal("Precio: "), requerido("Categoría (" + String.join("/", ControladorRestaurante.getCategorias()) + "): "));
        if (anterior != null)
            menu.removeIf(p -> p.getCodigo().equalsIgnoreCase(codigo));
        menu.add(plato);
        controlador.guardarMenu(menu);
        salida.println("Menú guardado.");
    }

    private void eliminarPlato() throws IOException {
        exigirGerente();
        String codigo = requerido("Código a eliminar: ");
        buscarPlato(codigo);
        if (!"S".equalsIgnoreCase(requerido("¿Confirmar eliminación? (S/N): ")))
            return;
        List<Plato> menu = controlador.getMenu();
        menu.removeIf(p -> p.getCodigo().equalsIgnoreCase(codigo));
        controlador.guardarMenu(menu);
    }

    private void inventario() {
        for (Insumo i : controlador.getInventario())
            salida.println(i.getNombre() + " | " + i.getCantidad() + " " + i.getUnidad());
    }

    private void guardarInsumo() throws IOException {
        exigirGerente();
        String nombre = requerido("Nombre del insumo: ");
        Insumo insumo = new Insumo(nombre, requerido("Unidad: "), decimal("Existencia total: "));
        List<Insumo> inventario = controlador.getInventario();
        inventario.removeIf(i -> i.getNombre().equalsIgnoreCase(nombre));
        inventario.add(insumo);
        controlador.guardarInventario(inventario);
    }

    private void eliminarInsumo() throws IOException {
        exigirGerente();
        String nombre = requerido("Insumo a eliminar: ");
        if (!"S".equalsIgnoreCase(requerido("¿Confirmar eliminación? (S/N): ")))
            return;
        List<Insumo> inventario = controlador.getInventario();
        if (!inventario.removeIf(i -> i.getNombre().equalsIgnoreCase(nombre)))
            throw new IllegalArgumentException("No existe ese insumo.");
        controlador.guardarInventario(inventario);
    }

    private void receta() throws IOException {
        exigirGerente();
        String codigo = requerido("Código de plato: ");
        List<IngredienteReceta> ingredientes = new ArrayList<>();
        while (true) {
            String insumo = requerido("Nombre de insumo (0 terminar): ");
            if ("0".equals(insumo))
                break;
            ingredientes.add(new IngredienteReceta(
                    insumo, requerido("Unidad: "), decimal("Cantidad por plato: ")));
        }
        controlador.guardarReceta(codigo, ingredientes);
    }

    private void exigirGerente() {
        if (!controlador.esGerente())
            throw new SecurityException("Acceso exclusivo del gerente.");
    }

    private String leer(String mensaje) throws IOException {
        salida.print(mensaje);
        salida.flush();
        String linea = entrada.readLine();
        return linea == null ? null : linea.trim();
    }
    private String requerido(String mensaje) throws IOException {
        String texto = leer(mensaje);
        if (texto == null)
            throw new EOFException("Fin de la entrada.");
        return texto;
    }
    private int entero(String mensaje) throws IOException {
        return Integer.parseInt(requerido(mensaje));
    }
    private double decimal(String mensaje) throws IOException {
        return Double.parseDouble(requerido(mensaje).replace(',', '.'));
    }
}
