package restaurante;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import restaurante.controlador.ControladorRestaurante;
import restaurante.datos.CatalogoMenu;
import restaurante.entidades.AdicionalPedido;
import restaurante.entidades.DetallePedido;
import restaurante.entidades.EstadoPedido;
import restaurante.entidades.Pedido;
import restaurante.entidades.Plato;
import restaurante.presentacion.PanelInventario;
import restaurante.presentacion.PanelMenu;
import restaurante.presentacion.Tema;
import restaurante.seguridad.Autenticacion;
import restaurante.seguridad.Rol;
import restaurante.seguridad.Sesion;
import restaurante.servicios.ServicioRestaurante;
import restaurante.servicios.ValidacionException;

/** Regresiones de aislamiento, validación y guardados aparentes detectados en la limpieza. */
public final class LimpiezaTest {
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

    private static void rechazar(Class<? extends Exception> tipo, Accion accion, String mensaje)
            throws Exception {
        try {
            accion.ejecutar();
        } catch (Exception ex) {
            if (!tipo.isInstance(ex))
                throw ex;
            comprobar(true, mensaje);
            return;
        }
        throw new AssertionError(mensaje);
    }

    private static Object campo(Object objeto, String nombre) throws Exception {
        Field campo = objeto.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        return campo.get(objeto);
    }

    private static void invocar(Object objeto, String nombre, Class<?>[] tipos, Object... valores)
            throws Exception {
        Method metodo = objeto.getClass().getDeclaredMethod(nombre, tipos);
        metodo.setAccessible(true);
        try {
            metodo.invoke(objeto, valores);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception)
                throw (Exception) ex.getCause();
            throw ex;
        }
    }

    private static Plato plato(ServicioRestaurante servicio, String codigo) {
        return servicio.getMenu()
                .stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst()
                .get();
    }

    private static void copiasYValidacion() throws Exception {
        Path carpeta = Files.createTempDirectory("restaurante-limpieza-");
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            Sesion sesion =
                    new Autenticacion(carpeta).autenticar(Rol.GERENTE, "gerente123".toCharArray());
            Plato base = plato(servicio, "H002");
            Plato queso = plato(servicio, "E003");
            AdicionalPedido adicional = new AdicionalPedido(queso, 2, queso.getPrecio());
            queso.setNombre("Cambio externo");
            comprobar(!adicional.getPlato().getNombre().equals(queso.getNombre()),
                    "cambiar el plato original no modifica un adicional ya creado");
            Plato expuesto = adicional.getPlato();
            expuesto.setPrecio(1);
            comprobar(adicional.getPlato().getPrecio() != 1,
                    "consultar un adicional no expone su plato interno");

            DetallePedido unidad = new DetallePedido(base, 1, base.getPrecio());
            unidad.setComensal("Silla 2");
            unidad.setObservaciones("Servir primero");
            unidad.setAdicionales(Arrays.asList(adicional));
            unidad.setSinIngredientes(Arrays.asList("Tomate"));
            DetallePedido copia = unidad.copia();
            comprobar(copia.calcularSubtotal() == unidad.calcularSubtotal()
                            && copia.getComensal().equals("Silla 2")
                            && copia.getObservaciones().equals("Servir primero"),
                    "copiar una unidad conserva su total, comensal e indicaciones");
            base.setNombre("Otro plato");
            unidad.setAdicionales(null);
            unidad.setSinIngredientes(null);
            comprobar(!copia.getPlato().getNombre().equals(base.getNombre())
                            && copia.getAdicionales().size() == 1
                            && copia.getSinIngredientes().equals(Arrays.asList("Tomate")),
                    "editar una unidad no altera las copias ni sus personalizaciones");
            rechazar(UnsupportedOperationException.class,
                    ()
                            -> copia.getAdicionales().clear(),
                    "la consulta de adicionales impide modificar la unidad por fuera");
            rechazar(UnsupportedOperationException.class,
                    ()
                            -> copia.getSinIngredientes().clear(),
                    "la consulta de exclusiones impide modificar la unidad por fuera");
            comprobar(new DetallePedido().copia().getPlato() == null,
                    "copiar un detalle sin completar conserva la ausencia de plato");
            List<Plato> catalogo = CatalogoMenu.getPlatos();
            catalogo.get(0).setNombre("Cambio externo");
            comprobar(!CatalogoMenu.getPlatos().get(0).getNombre().equals("Cambio externo"),
                    "el catálogo de ejemplo mantiene aislados sus platos");
            List<Plato> menu = servicio.getMenu();
            menu.get(0).setCodigo("CAMBIO");
            comprobar(!servicio.getMenu().get(0).getCodigo().equals("CAMBIO"),
                    "el menú persistido no cambia a través de una consulta");

            Plato minusculas = plato(servicio, "H002");
            minusculas.setCodigo("h002");
            DetallePedido validar = new DetallePedido(minusculas, 1, minusculas.getPrecio());
            Plato extra = plato(servicio, "E003");
            extra.setCodigo("e003");
            validar.setAdicionales(Arrays.asList(new AdicionalPedido(extra, 1, extra.getPrecio())));
            comprobar(servicio.validarUnidad(validar).getAdicionales().size() == 1,
                    "los códigos de platos y adicionales siguen admitiendo minúsculas");
            Plato ajeno = new Plato("NO_EXISTE", "Ajeno", "Prueba", 100, "Platos");
            rechazar(ValidacionException.class,
                    ()
                            -> servicio.validarUnidad(new DetallePedido(ajeno, 1, 100)),
                    "un código desconocido produce un error de validación explícito");
            ajeno.setCodigo(null);
            rechazar(ValidacionException.class,
                    ()
                            -> servicio.validarUnidad(new DetallePedido(ajeno, 1, 100)),
                    "un código nulo produce un error de validación explícito");
            validar.setAdicionales(Arrays.asList(new AdicionalPedido(
                    new Plato("NO_EXISTE", "Ajeno", "Prueba", 100, "Adicionales"), 1, 100)));
            rechazar(ValidacionException.class,
                    ()
                            -> servicio.validarUnidad(validar),
                    "un adicional desconocido no se acepta como disponible");
            Plato principal = plato(servicio, "H002");
            validar.setAdicionales(Arrays.asList(new AdicionalPedido(principal, 1, 100)));
            rechazar(ValidacionException.class,
                    ()
                            -> servicio.validarUnidad(validar),
                    "un plato principal no puede usarse como adicional");
            comprobar(servicio.getPedidos().isEmpty() && servicio.getPedidosAbiertos().isEmpty(),
                    "validar unidades no registra ventas ni atenciones");

            Pedido guardado =
                    servicio.guardarPedidoAbierto(sesion, null, null, Arrays.asList(copia));
            copia.getPlato().setNombre("Cambio posterior");
            comprobar(guardado.getEstado() == EstadoPedido.PENDIENTE
                            && !servicio.getPedidosAbiertos()
                                    .get(0)
                                    .getDetalles()
                                    .get(0)
                                    .getPlato()
                                    .getNombre()
                                    .equals("Cambio posterior"),
                    "guardar una copia conserva el aislamiento del pedido pendiente");
        }
    }

    private static void erroresDeLectura() throws Exception {
        Path carpeta = Files.createTempDirectory("restaurante-lectura-fallida-");
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            comprobar(!servicio.getMenu().isEmpty(), "una carpeta nueva inicia correctamente");
        }
        Path menu = carpeta.resolve("menu.txt");
        byte[] original = Files.readAllBytes(menu);
        Files.write(menu, Arrays.asList("FORMATO INVALIDO"));
        byte[] corrupto = Files.readAllBytes(menu);
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            throw new AssertionError(
                    "Se aceptó un menú corrupto con " + servicio.getMenu().size() + " platos");
        } catch (IOException ex) {
            comprobar(ex.getCause() != null && ex.getCause().getMessage() != null,
                    "el error de lectura conserva la causa para diagnosticarlo");
        }
        comprobar(Arrays.equals(corrupto, Files.readAllBytes(menu)),
                "un error de lectura no reemplaza el archivo corrupto por valores de ejemplo");
        Files.write(menu, original);
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            comprobar(!servicio.getMenu().isEmpty(),
                    "el error de apertura libera el bloqueo y permite reabrir tras reparar");
        }
    }

    private static void vistasPrevias() throws Exception {
        PanelMenu menu = new PanelMenu();
        comprobar(!((JButton) campo(menu, "btnNuevo")).isEnabled(),
                "la vista previa del menú deshabilita crear platos sin controlador");
        JTable platos = (JTable) campo(menu, "tablaPlatos");
        ((DefaultTableModel) platos.getModel())
                .addRow(new Object[] {"H001", "Vista previa", "Hamburguesas", 100, "Ejemplo"});
        platos.setRowSelectionInterval(0, 0);
        for (String nombre : Arrays.asList("btnEditar", "btnEliminar", "btnReceta")) {
            JButton boton = (JButton) campo(menu, nombre);
            comprobar(!boton.isEnabled(), "la vista previa del menú deshabilita " + nombre);
            boton.doClick();
        }
        platos.dispatchEvent(new java.awt.event.MouseEvent(
                platos, java.awt.event.MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 2, false));
        comprobar(platos.getRowCount() == 1,
                "un doble clic en la vista previa no abre el formulario de escritura");
        rechazar(IllegalStateException.class,
                ()
                        -> invocar(menu, "guardarMenu", new Class<?>[] {List.class},
                                CatalogoMenu.getPlatos()),
                "un guardado interno del menú sin controlador no devuelve éxito aparente");

        PanelInventario inventario = new PanelInventario();
        comprobar(!((JButton) campo(inventario, "btnAgregar")).isEnabled(),
                "la vista previa del inventario deshabilita agregar insumos");
        JTable tabla = (JTable) campo(inventario, "tabla");
        ((DefaultTableModel) tabla.getModel()).addRow(new Object[] {"Vista previa", "g", 30.0});
        tabla.setRowSelectionInterval(0, 0);
        for (String nombre : Arrays.asList("btnAjustar", "btnEliminar")) {
            JButton boton = (JButton) campo(inventario, nombre);
            comprobar(!boton.isEnabled(), "la vista previa del inventario deshabilita " + nombre);
            boton.doClick();
        }
        rechazar(IllegalStateException.class,
                ()
                        -> invocar(inventario, "persistir", new Class<?>[0]),
                "un guardado interno del inventario sin controlador falla explícitamente");
    }

    private static void vistasConectadas() throws Exception {
        Path carpeta = Files.createTempDirectory("restaurante-vistas-conectadas-");
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            Sesion sesion =
                    new Autenticacion(carpeta).autenticar(Rol.GERENTE, "gerente123".toCharArray());
            ControladorRestaurante controlador = new ControladorRestaurante(servicio, sesion);
            PanelMenu menu = new PanelMenu(controlador);
            comprobar(((JButton) campo(menu, "btnNuevo")).isEnabled(),
                    "el menú conectado permite crear platos");
            ((JTable) campo(menu, "tablaPlatos")).setRowSelectionInterval(0, 0);
            for (String nombre : Arrays.asList("btnEditar", "btnEliminar", "btnReceta")) {
                comprobar(((JButton) campo(menu, nombre)).isEnabled(),
                        "el menú conectado conserva habilitado " + nombre);
            }
            PanelInventario inventario = new PanelInventario(controlador);
            comprobar(((JButton) campo(inventario, "btnAgregar")).isEnabled(),
                    "el inventario conectado permite agregar insumos");
            ((JTable) campo(inventario, "tabla")).setRowSelectionInterval(0, 0);
            for (String nombre : Arrays.asList("btnAjustar", "btnEliminar")) {
                comprobar(((JButton) campo(inventario, nombre)).isEnabled(),
                        "el inventario conectado conserva habilitado " + nombre);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        copiasYValidacion();
        erroresDeLectura();
        Tema.instalar();
        SwingUtilities.invokeAndWait(() -> {
            try {
                vistasPrevias();
                vistasConectadas();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        System.out.println("TOTAL LIMPIEZA: " + pruebas + " comprobaciones.");
    }
}
