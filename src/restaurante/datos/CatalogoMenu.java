package restaurante.datos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import restaurante.entidades.Plato;

/** Catálogo en memoria compartido por las pantallas Menú y Pedidos. */
public final class CatalogoMenu {
    private static final List<String> CATEGORIAS = Collections.unmodifiableList(Arrays.asList(
            "Hamburguesas", "Combos", "Bebidas", "Extras"));
    private static final List<Plato> PLATOS = new ArrayList<>();

    static {
        PLATOS.add(new Plato("H001", "La Doble", "Especialidad de la casa: doble carne, queso, cebolla caramelizada y salsa de la casa.", 28900, "Hamburguesas"));
        PLATOS.add(new Plato("H002", "Clásica", "Carne a la plancha, queso, lechuga, tomate y salsa de la casa.", 18000, "Hamburguesas"));
        PLATOS.add(new Plato("H003", "BBQ Bacon", "Carne, queso, tocineta crocante y salsa BBQ.", 26000, "Hamburguesas"));

        PLATOS.add(new Plato("C001", "Combo La Doble", "La Doble con papas y gaseosa.", 36900, "Combos"));
        PLATOS.add(new Plato("C002", "Combo Clásico", "Hamburguesa clásica con papas y gaseosa.", 27000, "Combos"));
        PLATOS.add(new Plato("C003", "Combo BBQ", "Hamburguesa BBQ Bacon con papas y gaseosa.", 35000, "Combos"));

        PLATOS.add(new Plato("B001", "Gaseosa", "Gaseosa personal.", 4000, "Bebidas"));
        PLATOS.add(new Plato("B002", "Limonada", "Limonada natural.", 6000, "Bebidas"));
        PLATOS.add(new Plato("B003", "Agua", "Agua embotellada.", 3000, "Bebidas"));

        PLATOS.add(new Plato("E001", "Papas", "Porción de papas fritas.", 7000, "Extras"));
        PLATOS.add(new Plato("E002", "Tocineta", "Porción adicional de tocineta.", 5000, "Extras"));
        PLATOS.add(new Plato("E003", "Queso extra", "Porción adicional de queso.", 4000, "Extras"));
    }

    private CatalogoMenu() {
    }

    /** Lista compartida para que las altas, cambios y bajas se reflejen en ambas pantallas. */
    public static List<Plato> getPlatos() {
        return PLATOS;
    }

    public static List<String> getCategorias() {
        return CATEGORIAS;
    }

    public static List<Plato> porCategoria(String categoria) {
        List<Plato> resultado = new ArrayList<>();
        for (Plato plato : PLATOS) {
            if (categoria.equals(plato.getCategoria())) resultado.add(plato);
        }
        return resultado;
    }
}
