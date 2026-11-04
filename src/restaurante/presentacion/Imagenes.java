package restaurante.presentacion;

import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import restaurante.entidades.Plato;

/** Sólo carga y escala recursos PNG; no dibuja fotografías ni iconos. */
public final class Imagenes {
    private static final Map<String, ImageIcon> CACHE = new HashMap<>();
    private Imagenes() {}
    public static ImageIcon cargar(String archivo, int ancho, int alto) {
        String clave = archivo + ancho + "x" + alto;
        if (!CACHE.containsKey(clave)) {
            URL ruta = Imagenes.class.getResource("/restaurante/imagenes/" + archivo + ".png");
            if (ruta == null) throw new IllegalStateException("No se encontró la imagen: " + archivo);
            CACHE.put(clave, new ImageIcon(new ImageIcon(ruta).getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH)));
        }
        return CACHE.get(clave);
    }
    public static String archivo(Plato plato) {
        String nombre=plato.getNombre().toLowerCase(java.util.Locale.ROOT);
        if (nombre.contains("limonada")) return "limonada";
        if (nombre.equals("agua")) return "agua";
        if (nombre.equals("tocineta")) return "tocineta";
        if (nombre.contains("queso extra")) return "queso";
        if ("Bebidas".equals(plato.getCategoria())) return "bebida";
        if ("Extras".equals(plato.getCategoria())) return "papas";
        if (plato.getNombre().toLowerCase(java.util.Locale.ROOT).contains("bbq")) return "bbq-bacon";
        if (plato.getNombre().toLowerCase(java.util.Locale.ROOT).contains("clásic")) return "clasica";
        return "la-doble";
    }
}
