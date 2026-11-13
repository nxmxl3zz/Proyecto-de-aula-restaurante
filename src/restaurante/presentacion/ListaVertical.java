package restaurante.presentacion;

import java.awt.*;
import javax.swing.*;

/** Lista de tarjetas que ocupa el ancho disponible y se desplaza verticalmente. */
public final class ListaVertical extends JPanel implements Scrollable {
    public ListaVertical() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
    }
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }
    public int getScrollableUnitIncrement(Rectangle area, int orientacion, int direccion) {
        return 24;
    }
    public int getScrollableBlockIncrement(Rectangle area, int orientacion, int direccion) {
        return Math.max(24, area.height - 24);
    }
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
