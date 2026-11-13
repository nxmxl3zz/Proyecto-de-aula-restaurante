package restaurante;

import java.io.IOException;
import java.util.*;
import restaurante.entidades.Insumo;
import restaurante.seguridad.*;
import restaurante.servicios.ServicioRestaurante;

/**
 * Existencias controladas para conservar los escenarios de agotamiento de las pruebas anteriores.
 */
final class DatosPrueba {
    private DatosPrueba() {}
    static void stockClasico(ServicioRestaurante servicio) throws IOException {
        String[] nombres = {"Pan hamburguesa", "Medallones de carne", "Queso cheddar", "Tocineta",
                "Cebolla caramelizada", "Lechuga", "Tomate", "Salsa de la casa", "Salsa BBQ",
                "Papas", "Aceite", "Gaseosa", "Limonada", "Agua"};
        double[] cantidades = {20, 40, 40, 600, 400, 250, 500, 600, 400, 4000, 1500, 24, 3600, 12};
        Map<String, Double> limites = new HashMap<>();
        for (int i = 0; i < nombres.length; i++) limites.put(nombres[i], cantidades[i]);
        List<Insumo> stock = new ArrayList<>();
        for (Insumo i : servicio.getInventario())
            stock.add(new Insumo(i.getNombre(), i.getUnidad(),
                    limites.getOrDefault(i.getNombre(), i.getCantidad())));
        Sesion gerente = new Autenticacion(servicio.getDirectorio())
                                 .autenticar(Rol.GERENTE, "gerente123".toCharArray());
        servicio.guardarInventario(gerente, stock);
    }
}
