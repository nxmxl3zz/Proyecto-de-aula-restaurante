package restaurante.presentacion;

import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import restaurante.entidades.Plato;

/** Fotografías del catálogo, cargadas desde recursos PNG y reutilizadas por tamaño. */
public final class Imagenes {
    private static final Map<String, ImageIcon> CACHE = new HashMap<>();
    private Imagenes() {}
    public static Icon producto(Plato plato, int ancho, int alto) {
        return cargar(archivo(plato), ancho, alto);
    }
    public static ImageIcon cargar(String archivo, int ancho, int alto) {
        String clave = archivo + ":" + ancho + "x" + alto;
        if (!CACHE.containsKey(clave)) {
            URL ruta = Imagenes.class.getResource("/restaurante/imagenes/" + archivo + ".png");
            if (ruta == null) {
                throw new IllegalStateException("No se encontró la imagen: " + archivo);
            }
            CACHE.put(clave, new ImageIcon(new ImageIcon(ruta).getImage()
                    .getScaledInstance(ancho, alto, Image.SCALE_SMOOTH)));
        }
        return CACHE.get(clave);
    }
    public static String archivo(Plato plato) {
        String codigo = plato.getCodigo();
        if (codigo != null) {
            switch (codigo.toUpperCase(Locale.ROOT)) {
                case "S001": return "salchipapa-clasica";
                case "S002": return "salchipapa-especial";
                case "P001": return "perro-clasico";
                case "P002": return "perro-especial";
                case "Z001": return "pizza-margarita";
                case "Z002": return "pizza-pepperoni";
                case "Z003": return "pizza-hawaiana";
                case "D001": return "pollo-plancha";
                case "D002": return "carne-plancha";
                case "D003": return "ensalada-casa";
                default: break;
            }
        }
        String nombre = plato.getNombre().toLowerCase(Locale.ROOT);
        if (nombre.contains("limonada")) return "limonada";
        if (nombre.equals("agua")) return "agua";
        if (nombre.equals("tocineta")) return "tocineta";
        if (nombre.contains("queso extra")) return "queso";
        if ("Bebidas".equals(plato.getCategoria())) return "bebida";
        if (plato.esAdicional()) return "papas";
        if (nombre.contains("bbq")) return "bbq-bacon";
        if (nombre.contains("clásic")) return "clasica";
        return "la-doble";
    }
}
