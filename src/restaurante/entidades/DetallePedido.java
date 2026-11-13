package restaurante.entidades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Relaciona un plato con la cantidad y el precio de una línea del pedido. */
public class DetallePedido {
    private Plato plato;
    private int cantidad;
    private double precioUnitario;
    private String comensal = "";
    private String observaciones = "";
    private final List<AdicionalPedido> adicionales = new ArrayList<>();
    private final List<String> sinIngredientes = new ArrayList<>();

    public DetallePedido() {}

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
     * @return cantidad por el precio base y los adicionales de esta unidad
     */
    public double calcularSubtotal() {
        return cantidad
                * (precioUnitario
                        + adicionales.stream().mapToDouble(AdicionalPedido::calcularTotal).sum());
    }

    public String getComensal() {
        return comensal;
    }
    public void setComensal(String texto) {
        comensal = texto == null ? "" : texto.trim();
    }
    public String getObservaciones() {
        return observaciones;
    }
    public void setObservaciones(String texto) {
        observaciones = texto == null ? "" : texto.trim();
    }
    public List<AdicionalPedido> getAdicionales() {
        return Collections.unmodifiableList(new ArrayList<>(adicionales));
    }
    public void setAdicionales(List<AdicionalPedido> nuevos) {
        adicionales.clear();
        if (nuevos != null)
            adicionales.addAll(nuevos);
    }
    public List<String> getSinIngredientes() {
        return Collections.unmodifiableList(new ArrayList<>(sinIngredientes));
    }
    public void setSinIngredientes(List<String> nombres) {
        sinIngredientes.clear();
        if (nombres != null)
            sinIngredientes.addAll(nombres);
    }
    public boolean estaPersonalizado() {
        return !adicionales.isEmpty() || !sinIngredientes.isEmpty() || !comensal.isEmpty()
                || !observaciones.isEmpty();
    }
    public DetallePedido copia() {
        DetallePedido copia =
                new DetallePedido(plato == null ? null : plato.copia(), cantidad, precioUnitario);
        copia.setComensal(comensal);
        copia.setObservaciones(observaciones);
        copia.setAdicionales(adicionales);
        copia.setSinIngredientes(sinIngredientes);
        return copia;
    }

    @Override
    public String toString() {
        return "Plato: " + (plato == null ? "Sin plato" : plato.getNombre()) + ", Cantidad: "
                + cantidad + ", Precio unitario: " + precioUnitario + ", Comensal: " + comensal
                + ", Sin: " + sinIngredientes + ", Nota: " + observaciones + ", Adicionales: "
                + adicionales.stream()
                          .map(a -> a.getPlato().getNombre() + " x" + a.getCantidad())
                          .collect(Collectors.joining(", "))
                + ", Subtotal: " + calcularSubtotal();
    }
}
