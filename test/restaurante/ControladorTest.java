package restaurante;

import java.nio.file.*;
import java.util.*;
import restaurante.controlador.ControladorRestaurante;
import restaurante.entidades.*;
import restaurante.seguridad.*;
import restaurante.servicios.*;

/** Casos negativos del controlador y reglas verificadas sin depender de Swing. */
public final class ControladorTest {
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
    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("la-doble-controlador-");
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            Autenticacion acceso = new Autenticacion(carpeta);
            Sesion empleado = acceso.autenticar(Rol.EMPLEADO, "empleado123".toCharArray());
            Sesion gerente = acceso.autenticar(Rol.GERENTE, "gerente123".toCharArray());
            ControladorRestaurante atencion = new ControladorRestaurante(servicio, empleado);
            ControladorRestaurante administracion = new ControladorRestaurante(servicio, gerente);
            rechazar(SecurityException.class,
                    () -> new ControladorRestaurante(servicio, null), "controlador exige sesión");
            rechazar(SecurityException.class, atencion::getReporteVentas,
                    "empleado no consulta reportes aunque invoque el controlador");
            rechazar(SecurityException.class,
                    ()
                            -> atencion.guardarMenu(atencion.getMenu()),
                    "controlador impide modificar menú al empleado");
            rechazar(SecurityException.class,
                    ()
                            -> atencion.guardarInventario(atencion.getInventario()),
                    "controlador impide modificar inventario al empleado");
            rechazar(ValidacionException.class,
                    () -> administracion.guardarMenu(null), "menú nulo genera error de validación");
            rechazar(ValidacionException.class,
                    ()
                            -> administracion.guardarInventario(Arrays.asList((Insumo) null)),
                    "insumo nulo genera error de validación");
            rechazar(ValidacionException.class,
                    ()
                            -> atencion.registrarPedido(1, null),
                    "carrito nulo genera error de validación");
            rechazar(ValidacionException.class,
                    ()
                            -> atencion.registrarPedido(1, Arrays.asList((DetallePedido) null)),
                    "detalle nulo genera error de validación");
            Plato plato = atencion.getMenu().get(0);
            rechazar(ValidacionException.class,
                    ()
                            -> atencion.registrarPedido(1,
                                    Arrays.asList(new DetallePedido(plato, 0, plato.getPrecio()))),
                    "cantidad cero rechazada");
            rechazar(ValidacionException.class,
                    ()
                            -> atencion.registrarPedido(
                                    1, Arrays.asList(new DetallePedido(plato, 1, Double.NaN))),
                    "precio NaN rechazado");
            Plato falso = new Plato("NO_EXISTE", "Inválido", "Prueba", 100, "Extras");
            rechazar(ValidacionException.class,
                    ()
                            -> atencion.registrarPedido(
                                    1, Arrays.asList(new DetallePedido(falso, 1, 100))),
                    "plato ajeno al menú rechazado");
            List<Plato> menu = administracion.getMenu();
            menu.add(new Plato(" h001 ", "Duplicado", "Prueba", 100, "Extras"));
            rechazar(ValidacionException.class,
                    ()
                            -> administracion.guardarMenu(menu),
                    "código duplicado con espacios rechazado");
            comprobar(administracion.getReporteVentas().isEmpty(),
                    "operaciones inválidas no crean ventas");
            Pedido cobrado = atencion.registrarPedido(
                    null, Arrays.asList(new DetallePedido(plato, 1, plato.getPrecio())));
            comprobar(cobrado.getEstado() == EstadoPedido.COBRADO,
                    "pedido confirmado tiene estado cobrado");
            List<Plato> copia = atencion.getMenu();
            copia.clear();
            comprobar(!atencion.getMenu().isEmpty(),
                    "vista no modifica el menú por una referencia compartida");
            atencion.cerrarSesion();
            rechazar(
                    SecurityException.class, atencion::getMenu, "sesión cerrada no consulta datos");
            rechazar(SecurityException.class,
                    ()
                            -> atencion.registrarPedido(1,
                                    Arrays.asList(new DetallePedido(plato, 1, plato.getPrecio()))),
                    "sesión cerrada no cobra");
        }
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            comprobar(servicio.getPedidos().get(0).getEstado() == EstadoPedido.COBRADO,
                    "estado cobrado se recupera después del reinicio");
        }
        // Impide que futuras pantallas vuelvan a saltarse el controlador.
        try (java.util.stream.Stream<Path> archivos =
                        Files.walk(Paths.get("src/restaurante/presentacion"))) {
            for (Path archivo : (Iterable<Path>) archivos.filter(
                         p -> p.toString().endsWith(".java"))::iterator) {
                String texto = new String(
                        Files.readAllBytes(archivo), java.nio.charset.StandardCharsets.UTF_8);

                comprobar(!texto.contains("restaurante.datos.")
                                && !texto.contains("restaurante.servicios."),
                        "vista depende del controlador: " + archivo.getFileName());
            }
        }
        System.out.println("TOTAL CONTROLADOR: " + pruebas + " comprobaciones.");
    }
}
