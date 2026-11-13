package restaurante.controlador;

import java.io.IOException;
import java.util.List;
import restaurante.entidades.*;
import restaurante.seguridad.Sesion;
import restaurante.servicios.ServicioRestaurante;

/** Adapta las acciones de la vista a los casos de uso usando la sesión autenticada. */
public final class ControladorRestaurante {
    private final ServicioRestaurante servicio;
    private final Sesion sesion;

    public ControladorRestaurante(ServicioRestaurante servicio, Sesion sesion) {
        ServicioRestaurante.autenticar(sesion);
        this.servicio = java.util.Objects.requireNonNull(servicio);
        this.sesion = sesion;
    }

    public boolean esGerente() {
        return sesion.estaActiva() && sesion.esGerente();
    }
    public String getNombreRol() {
        return sesion.getRol().toString();
    }
    public void cerrarSesion() {
        sesion.cerrar();
    }
    public List<Plato> getMenu() {
        exigirSesion();
        return servicio.getMenu();
    }
    public DetallePedido validarUnidad(DetallePedido detalle) throws IOException {
        exigirSesion();
        return servicio.validarUnidad(detalle);
    }
    public void comprobarExistencias(List<DetallePedido> carrito) throws IOException {
        exigirSesion();
        servicio.comprobarExistencias(carrito);
    }
    public List<Pedido> getPedidos() {
        exigirSesion();
        return servicio.getPedidos();
    }
    public List<Pedido> getReporteVentas() {
        return servicio.consultarReporte(sesion);
    }
    public List<Insumo> getInventario() {
        exigirSesion();
        return servicio.getInventario();
    }
    public boolean[] getMesas() {
        exigirSesion();
        return servicio.getMesas();
    }
    public List<Mesa> getDetalleMesas() {
        exigirSesion();
        return servicio.getDetalleMesas();
    }
    public List<Pedido> getPedidosAbiertos() {
        exigirSesion();
        return servicio.getPedidosAbiertos();
    }
    public Pedido getPedidoAbiertoPorMesa(int numero) {
        exigirSesion();
        return servicio.getPedidoAbiertoPorMesa(numero);
    }
    public Pedido guardarPedidoAbierto(Integer numero, Integer mesa, List<DetallePedido> carrito)
            throws IOException {
        return servicio.guardarPedidoAbierto(sesion, numero, mesa, carrito);
    }
    public Pedido cobrarPedidoAbierto(int numero, List<DetallePedido> carrito) throws IOException {
        return servicio.cobrarPedidoAbierto(sesion, numero, carrito);
    }
    public Pedido cobrarPedidoAbierto(
            int numero, List<DetallePedido> carrito, MetodoPago metodo, Double recibido)
            throws IOException {
        return servicio.cobrarPedidoAbierto(sesion, numero, carrito, metodo, recibido);
    }
    public Pedido avanzarEstadoPedidoAbierto(int numero) throws IOException {
        return servicio.avanzarEstadoPedidoAbierto(sesion, numero);
    }
    public void cancelarPedidoAbierto(int numero) throws IOException {
        servicio.cancelarPedidoAbierto(sesion, numero);
    }
    public void configurarSillas(int numero, int cantidad) throws IOException {
        servicio.configurarSillas(sesion, numero, cantidad);
    }
    public void unirMesas(List<Integer> numeros) throws IOException {
        servicio.unirMesas(sesion, numeros);
    }
    public void separarMesas(int numero) throws IOException {
        servicio.separarMesas(sesion, numero);
    }
    public List<IngredienteReceta> getReceta(String codigo) {
        exigirSesion();
        return servicio.getReceta(codigo);
    }
    public static List<String> getCategorias() {
        return ServicioRestaurante.getCategorias();
    }

    public Pedido registrarPedido(Integer mesa, List<DetallePedido> carrito) throws IOException {
        return servicio.registrarPedido(sesion, mesa, carrito);
    }
    public Pedido registrarPedido(Integer mesa, List<DetallePedido> carrito, MetodoPago metodo,
            Double recibido) throws IOException {
        return servicio.registrarPedido(sesion, mesa, carrito, metodo, recibido);
    }
    public void guardarMenu(List<Plato> platos) throws IOException {
        servicio.guardarMenu(sesion, platos);
    }
    public void guardarInventario(List<Insumo> insumos) throws IOException {
        servicio.guardarInventario(sesion, insumos);
    }
    public void guardarReceta(String codigo, List<IngredienteReceta> ingredientes)
            throws IOException {
        servicio.guardarReceta(sesion, codigo, ingredientes);
    }
    public void guardarMesa(int numero, boolean ocupada) throws IOException {
        servicio.guardarMesa(sesion, numero, ocupada);
    }
    private void exigirSesion() {
        ServicioRestaurante.autenticar(sesion);
    }
}
