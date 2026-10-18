package restaurante.entidades;

/** Relaciona un plato con la cantidad y el precio de una línea del pedido. */
public class DetallePedido {
    private Plato plato;
    private int cantidad;
    private double precioUnitario;

    public DetallePedido() {
    }

    public DetallePedido(Plato plato, int cantidad, double precioUnitario) {
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

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    /**
     * Calcula el valor de esta línea del pedido.
     *
     * @return la cantidad multiplicada por el precio unitario
     */
    public double calcularSubtotal() {
        return cantidad * precioUnitario;
    }

    @Override
    public String toString() {
        return "Plato: " + (plato == null ? "Sin plato" : plato.getNombre())
                + ", Cantidad: " + cantidad + ", Precio unitario: " + precioUnitario
                + ", Subtotal: " + calcularSubtotal();
    }
}
