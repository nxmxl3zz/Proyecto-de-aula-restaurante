package restaurante.entidades;

import java.math.BigDecimal;

/** Relaciona un plato con la cantidad y el precio de una línea del pedido. */
public class DetallePedido {
    private Plato plato;
    private int cantidad;
    private BigDecimal precioUnitario;

    public DetallePedido() {
    }

    public DetallePedido(Plato plato, int cantidad, BigDecimal precioUnitario) {
        this.plato = plato;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Plato getPlato() {
        return plato;
    }

    public void setPlato(Plato plato) {
        this.plato = plato;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}

