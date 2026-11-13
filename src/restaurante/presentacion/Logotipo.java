package restaurante.presentacion;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;

/** Emblema de La Doble dibujado en Java2D y escalable a los iconos de las ventanas. */
public final class Logotipo implements Icon {
    private final int tamano;

    public Logotipo(int tamano) {
        if (tamano < 1)
            throw new IllegalArgumentException("Tamaño de logotipo inválido.");
        this.tamano = tamano;
    }

    @Override
    public int getIconWidth() {
        return tamano;
    }
    @Override
    public int getIconHeight() {
        return tamano;
    }

    @Override
    public void paintIcon(Component componente, Graphics grafica, int x, int y) {
        Graphics2D g = (Graphics2D) grafica.create();
        try {
            g.translate(x, y);
            g.scale(tamano / 64.0, tamano / 64.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Tema.ACENTO);
            g.fill(new RoundRectangle2D.Double(1, 1, 62, 62, 18, 18));
            g.setColor(Tema.FONDO);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Ellipse2D.Double(16, 16, 32, 32));
            g.setColor(Tema.TEXTO);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Double(20, 20, 24, 24));
            g.setColor(Tema.FONDO);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(9, 16, 9, 48);
            g.drawLine(6, 16, 6, 26);
            g.drawLine(12, 16, 12, 26);
            g.drawLine(6, 26, 12, 26);
            g.fill(new Ellipse2D.Double(51, 16, 8, 15));
            g.drawLine(55, 29, 55, 48);
            g.setFont(new Font("Serif", Font.BOLD, 12));
            g.drawString("LD", 23, 36);
        } finally {
            g.dispose();
        }
    }

    public BufferedImage imagen() {
        BufferedImage imagen = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        paintIcon(null, g, 0, 0);
        g.dispose();
        return imagen;
    }

    public static List<Image> iconosVentana() {
        List<Image> imagenes = new ArrayList<>();
        for (int tamano : new int[] {16, 32, 48, 64, 128})
            imagenes.add(new Logotipo(tamano).imagen());
        return imagenes;
    }
}
