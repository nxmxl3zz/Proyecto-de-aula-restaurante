package restaurante;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import restaurante.controlador.ControladorRestaurante;
import restaurante.entidades.*;
import restaurante.presentacion.*;
import restaurante.seguridad.*;
import restaurante.servicios.ServicioRestaurante;

/** Regresiones del flujo por unidad, existencias, filtros y tamaños de los paneles reales. */
public final class ExperienciaUsuarioTest {
    private static int comprobaciones;
    private static Object campo(Object objeto, String nombre) throws Exception {
        Field campo = objeto.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        return campo.get(objeto);
    }
    private static JButton boton(Object objeto, String nombre) throws Exception {
        return (JButton) campo(objeto, nombre);
    }
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion)
            throw new AssertionError(mensaje);
        comprobaciones++;
        System.out.println("OK " + mensaje);
    }
    private static JButton buscarBoton(Container contenedor, String nombre) {
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof JButton
                    && nombre.equals(hijo.getAccessibleContext().getAccessibleName()))
                return (JButton) hijo;
            if (hijo instanceof Container) {
                JButton encontrado = buscarBoton((Container) hijo, nombre);
                if (encontrado != null)
                    return encontrado;
            }
        }
        return null;
    }
    private static void distribuir(Container contenedor) {
        contenedor.doLayout();
        for (Component hijo : contenedor.getComponents())
            if (hijo instanceof Container)
                distribuir((Container) hijo);
    }
    private static void capturar(JPanel panel, String nombre, int ancho, int alto)
            throws Exception {
        panel.addNotify();
        panel.setSize(ancho, alto);
        distribuir(panel);
        if (panel instanceof PanelPedidos)
            ((PanelPedidos) panel).refrescarCatalogo();
        distribuir(panel);
        distribuir(panel);
        if (panel instanceof PanelPedidos) {
            JPanel productos = (JPanel) campo(panel, "panelProductos");
            for (Component producto : productos.getComponents()) {
                JLabel descripcion = (JLabel) campo(producto, "descripcion");
                JButton agregar = boton(producto, "btnAgregar");
                comprobar(!descripcion.isVisible()
                                || descripcion.getHeight() >= descripcion.getPreferredSize().height,
                        "descripción completa a " + ancho + " px");
                comprobar(agregar.getWidth() >= agregar.getPreferredSize().width,
                        "acción Agregar completa a " + ancho + " px");
            }
            JScrollPane pedido = (JScrollPane) campo(panel, "scrollCarrito");
            comprobar(pedido.getViewport().getExtentSize().width >= 320,
                    "pedido mantiene un ancho legible a " + ancho + " px");
            comprobar(boton(panel, "btnCobrar").getHeight() >= 34,
                    "cobro accesible a " + ancho + " px");
            JButton agregar = boton(productos.getComponent(0), "btnAgregar");
            comprobar(agregar.getVisibleRect().height == agregar.getHeight(),
                    "primer producto permite agregar sin desplazar a " + ancho + " px");
            JPanel unidades = (JPanel) campo(panel, "listaUnidades");
            JButton editar = boton(unidades.getComponent(0), "btnPersonalizar");
            comprobar(editar.getVisibleRect().height == editar.getHeight(),
                    "primera unidad permite personalizar sin desplazar a " + ancho + " px");
        }
        if (panel instanceof PanelInicio) {
            JButton continuar = buscarBoton(panel, "Continuar pedido");
            comprobar(continuar.getVisibleRect().height == continuar.getHeight(),
                    "Inicio muestra la acción Continuar a " + ancho + " px");
        }
        if (panel instanceof PanelMesas) {
            JButton sillas = boton(panel, "btnSillas");
            comprobar(sillas.getVisibleRect().height == sillas.getHeight(),
                    "Mesas muestra todas sus acciones a " + ancho + " px");
        }
        if (panel instanceof PanelMenu) {
            JButton receta = boton(panel, "btnReceta");
            comprobar(receta.getParent() != null && receta.getVisibleRect().height > 0,
                    "Menú permite abrir recetas a " + ancho + " px");
        }
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafica = imagen.createGraphics();
        panel.printAll(grafica);
        grafica.dispose();
        panel.removeNotify();
        Path destino = Paths.get("build/capturas-prueba");
        Files.createDirectories(destino);
        ImageIO.write(imagen, "png", destino.resolve(nombre + ".png").toFile());
    }
    private static Pedido venta(int numero, LocalDate fecha, Plato plato) {
        Pedido pedido = new Pedido();
        pedido.setNumero(numero);
        pedido.setFecha(fecha);
        pedido.getDetalles().add(new DetallePedido(plato, 1, plato.getPrecio()));
        return pedido;
    }
    private static void ejecutar() throws Exception {
        Path carpeta = Files.createTempDirectory("restaurante-experiencia-");
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            Sesion sesion =
                    new Autenticacion(carpeta).autenticar(Rol.GERENTE, "gerente123".toCharArray());
            ControladorRestaurante c = new ControladorRestaurante(servicio, sesion);
            PanelPedidos pedidos = new PanelPedidos(c);
            comprobar(!boton(pedidos, "btnCobrar").isEnabled()
                            && !boton(pedidos, "btnGuardar").isEnabled(),
                    "pedido vacío deshabilita cobro y guardado");
            JTextField busqueda = (JTextField) campo(pedidos, "campoBusqueda");
            JPanel productos = (JPanel) campo(pedidos, "panelProductos");
            busqueda.setText("Z001");
            comprobar(productos.getComponentCount() == 1,
                    "la búsqueda encuentra pizza desde Hamburguesas");
            busqueda.postActionEvent();
            comprobar(
                    pedidos.getUnidades().size() == 1, "Enter agrega el resultado de la búsqueda");
            JPanel unidades = (JPanel) campo(pedidos, "listaUnidades");
            boton(unidades.getComponent(0), "btnQuitar").doClick(0);
            busqueda.setText("H002");
            busqueda.postActionEvent();
            PersonalizarPlato personalizar = new PersonalizarPlato(pedidos.getUnidades().get(0), c);
            JButton mas = buscarBoton(personalizar, "Agregar una porción de Queso extra");
            mas.doClick(0);
            mas.doClick(0);
            comprobar(((JLabel) campo(personalizar, "precioFinal")).getText().contains("26.000"),
                    "dos porciones de queso actualizan el precio antes de aplicar");
            for (int i = 0; i < 8; i++) mas.doClick(0);
            comprobar(!mas.isEnabled()
                            && personalizar.aplicar().getAdicionales().get(0).getCantidad() == 5,
                    "el control detiene adicionales en cinco porciones");
            JButton menos = buscarBoton(personalizar, "Quitar una porción de Queso extra");
            for (int i = 0; i < 4; i++) menos.doClick(0);
            @SuppressWarnings("unchecked")
            Map<String, JCheckBox> exclusiones =
                    (Map<String, JCheckBox>) campo(personalizar, "exclusiones");
            exclusiones.get("Tomate").doClick(0);
            ((JTextField) campo(personalizar, "comensal")).setText("Silla 1");
            comprobar(((JLabel) campo(personalizar, "resumen")).getText().contains("SIN Tomate"),
                    "el resumen actualiza ingredientes omitidos");
            pedidos.personalizarUnidad(0, personalizar.aplicar());
            boton(unidades.getComponent(0), "btnDuplicar").doClick(0);
            comprobar(pedidos.getUnidades().size() == 2
                            && pedidos.getUnidades().get(1).getAdicionales().size() == 1,
                    "duplicar desde la unidad conserva su personalización");
            boton(unidades.getComponent(0), "btnQuitar").doClick(0);
            comprobar(pedidos.getUnidades().get(0).getSinIngredientes().contains("Tomate"),
                    "quitar una unidad conserva la preparación de la restante");
            Pedido guardado = pedidos.guardarPedidoActual();
            comprobar(!pedidos.tienePedidoPendiente() && !boton(pedidos, "btnGuardar").isEnabled(),
                    "guardar actualiza el estado persistente y deshabilita guardado redundante");
            PanelInicio inicio = new PanelInicio();
            final int[] retomado = {-1};
            inicio.alContinuar(numero -> retomado[0] = numero);
            java.util.List<Pedido> historial =
                    Arrays.asList(venta(21, LocalDate.now(), c.getMenu().get(0)),
                            venta(22, LocalDate.now().minusDays(6), c.getMenu().get(0)),
                            venta(23, LocalDate.now().minusDays(7), c.getMenu().get(0)));
            inicio.refrescar(historial, 2, c.getPedidosAbiertos());
            comprobar("1".equals(((JLabel) campo(inicio, "lblPedidos")).getText())
                            && "8 de 10".equals(((JLabel) campo(inicio, "lblMesas")).getText()),
                    "Inicio cuenta ventas de hoy y mesas libres");
            buscarBoton(inicio, "Continuar pedido").doClick(0);
            comprobar(
                    retomado[0] == guardado.getNumero(), "Inicio retoma el identificador correcto");
            PanelReportes reportes = new PanelReportes(() -> historial);
            JComboBox<?> periodo = (JComboBox<?>) campo(reportes, "periodo");
            JTable tabla = (JTable) campo(reportes, "tabla");
            periodo.setSelectedItem("Hoy");
            comprobar(tabla.getRowCount() == 1, "filtro Hoy excluye otras fechas");
            periodo.setSelectedItem("Últimos 7 días");
            comprobar(tabla.getRowCount() == 2, "siete días incluye el límite inicial");
            periodo.setSelectedItem("Intervalo");
            ((JTextField) campo(reportes, "desde")).setText(LocalDate.now().plusDays(1).toString());
            reportes.refrescar();
            comprobar(tabla.getRowCount() == 1
                            && ((JLabel) campo(reportes, "errorFiltro"))
                                    .getText()
                                    .contains("Revisa"),
                    "intervalo invertido conserva el último resultado y muestra error");
            ((JTextField) campo(reportes, "desde")).setText("2026-02-30");
            reportes.refrescar();
            comprobar(tabla.getRowCount() == 1, "fecha inexistente no reemplaza el reporte válido");
            periodo.setSelectedItem("Hoy");
            c.unirMesas(Arrays.asList(2, 4));
            PanelMesas mesas = new PanelMesas(c, numero -> true);
            comprobar(boton(mesas, "mesa2").getParent() == boton(mesas, "mesa4").getParent(),
                    "mesas unidas comparten un recuadro visual");
            PanelInventario inventario = new PanelInventario(c);
            ((JTextField) campo(inventario, "campoBusqueda")).setText("Pan hamburguesa");
            comprobar(((JTable) campo(inventario, "tabla")).getRowCount() == 1,
                    "inventario filtra insumos por nombre");
            ((JTextField) campo(inventario, "campoBusqueda")).setText("");
            PanelMenu menu = new PanelMenu(c);
            busqueda.setText("");
            pedidos.mostrarCategoria("Hamburguesas");
            for (int ancho : new int[] {1024, 1320}) {
                int alto = ancho == 1024 ? 600 : 780;
                String sufijo = ancho == 1024 ? "-compacto" : "-actual";
                capturar(pedidos, "pedido" + sufijo, ancho, alto);
                capturar(inicio, "inicio" + sufijo, ancho, alto);
                capturar(mesas, "mesas" + sufijo, ancho, alto);
                capturar(inventario, "inventario" + sufijo, ancho, alto);
                capturar(reportes, "reportes" + sufijo, ancho, alto);
                capturar(menu, "menu" + sufijo, ancho, alto);
            }
            capturar(personalizar, "personalizacion-compacta", 620, 530);
            pedidos.mostrarCategoria("Platos");
            capturar(pedidos, "menu-platos", 1320, 900);
            java.util.List<Insumo> stock = new ArrayList<>(c.getInventario());
            for (int i = 0; i < stock.size(); i++)
                if (stock.get(i).getNombre().equals("Pan hamburguesa"))
                    stock.set(i, new Insumo("Pan hamburguesa", stock.get(i).getUnidad(), 1));
            c.guardarInventario(stock);
            PanelPedidos limitado = new PanelPedidos(c);
            JTextField buscar = (JTextField) campo(limitado, "campoBusqueda");
            buscar.setText("H002");
            buscar.postActionEvent();
            JPanel oferta = (JPanel) campo(limitado, "panelProductos");
            comprobar(!boton(oferta.getComponent(0), "btnAgregar").isEnabled(),
                    "disponibilidad descuenta lo ya incluido en el carrito");
            JPanel lineas = (JPanel) campo(limitado, "listaUnidades");
            boton(lineas.getComponent(0), "btnDuplicar").doClick(0);
            comprobar(limitado.getUnidades().size() == 1,
                    "duplicar con existencias insuficientes conserva el pedido");
            comprobar(c.getInventario().stream().anyMatch(
                              i -> i.getNombre().equals("Pan hamburguesa") && i.getCantidad() == 1),
                    "la comprobación de disponibilidad no consume inventario");
            boton(lineas.getComponent(0), "btnQuitar").doClick(0);
            comprobar(boton(oferta.getComponent(0), "btnAgregar").isEnabled(),
                    "quitar la unidad vuelve a habilitar el producto");
            sesion.cerrar();
            try {
                c.comprobarExistencias(Collections.emptyList());
                throw new AssertionError("sesión vencida");
            } catch (SecurityException esperada) {
                comprobar(true, "consulta de existencias exige sesión activa");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Tema.instalar();
        SwingUtilities.invokeAndWait(() -> {
            try {
                ejecutar();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        System.out.println("TOTAL EXPERIENCIA: " + comprobaciones + " comprobaciones.");
    }
}
