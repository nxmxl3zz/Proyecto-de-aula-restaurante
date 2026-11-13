package restaurante.presentacion;

import java.util.List;
import restaurante.entidades.*;

/** Comanda explícita: cada preparación con sus propios adicionales e indicaciones. */
public final class ResumenPedido {
    private ResumenPedido() {}
    public static String texto(String mesas, List<DetallePedido> detalles) {
        StringBuilder texto = new StringBuilder(mesas + "\n\n");
        int numero = 1;
        double total = 0;
        for (DetallePedido d : detalles) {
            texto.append("UNIDAD ").append(numero++).append(" · ").append(d.getPlato().getNombre());
            if (d.getCantidad() != 1)
                texto.append(" x").append(d.getCantidad());
            texto.append(" — ").append(Tema.moneda(d.calcularSubtotal())).append('\n');
            if (!d.getComensal().isEmpty())
                texto.append("  Comensal / silla: ").append(d.getComensal()).append('\n');
            for (AdicionalPedido a : d.getAdicionales())
                texto.append("  + ")
                        .append(a.getPlato().getNombre())
                        .append(" x")
                        .append(a.getCantidad())
                        .append(" (por esta unidad)\n");
            for (String insumo : d.getSinIngredientes())
                texto.append("  SIN ").append(insumo).append('\n');
            if (!d.getObservaciones().isEmpty())
                texto.append("  Indicación: ").append(d.getObservaciones()).append('\n');
            texto.append('\n');
            total += d.calcularSubtotal();
        }
        return texto.append("TOTAL: ").append(Tema.moneda(total)).toString();
    }
}
