package restaurante;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import restaurante.controlador.ControladorRestaurante;
import restaurante.datos.ArchivosTxt;
import restaurante.entidades.*;
import restaurante.presentacion.*;
import restaurante.seguridad.*;
import restaurante.servicios.ServicioRestaurante;

/** Caso de cinco hamburguesas personalizadas y capacidad real de 30 ventas de cada producto. */
public final class PersonalizacionesTest {
    private static int pruebas;
    private interface Accion {
        void ejecutar() throws Exception;
    }
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor)
            throw new AssertionError(mensaje);
        pruebas++;
        System.out.println("OK " + mensaje);
    }
    private static void rechazar(Accion accion, String mensaje) throws Exception {
        try {
            accion.ejecutar();
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            comprobar(true, mensaje);
            return;
        }
        throw new AssertionError(mensaje);
    }
    private static Object campo(Object objeto, String nombre) throws Exception {
        Field f = objeto.getClass().getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(objeto);
    }
    private static ControladorRestaurante entrar(ServicioRestaurante s) throws IOException {
        return new ControladorRestaurante(s,
                new Autenticacion(s.getDirectorio())
                        .autenticar(Rol.GERENTE, "gerente123".toCharArray()));
    }
    private static Plato plato(ControladorRestaurante c, String codigo) {
        return c.getMenu().stream().filter(p -> p.getCodigo().equals(codigo)).findFirst().get();
    }
    private static double stock(ControladorRestaurante c, String nombre) {
        return c.getInventario()
                .stream()
                .filter(i -> i.getNombre().equals(nombre))
                .findFirst()
                .get()
                .getCantidad();
    }
    private static DetallePedido unidad(ControladorRestaurante c) {
        Plato p = plato(c, "H002");
        return new DetallePedido(p, 1, p.getPrecio());
    }
    private static AdicionalPedido queso(ControladorRestaurante c) {
        Plato p = plato(c, "E003");
        return new AdicionalPedido(p, 1, p.getPrecio());
    }
    private static void distribuir(Container contenedor) {
        contenedor.doLayout();
        for (Component hijo : contenedor.getComponents())
            if (hijo instanceof Container)
                distribuir((Container) hijo);
    }
    private static void capturar(JPanel panel, String nombre) throws Exception {
        int ancho=panel instanceof PersonalizarPlato?760:1320;
        int alto=panel instanceof PersonalizarPlato?530:900;
        panel.addNotify();
        panel.setSize(ancho, alto);
        distribuir(panel);
        if (panel instanceof PanelPedidos)
            ((PanelPedidos) panel).refrescarCatalogo();
        distribuir(panel);
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        panel.printAll(g);
        g.dispose();
        panel.removeNotify();
        Path destino = Paths.get("build/capturas-prueba");
        Files.createDirectories(destino);
        ImageIO.write(imagen, "png", destino.resolve(nombre + ".png").toFile());
    }
    private static void interfaz(Path carpeta) throws Exception {
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            ControladorRestaurante c = entrar(servicio);
            comprobar(c.getMenu().stream().filter(p -> !p.esAdicional())
                            .allMatch(p -> Imagenes.producto(p, 180, 134) instanceof ImageIcon),
                    "todos los productos del catálogo usan fotografías PNG en las tarjetas");
            PanelPedidos vista = new PanelPedidos(c);
            ((JTextField) campo(vista, "campoBusqueda")).setText("H002");
            JPanel productos = (JPanel) campo(vista, "panelProductos");
            JButton agregar = (JButton) campo(productos.getComponent(0), "btnAgregar");
            for (int i = 0; i < 5; i++) agregar.doClick();
            comprobar(vista.getUnidades().size() == 5,
                    "cinco hamburguesas producen cinco unidades independientes");
            for (int i = 0; i < 5; i++) {
                PersonalizarPlato formulario = new PersonalizarPlato(vista.getUnidades().get(i), c);
                ((JTextField) campo(formulario, "comensal")).setText("Silla " + (i + 1));
                if (i < 2) {
                    DefaultTableModel modelo = (DefaultTableModel) campo(formulario, "modelo");
                    for (int fila = 0; fila < modelo.getRowCount(); fila++)
                        if ("Queso extra".equals(modelo.getValueAt(fila, 0)))
                            modelo.setValueAt(1, fila, 2);
                }
                if (i == 2) {
                    @SuppressWarnings("unchecked")
                    Map<String, JCheckBox> sin =
                            (Map<String, JCheckBox>) campo(formulario, "exclusiones");
                    sin.get("Tomate").setSelected(true);
                    ((JTextArea) campo(formulario, "indicaciones"))
                            .setText("Poco picante | servir primero\nSilla #3");
                    capturar(formulario, "formulario-personalizacion");
                }
                vista.personalizarUnidad(i, formulario.aplicar());
            }
            java.util.List<DetallePedido> lista = vista.getUnidades();
            comprobar(lista.get(0).getAdicionales().size() == 1
                            && lista.get(1).getAdicionales().size() == 1
                            && lista.get(2).getAdicionales().isEmpty(),
                    "los dos quesos pertenecen solamente a sus dos hamburguesas");
            comprobar(lista.get(2).getSinIngredientes().equals(Arrays.asList("Tomate"))
                            && lista.get(1).getSinIngredientes().isEmpty(),
                    "solo la tercera hamburguesa va sin tomate");
            comprobar(lista.stream().mapToDouble(DetallePedido::calcularSubtotal).sum() == 98000,
                    "total incluye cinco platos y dos adicionales sin duplicar cargos");
            JPanel unidades = (JPanel) campo(vista, "listaUnidades");
            comprobar(unidades.getComponentCount() == 5
                            && ((JLabel)((JPanel)unidades.getComponent(0)).getComponent(0)).getText().contains("Queso extra")
                            && ((JLabel)((JPanel)unidades.getComponent(2)).getComponent(0)).getText().contains("SIN Tomate"),
                    "adicionales, exclusión e indicaciones aparecen bajo sus unidades");
            JPanel categorias = (JPanel) campo(vista, "panelCategorias");
            comprobar(categorias.getComponentCount() == 8
                            && Arrays.stream(categorias.getComponents())
                                    .noneMatch(b -> ((JButton) b).getText().equals("Extras")),
                    "el catálogo no presenta una pestaña de adicionales independientes");
            double tomate = stock(c, "Tomate"), cheddar = stock(c, "Queso cheddar");
            Pedido abierto = vista.guardarPedidoActual();
            comprobar(c.getPedidos().isEmpty() && stock(c, "Tomate") == tomate,
                    "guardar personalizaciones no consume inventario");
            PanelPedidos recuperada = new PanelPedidos(c);
            comprobar(recuperada.cargarPedidoAbierto(abierto.getNumero()),
                    "otra vista recupera el pedido personalizado");
            comprobar(recuperada.getUnidades().get(2).getObservaciones().contains("servir primero"),
                    "recuperar conserva las indicaciones de preparación");
            String resumen = ResumenPedido.texto("Mesa de prueba", recuperada.getUnidades());
            comprobar(resumen.contains("UNIDAD 3") && resumen.contains("SIN Tomate")
                            && resumen.contains("Silla 3"),
                    "la comanda identifica explícitamente la unidad y su comensal");
            capturar(recuperada, "pedido-personalizado");
            ((JTextField) campo(vista, "campoBusqueda")).setText("");
            vista.mostrarCategoria("Pizzas");
            capturar(vista, "menu-pizzas");
            vista.mostrarCategoria("Salchipapas");
            capturar(vista, "menu-salchipapas");
            vista.mostrarCategoria("Perros calientes");
            capturar(vista, "menu-perros");
            Pedido venta = c.cobrarPedidoAbierto(abierto.getNumero(), recuperada.getUnidades());
            comprobar(venta.calcularTotal() == 98000 && stock(c, "Tomate") == tomate - 80,
                    "cobro descuenta tomate para cuatro hamburguesas, no para cinco");
            comprobar(stock(c, "Queso cheddar") == cheddar - 7,
                    "cobro descuenta los cinco quesos base y los dos adicionales");
            PanelReportes reporte = new PanelReportes(c::getReporteVentas);
            ((JTable) campo(reporte, "tabla")).setRowSelectionInterval(0, 0);
            comprobar(reporte.getDetalleSeleccionado().contains("SIN Tomate")
                            && reporte.getDetalleSeleccionado().contains("Silla 2"),
                    "el reporte conserva la comanda completa después del cobro");
            ImageIO.write(new Logotipo(512).imagen(), "png",
                    Paths.get("build/capturas-prueba/logotipo.png").toFile());
        }
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            DetallePedido tercero = servicio.getPedidos().get(0).getDetalles().get(2);
            comprobar(tercero.getSinIngredientes().equals(Arrays.asList("Tomate"))
                            && tercero.getObservaciones().contains("| servir primero\nSilla #3"),
                    "persistencia conserva asociaciones y caracteres especiales tras reiniciar");
            comprobar(servicio.getPedidos().get(0).getDetalles().get(0).getAdicionales().size() == 1
                            && servicio.getPedidos()
                                    .get(0)
                                    .getDetalles()
                                    .get(4)
                                    .getAdicionales()
                                    .isEmpty(),
                    "las asociaciones de adicionales sobreviven al reinicio");
        }
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Tema.instalar();
        Path carpeta = Files.createTempDirectory("la-doble-personalizaciones-");
        SwingUtilities.invokeAndWait(() -> {
            try {
                interfaz(carpeta);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            ControladorRestaurante c = entrar(servicio);
            rechazar(()
                             -> c.registrarPedido(null,
                                     Arrays.asList(new DetallePedido(plato(c, "E003"), 1, 4000))),
                    "un adicional no puede ser un producto independiente");
            DetallePedido duplicado = unidad(c);
            duplicado.setAdicionales(Arrays.asList(queso(c), queso(c)));
            rechazar(()
                             -> c.validarUnidad(duplicado),
                    "adicional repetido rechazado en una misma unidad");
            DetallePedido omitido = unidad(c);
            omitido.setSinIngredientes(Arrays.asList("Ingrediente inexistente"));
            rechazar(()
                             -> c.validarUnidad(omitido),
                    "no se puede omitir un ingrediente ajeno a la receta");
            DetallePedido conflicto = unidad(c);
            conflicto.setSinIngredientes(Arrays.asList("Queso cheddar"));
            conflicto.setAdicionales(Arrays.asList(queso(c)));
            rechazar(()
                             -> c.validarUnidad(conflicto),
                    "se rechaza un adicional que contradiga un ingrediente retirado");
            DetallePedido agrupado = unidad(c);
            agrupado.setCantidad(2);
            agrupado.setAdicionales(Arrays.asList(queso(c)));
            rechazar(()
                             -> c.validarUnidad(agrupado),
                    "cada unidad personalizada debe identificarse por separado");
            DetallePedido bebida = new DetallePedido(plato(c, "B001"), 1, 4000);
            bebida.setAdicionales(Arrays.asList(queso(c)));
            rechazar(
                    () -> c.validarUnidad(bebida), "adicionales de comida no se asignan a bebidas");
            DetallePedido vacio = unidad(c);
            vacio.setSinIngredientes(c.getReceta("H002")
                            .stream()
                            .map(IngredienteReceta::getInsumo)
                            .collect(java.util.stream.Collectors.toList()));
            rechazar(() -> c.validarUnidad(vacio), "no se puede retirar toda la receta");
        }
        Path jornada = Files.createTempDirectory("la-doble-30-todo-");
        try (ServicioRestaurante servicio = new ServicioRestaurante(jornada)) {
            ControladorRestaurante c = entrar(servicio);
            comprobar(c.getMenu().size() == 22,
                    "el menú contiene diez productos nuevos y conserva los doce originales");
            java.util.List<DetallePedido> todo = new ArrayList<>();
            for (Plato p : c.getMenu())
                if (!p.esAdicional())
                    for (int i = 0; i < 30; i++) {
                        DetallePedido d = new DetallePedido(p, 1, p.getPrecio());
                        if (p.getCodigo().equals("H001")) {
                            java.util.List<AdicionalPedido> extras = new ArrayList<>();
                            for (Plato extra : c.getMenu())
                                if (extra.esAdicional())
                                    extras.add(new AdicionalPedido(extra, 1, extra.getPrecio()));
                            d.setAdicionales(extras);
                        }
                        todo.add(d);
                    }
            comprobar(todo.size() == 570,
                    "se preparan treinta unidades de cada uno de los diecinueve productos "
                    + "principales");
            c.registrarPedido(null, todo);
            comprobar(c.getInventario().stream().allMatch(i -> i.getCantidad() == 0),
                    "stock suficiente para 30 de CADA producto y 30 de cada adicional "
                    + "simultáneamente");
            rechazar(()
                             -> c.registrarPedido(null, Arrays.asList(unidad(c))),
                    "el inventario agotado impide una venta adicional");
        }
        try (ServicioRestaurante servicio = new ServicioRestaurante(jornada)) {
            comprobar(servicio.getInventario().stream().allMatch(i -> i.getCantidad() == 0),
                    "reiniciar no repone las existencias agotadas");
        }
        Path antigua = Files.createTempDirectory("la-doble-migracion-menu-");
        java.util.List<String> menuViejo =
                new ArrayList<>(Arrays.asList("# Menú anterior con un precio propio"));
        for (Plato p : restaurante.datos.CatalogoMenu.getPlatos())
            if (p.getCodigo().matches("[HCBE]00[123]"))
                menuViejo.add(ArchivosTxt.fila(p.getCodigo(), p.getNombre(), p.getDescripcion(),
                        p.getCodigo().equals("H002") ? 19001 : p.getPrecio(),
                        p.esAdicional() ? "Extras" : p.getCategoria()));
        ArchivosTxt.guardar(antigua.resolve("menu.txt"), menuViejo);
        java.util.List<String> recetasViejas = new ArrayList<>();
        for (String linea : Files.readAllLines(Paths.get("src/restaurante/config/recetas.txt")))
            if (linea.startsWith("#") || linea.matches("[HCBE]00[123]\\|.*"))
                recetasViejas.add(linea);
        ArchivosTxt.guardar(antigua.resolve("recetas.txt"), recetasViejas);
        ArchivosTxt.guardar(antigua.resolve("inventario.txt"),
                Arrays.asList("Pan hamburguesa|unidades|20", "Insumo personal|kg|5"));
        byte[] originalMenu = Files.readAllBytes(antigua.resolve("menu.txt"));
        double restante;
        try (ServicioRestaurante servicio = new ServicioRestaurante(antigua)) {
            ControladorRestaurante c = entrar(servicio);
            comprobar(c.getMenu().size() == 22,
                    "un directorio antiguo recibe los diez productos nuevos");
            comprobar(plato(c, "H002").getPrecio() == 19001,
                    "la ampliación conserva los precios editados del menú anterior");
            comprobar(stock(c, "Insumo personal") == 5,
                    "la ampliación conserva los insumos ajenos al catálogo de ejemplo");
            comprobar(Arrays.equals(originalMenu,
                              Files.readAllBytes(
                                      antigua.resolve(".respaldo-menu-restaurante-v3/menu.txt"))),
                    "se conserva una copia exacta del menú previo antes de ampliar");
            comprobar(c.getMenu().stream().noneMatch(p -> p.getCategoria().equals("Extras")),
                    "los adicionales antiguos se reclasifican sin perder sus códigos");
            c.registrarPedido(null, Arrays.asList(new DetallePedido(plato(c, "H001"), 1, 28900)));
            restante = stock(c, "Pan hamburguesa");
        }
        try (ServicioRestaurante servicio = new ServicioRestaurante(antigua)) {
            comprobar(stock(entrar(servicio), "Pan hamburguesa") == restante,
                    "la migración se aplica una vez y no repone las ventas de un directorio "
                    + "actualizado");
        }
        System.out.println("TOTAL PERSONALIZACIONES: " + pruebas + " comprobaciones.");
    }
}
