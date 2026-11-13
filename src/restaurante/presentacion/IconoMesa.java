package restaurante.presentacion;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Plano de mesa y sillas dibujado en Java según la capacidad configurada. */
public final class IconoMesa implements Icon {
    private final int sillas;
    private final boolean ocupada;

    public IconoMesa(int sillas, boolean ocupada) {
        this.sillas = sillas;
        this.ocupada = ocupada;
    }
    @Override
    public int getIconWidth() {
        return 96;
    }
    @Override
    public int getIconHeight() {
        return 96;
    }
    @Override
    public void paintIcon(Component componente, Graphics grafica, int x, int y) {
        Graphics2D g = (Graphics2D) grafica.create();
        try {
            g.translate(x + 48, y + 48);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < sillas; i++) {
                double angulo = 2 * Math.PI * i / sillas;
                Graphics2D silla = (Graphics2D) g.create();
                silla.rotate(angulo);
                silla.translate(0, -36);
                silla.setColor(ocupada ? Tema.ACENTO : Tema.TEXTO_SEC);
                silla.fill(new RoundRectangle2D.Double(-5, -5, 10, 10, 3, 3));
                silla.setColor(Tema.FONDO);
                silla.drawLine(-3, -2, 3, -2);
                silla.dispose();
            }
            g.setColor(ocupada ? new Color(0x70512C) : new Color(0x554C3E));
            g.fill(new RoundRectangle2D.Double(-23, -23, 46, 46, 13, 13));
            g.setColor(ocupada ? Tema.ACENTO : Tema.BORDE.brighter());
            g.setStroke(new BasicStroke(2));
            g.draw(new RoundRectangle2D.Double(-23, -23, 46, 46, 13, 13));
        } finally {
            g.dispose();
        }
    }
}
