package restaurante;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import restaurante.controlador.ControladorRestaurante;
import restaurante.datos.ArchivosTxt;
import restaurante.entidades.*;
import restaurante.seguridad.*;
import restaurante.servicios.ServicioRestaurante;

/** Verifica edición antes del cobro, grupos, sillas, migración y recuperación conjunta. */
public final class AtencionTest {
    private static int pruebas;
    private interface Accion {
        void ejecutar() throws Exception;
    }
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion)
            throw new AssertionError(mensaje);
        pruebas++;
        System.out.println("OK " + mensaje);
    }
    private static void rechazar(Accion accion, String mensaje) throws Exception {
        try {
            accion.ejecutar();
        } catch (IOException | SecurityException | IllegalArgumentException ex) {
            comprobar(true, mensaje);
            return;
        }
        throw new AssertionError(mensaje);
    }
    private static ControladorRestaurante entrar(ServicioRestaurante servicio, Rol rol)
            throws Exception {
        Sesion sesion =
                new Autenticacion(servicio.getDirectorio())
                        .autenticar(rol,
                                (rol == Rol.GERENTE ? "gerente123" : "empleado123").toCharArray());
        return new ControladorRestaurante(servicio, sesion);
    }
    private static List<DetallePedido> carrito(ControladorRestaurante c, int cantidad) {
        Plato plato =
                c.getMenu().stream().filter(p -> p.getCodigo().equals("H001")).findFirst().get();
        return Arrays.asList(new DetallePedido(plato, cantidad, plato.getPrecio()));
    }
    private static double panes(ControladorRestaurante c) {
        return c.getInventario()
                .stream()
                .filter(i -> i.getNombre().equals("Pan hamburguesa"))
                .findFirst()
                .get()
                .getCantidad();
    }
    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("la-doble-atencion-");
        List<String> antiguas = new ArrayList<>();
        for (int i = 1; i <= 10; i++) antiguas.add(i + "|false");
        ArchivosTxt.guardar(carpeta.resolve("mesas.txt"), antiguas);
        Map<String, List<String>> previos = new LinkedHashMap<>();
        int numero;
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            DatosPrueba.stockClasico(servicio);
            ControladorRestaurante c = entrar(servicio, Rol.EMPLEADO),
                                   gerente = entrar(servicio, Rol.GERENTE);
            comprobar(c.getDetalleMesas().get(0).getSillas() == 2
                            && c.getDetalleMesas().get(8).getSillas() == 8,
                    "archivos antiguos conservan mesas y reciben capacidades variadas");
            rechazar(() -> c.configurarSillas(1, 6), "solo el gerente configura sillas");
            rechazar(() -> gerente.configurarSillas(1, 0), "capacidad cero rechazada");
            rechazar(() -> gerente.configurarSillas(1, 21), "capacidad excesiva rechazada");
            gerente.configurarSillas(1, 6);
            comprobar(c.getDetalleMesas().get(0).getSillas() == 6,
                    "capacidad individual actualizada");
            rechazar(() -> c.unirMesas(Arrays.asList(1)), "no se une una sola mesa");
            rechazar(() -> c.unirMesas(Arrays.asList(1, 1)), "mesas repetidas rechazadas");
            rechazar(() -> c.unirMesas(Arrays.asList(1, 11)), "mesa inexistente rechazada");
            rechazar(() -> c.unirMesas(Arrays.asList(1, null)), "mesa nula rechazada");
            c.guardarMesa(3, true);
            rechazar(()
                             -> c.unirMesas(Arrays.asList(1, 3)),
                    "no se mezclan mesas de reuniones ya ocupadas");
            c.guardarMesa(3, false);
            c.unirMesas(Arrays.asList(1, 3));
            comprobar(c.getDetalleMesas().get(2).getGrupo() == 1, "mesas 1 y 3 comparten grupo");
            comprobar(c.getDetalleMesas()
                                    .stream()
                                    .filter(m -> m.getGrupo() == 1)
                                    .mapToInt(Mesa::getSillas)
                                    .sum()
                            == 10,
                    "capacidad del grupo suma las sillas de sus integrantes");
            rechazar(() -> c.unirMesas(Arrays.asList(1, 3)), "un grupo no se une consigo mismo");
            Pedido abierto = c.guardarPedidoAbierto(null, 3, carrito(c, 1));
            numero = abierto.getNumero();
            comprobar(abierto.getEstado() == EstadoPedido.PENDIENTE
                            && abierto.getMesas().equals(Arrays.asList(1, 3)),
                    "el pedido abierto conserva todas las mesas de la reunión");
            comprobar(c.getMesas()[0] && c.getMesas()[2],
                    "guardar ocupa todo el grupo en la misma operación");
            comprobar(c.getPedidos().isEmpty() && panes(c) == 20,
                    "guardar no crea ventas ni descuenta insumos");
            c.guardarPedidoAbierto(numero, 1, carrito(c, 2));
            comprobar(c.getPedidosAbiertos().size() == 1
                            && c.getPedidosAbiertos().get(0).calcularTotal() == 57800,
                    "editar reemplaza el pedido sin duplicarlo");
            comprobar(panes(c) == 20, "editar cantidades no descuenta insumos");
            Pedido copia = c.getPedidosAbiertos().get(0);
            copia.getDetalles().clear();
            comprobar(c.getPedidosAbiertos().get(0).getDetalles().size() == 1,
                    "la vista no altera el pedido persistido por referencia");
            rechazar(()
                             -> c.guardarPedidoAbierto(null, 3, carrito(c, 1)),
                    "cada grupo tiene un solo pedido abierto");
            rechazar(()
                             -> c.registrarPedido(1, carrito(c, 1)),
                    "cobro directo no ignora un pedido abierto");
            rechazar(() -> c.guardarMesa(1, false), "pedido abierto impide liberar la reunión");
            rechazar(() -> c.separarMesas(3), "grupo ocupado no se separa");
            rechazar(() -> gerente.configurarSillas(1, 4), "mesa ocupada no cambia capacidad");
            rechazar(()
                             -> c.guardarPedidoAbierto(numero, 2, carrito(c, 1)),
                    "un pedido guardado no se reasigna a otra mesa");
            rechazar(()
                             -> c.guardarPedidoAbierto(numero, 1, carrito(c, 0)),
                    "edición con cantidad inválida rechazada");
            comprobar(c.getPedidosAbiertos().get(0).calcularTotal() == 57800,
                    "edición inválida conserva el pedido anterior");
            c.guardarPedidoAbierto(numero, 1, carrito(c, 21));
            rechazar(()
                             -> c.cobrarPedidoAbierto(numero, carrito(c, 21)),
                    "cobro de pedido abierto sin existencias rechazado");
            comprobar(c.getPedidosAbiertos().get(0).getDetalles().get(0).getCantidad() == 21
                            && panes(c) == 20 && c.getPedidos().isEmpty(),
                    "stock insuficiente conserva el pedido editable sin registrar venta");
            c.guardarPedidoAbierto(numero, 1, carrito(c, 2));
            Path diario = carpeta.resolve("cobro_pendiente.txt");
            Files.createDirectory(diario);
            try {
                rechazar(()
                                 -> c.cobrarPedidoAbierto(numero, carrito(c, 2)),
                        "fallo de escritura no cobra un pedido abierto");
            } finally {
                Files.delete(diario);
            }
            comprobar(c.getPedidosAbiertos().size() == 1 && c.getPedidos().isEmpty()
                            && panes(c) == 20,
                    "fallo conserva el abierto, las ventas y el inventario");
            for (String archivo : Arrays.asList(
                         "pedidos.txt", "inventario.txt", "pedidos_abiertos.txt", "mesas.txt")) {
                previos.put(archivo, ArchivosTxt.leer(carpeta.resolve(archivo)));
            }
        }
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            ControladorRestaurante c = entrar(servicio, Rol.EMPLEADO);
            comprobar(c.getPedidoAbiertoPorMesa(3).getNumero() == numero
                            && c.getPedidosAbiertos().get(0).getDetalles().get(0).getCantidad()
                                    == 2,
                    "reiniciar recupera el pedido editado desde cualquiera de las mesas unidas");
            comprobar(c.getDetalleMesas().get(0).getSillas() == 6
                            && c.getDetalleMesas().get(2).getGrupo() == 1,
                    "capacidades y agrupación sobreviven al reinicio");
            c.cobrarPedidoAbierto(numero, carrito(c, 2));
            comprobar(c.getPedidosAbiertos().isEmpty() && c.getPedidos().size() == 1
                            && panes(c) == 18,
                    "cobro elimina el abierto y consume exactamente su última versión");
            rechazar(()
                             -> c.cobrarPedidoAbierto(numero, carrito(c, 2)),
                    "un pedido no se cobra dos veces");
        }
        // Simula una interrupción después de escribir, antes de retirar el diario.
        List<String> diario = new ArrayList<>();
        for (Map.Entry<String, List<String>> archivo : previos.entrySet()) {
            String tipo = archivo.getKey().equals("pedidos.txt")      ? "P"
                    : archivo.getKey().equals("inventario.txt")       ? "I"
                    : archivo.getKey().equals("pedidos_abiertos.txt") ? "A"
                                                                      : "M";
            diario.add(ArchivosTxt.fila("V", tipo));
            for (String linea : archivo.getValue()) diario.add(ArchivosTxt.fila(tipo, linea));
        }
        ArchivosTxt.guardar(carpeta.resolve("cobro_pendiente.txt"), diario);
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            ControladorRestaurante c = entrar(servicio, Rol.GERENTE);
            comprobar(c.getPedidos().isEmpty() && c.getPedidosAbiertos().size() == 1
                            && panes(c) == 20 && c.getMesas()[2],
                    "recuperación restaura juntos ventas, stock, pedido abierto y mesas");
            c.cobrarPedidoAbierto(numero, carrito(c, 2));
            c.guardarMesa(3, false);
            comprobar(
                    !c.getMesas()[0] && !c.getMesas()[2], "liberar una mesa libera todo el grupo");
            c.separarMesas(1);
            comprobar(c.getDetalleMesas().get(0).getGrupo() == 1
                            && c.getDetalleMesas().get(2).getGrupo() == 3,
                    "separar restaura mesas independientes sin cambiar las sillas");
            comprobar(c.getPedidos().get(0).getMesas().equals(Arrays.asList(1, 3)),
                    "separar no modifica el historial de la reunión");
            Pedido sinMesa = c.guardarPedidoAbierto(null, null, carrito(c, 1));
            c.cancelarPedidoAbierto(sinMesa.getNumero());
            comprobar(c.getPedidosAbiertos().isEmpty() && c.getPedidos().size() == 1
                            && panes(c) == 18,
                    "cancelar un abierto sin mesa no genera venta ni consumo");
            c.cerrarSesion();
            rechazar(() -> c.unirMesas(Arrays.asList(1, 2)), "sesión cerrada no une mesas");
            rechazar(()
                             -> c.guardarPedidoAbierto(null, null, Collections.emptyList()),
                    "sesión cerrada no crea pedidos");
        }
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            comprobar(servicio.getPedidosAbiertos().isEmpty()
                            && servicio.getPedidos().get(0).getMesas().equals(Arrays.asList(1, 3)),
                    "cancelación e historial de mesas permanecen tras cerrar");
        }
        Path mesas = carpeta.resolve("mesas.txt");
        byte[] validas = Files.readAllBytes(mesas);
        List<String> invalidas = ArchivosTxt.leer(mesas);
        invalidas.set(1, "1|false|0|1");
        ArchivosTxt.guardar(mesas, invalidas);
        byte[] corruptas = Files.readAllBytes(mesas);
        rechazar(()
                         -> new ServicioRestaurante(carpeta),
                "archivo con capacidad inválida impide abrir");
        comprobar(Arrays.equals(corruptas, Files.readAllBytes(mesas)),
                "no se sobrescribe el archivo de capacidades inválido");
        Files.write(mesas, validas);
        invalidas = ArchivosTxt.leer(mesas);
        invalidas.set(1, "1|false|6|2");
        invalidas.set(2, "2|false|2|1");
        ArchivosTxt.guardar(mesas, invalidas);
        corruptas = Files.readAllBytes(mesas);
        rechazar(()
                         -> new ServicioRestaurante(carpeta),
                "grupos circulares inválidos impiden abrir");
        comprobar(Arrays.equals(corruptas, Files.readAllBytes(mesas)),
                "no se sobrescribe el archivo de grupos inválido");
        Files.write(mesas, validas);
        System.out.println("TOTAL ATENCIÓN: " + pruebas + " comprobaciones.");
    }
}
