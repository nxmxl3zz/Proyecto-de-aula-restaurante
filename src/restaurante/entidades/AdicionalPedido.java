package restaurante.entidades;

/** Adicional de una unidad concreta de comida, con el precio aplicado al pedido. */
public final class AdicionalPedido {
    private final Plato plato;
    private final int cantidad;
    private final double precioUnitario;

    public AdicionalPedido(Plato plato, int cantidad, double precioUnitario) {
        if (plato == null || cantidad < 1 || cantidad > 5 || !Double.isFinite(precioUnitario)
                || precioUnitario <= 0) {
            throw new IllegalArgumentException(
                    "Adicional inválido; selecciona entre 1 y 5 porciones.");
        }
        this.plato = plato.copia();
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Plato getPlato() {
        return plato.copia();
    }
    public int getCantidad() {
        return cantidad;
    }
    public double getPrecioUnitario() {
        return precioUnitario;
    }
    public double calcularTotal() {
        return cantidad * precioUnitario;
    }
}
