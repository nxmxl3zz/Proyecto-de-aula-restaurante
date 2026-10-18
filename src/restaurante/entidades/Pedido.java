package restaurante.entidades;

import java.time.LocalDate;
import java.util.ArrayList;

/** Pedido de un cliente atendido por un mesero. */
public class Pedido {
    private int numero;
    private LocalDate fecha;
    private Cliente cliente;
    private Mesero mesero;
    private EstadoPedido estado;
    private ArrayList<DetallePedido> detalles;

    public Pedido() {
        detalles = new ArrayList<>();
    }

    public Pedido(int numero, LocalDate fecha, Cliente cliente, Mesero mesero, EstadoPedido estado,
            ArrayList<DetallePedido> detalles) {
        this.numero = numero;
        this.fecha = fecha;
        this.cliente = cliente;
        this.mesero = mesero;
        this.estado = estado;
        if (detalles == null) {
            this.detalles = new ArrayList<>();
        } else {
            this.detalles = detalles;
        }
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Mesero getMesero() {
        return mesero;
    }

    public void setMesero(Mesero mesero) {
        this.mesero = mesero;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public ArrayList<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(ArrayList<DetallePedido> detalles) {
        if (detalles == null) {
            this.detalles = new ArrayList<>();
        } else {
            this.detalles = detalles;
        }
    }

    /**
     * Agrega una línea al pedido.
     *
     * @param detalle la línea con el plato, la cantidad y el precio
     */
    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
    }

    /**
     * Suma los subtotales de todas las líneas del pedido.
     *
     * @return el total a pagar por el pedido
     */
    public double calcularTotal() {
        double total = 0;
        for (DetallePedido detalle : detalles) {
            total += detalle.calcularSubtotal();
        }
        return total;
    }

    @Override
    public String toString() {
        return "Número: " + numero + ", Fecha: " + fecha
                + ", Cliente: " + (cliente == null ? "Sin asignar" : cliente.getNombre())
                + ", Mesero: " + (mesero == null ? "Sin asignar" : mesero.getNombre())
                + ", Estado: " + estado + ", Total: " + calcularTotal();
    }
}
