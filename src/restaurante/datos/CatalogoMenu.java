package restaurante.datos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import restaurante.entidades.Plato;

/** Semilla del menú para una carpeta de datos nueva. No contiene el estado de las vistas. */
public final class CatalogoMenu {
    private static final List<String> CATEGORIAS =
            Collections.unmodifiableList(Arrays.asList("Hamburguesas", "Combos", "Salchipapas",
                    "Perros calientes", "Pizzas", "Platos", "Bebidas", "Adicionales", "Extras"));
    private static final List<Plato> PLATOS = new ArrayList<>();

    static {
        PLATOS.add(new Plato("H001", "La Doble",
                "Especialidad de la casa: doble carne, queso, cebolla caramelizada y salsa de la "
                        + "casa.",
                28900, "Hamburguesas"));
        PLATOS.add(new Plato("H002", "Clásica",
                "Carne a la plancha, queso, lechuga, tomate y salsa de la casa.", 18000,
                "Hamburguesas"));
        PLATOS.add(new Plato("H003", "BBQ Bacon", "Carne, queso, tocineta crocante y salsa BBQ.",
                26000, "Hamburguesas"));

        PLATOS.add(new Plato(
                "C001", "Combo La Doble", "La Doble con papas y gaseosa.", 35900, "Combos"));
        PLATOS.add(new Plato("C002", "Combo Clásico", "Hamburguesa clásica con papas y gaseosa.",
                26000, "Combos"));
        PLATOS.add(new Plato("C003", "Combo BBQ", "Hamburguesa BBQ Bacon con papas y gaseosa.",
                33000, "Combos"));

        PLATOS.add(new Plato("B001", "Gaseosa", "Gaseosa personal.", 4000, "Bebidas"));
        PLATOS.add(new Plato("B002", "Limonada", "Limonada natural.", 6000, "Bebidas"));
        PLATOS.add(new Plato("B003", "Agua", "Agua embotellada.", 3000, "Bebidas"));

        PLATOS.add(new Plato("S001", "Salchipapa clásica",
                "Papas fritas, salchicha y salsa de la casa.", 17000, "Salchipapas"));
        PLATOS.add(new Plato("S002", "Salchipapa especial",
                "Papas, salchicha, mozzarella, tocineta y salsa de la casa.", 24000,
                "Salchipapas"));
        PLATOS.add(new Plato("P001", "Perro caliente clásico",
                "Pan suave, salchicha, papas fosforito y salsa de la casa.", 14000,
                "Perros calientes"));
        PLATOS.add(new Plato("P002", "Perro caliente especial",
                "Salchicha, queso cheddar, tocineta, papas fosforito y salsa.", 20000,
                "Perros calientes"));
        PLATOS.add(new Plato("Z001", "Pizza margarita personal",
                "Base artesanal, salsa de tomate, mozzarella, tomate fresco y albahaca.", 22000,
                "Pizzas"));
        PLATOS.add(new Plato("Z002", "Pizza pepperoni personal",
                "Base artesanal, salsa de tomate, mozzarella y pepperoni.", 26000, "Pizzas"));
        PLATOS.add(new Plato("Z003", "Pizza hawaiana personal",
                "Base artesanal, salsa de tomate, mozzarella, jamón y piña.", 25000, "Pizzas"));
        PLATOS.add(new Plato("D001", "Pollo a la plancha",
                "Pechuga a la plancha con papas, lechuga y tomate.", 24000, "Platos"));
        PLATOS.add(new Plato("D002", "Carne a la plancha",
                "Carne de res a la plancha con papas, lechuga y tomate.", 28000, "Platos"));
        PLATOS.add(new Plato("D003", "Ensalada de la casa",
                "Lechuga, tomate, maíz y aderezo de aceite.", 14000, "Platos"));
        PLATOS.add(new Plato("E001", "Papas", "Porción de papas fritas para el plato seleccionado.",
                7000, "Adicionales"));
        PLATOS.add(new Plato(
                "E002", "Tocineta", "Porción adicional de tocineta.", 5000, "Adicionales"));
        PLATOS.add(new Plato(
                "E003", "Queso extra", "Porción adicional de queso.", 4000, "Adicionales"));
    }

    private CatalogoMenu() {}

    /** Copias de los platos iniciales; cada restaurante conserva su propio menú. */
    public static List<Plato> getPlatos() {
        List<Plato> copia = new ArrayList<>();
        for (Plato p : PLATOS)
            copia.add(p.copia());
        return copia;
    }

    public static List<String> getCategorias() {
        return CATEGORIAS;
    }

    public static List<Plato> porCategoria(String categoria) {
        List<Plato> resultado = new ArrayList<>();
        for (Plato plato : PLATOS) {
            if (categoria.equals(plato.getCategoria()))
                resultado.add(plato);
        }
        return resultado;
    }
}
