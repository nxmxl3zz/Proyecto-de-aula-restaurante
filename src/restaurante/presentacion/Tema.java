package restaurante.presentacion;

import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.*;

/** Colores y estilos compartidos; las fotografías se cargan desde archivos PNG. */
public final class Tema {
    public static final Color FONDO = new Color(0x211F1C);
    public static final Color LATERAL = new Color(0x1C1A17);
    public static final Color TARJETA = new Color(0x2D2A25);
    public static final Color BORDE = new Color(0x514A3F);
    public static final Color ACENTO = new Color(0xE9AF52);
    public static final Color TEXTO = new Color(0xF5EDDD);
    public static final Color TEXTO_SEC = new Color(0xBDB4A5);
    private Tema() {}
    public static String moneda(double valor) {
        NumberFormat formato=NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        formato.setMinimumFractionDigits(0);formato.setMaximumFractionDigits(2);return formato.format(valor);
    }
    public static void preparar(JPanel panel) {
        panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
    }
    public static void tarjeta(JPanel panel) {
        panel.setBackground(TARJETA);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDE), BorderFactory.createEmptyBorder(16,16,16,16)));
    }
    public static void principal(JButton boton) {
        boton.setBackground(ACENTO); boton.setForeground(FONDO);
        boton.setFont(new Font("SansSerif", Font.BOLD, 14));
        boton.setFocusPainted(false);
        boton.setMargin(new Insets(12,18,12,18));
    }
    public static void tabla(JTable tabla) {
        tabla.setRowHeight(42); tabla.setBackground(TARJETA); tabla.setForeground(TEXTO);
        tabla.setSelectionBackground(new Color(0x55432B)); tabla.setSelectionForeground(TEXTO);
        tabla.setGridColor(BORDE); tabla.setShowVerticalLines(false);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,13));
    }
}
