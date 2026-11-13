package restaurante.presentacion;

import java.awt.*;
import javax.swing.*;
import restaurante.entidades.AdicionalPedido;
import restaurante.entidades.DetallePedido;

/** Una unidad y sus modificaciones, con acciones accesibles desde el mismo bloque. */
public final class TarjetaUnidad extends JPanel {
    private final JButton btnPersonalizar = new JButton("Personalizar");
    private final JButton btnDuplicar = new JButton("Duplicar");
    private final JButton btnQuitar = new JButton("Quitar");

    public TarjetaUnidad(int numero, DetallePedido detalle, Runnable editar, Runnable duplicar,
            Runnable quitar) {
        super(new BorderLayout(0, 10));
        Tema.tarjeta(this);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 8, 0, Tema.TARJETA), getBorder()));
        StringBuilder texto = new StringBuilder("<html><div style='width:225px'>");
        texto.append("<font color='#BDB4A5'>UNIDAD ")
                .append(numero)
                .append("</font><br><b>")
                .append(Tema.escapar(detalle.getPlato().getNombre()))
                .append("</b><br>")
                .append(detalle.getComensal().isEmpty() ? "Comensal sin indicar"
                                                        : Tema.escapar(detalle.getComensal()));
        for (AdicionalPedido adicional : detalle.getAdicionales())
            texto.append("<br>+ ")
                    .append(adicional.getCantidad())
                    .append(" × ")
                    .append(Tema.escapar(adicional.getPlato().getNombre()))
                    .append(" · ")
                    .append(Tema.moneda(adicional.calcularTotal()));
        for (String ingrediente : detalle.getSinIngredientes())
            texto.append("<br><font color='#E9AF52'><b>SIN ")
                    .append(Tema.escapar(ingrediente))
                    .append("</b></font>");
        if (!detalle.getObservaciones().isEmpty())
            texto.append("<br>Nota: ")
                    .append(Tema.escapar(detalle.getObservaciones()).replace("\n", "<br>"));
        texto.append("<br><b>")
                .append(Tema.moneda(detalle.calcularSubtotal()))
                .append("</b></div></html>");
        JLabel contenido = new JLabel(texto.toString());
        contenido.setFont(new Font("SansSerif", Font.PLAIN, 14));
        add(contenido, BorderLayout.CENTER);
        JPanel acciones = new JPanel(new GridLayout(1, 3, 6, 0));
        acciones.setOpaque(false);
        JButton[] botones = {btnPersonalizar, btnDuplicar, btnQuitar};
        Runnable[] tareas = {editar, duplicar, quitar};
        for (int i = 0; i < botones.length; i++) {
            JButton boton = botones[i];
            Runnable tarea = tareas[i];
            boton.setMargin(new Insets(7, 3, 7, 3));
            boton.setFont(new Font("SansSerif", Font.PLAIN, 13));
            boton.getAccessibleContext().setAccessibleName(boton.getText() + " unidad " + numero);
            boton.addActionListener(e -> tarea.run());
            acciones.add(boton);
        }
        btnQuitar.setForeground(new Color(0xED998B));
        Tema.atajo(this, "control E", editar);
        Tema.atajo(this, "control D", duplicar);
        Tema.atajo(this, "DELETE", quitar);
        btnPersonalizar.setToolTipText("Personalizar esta unidad · Ctrl+E");
        btnDuplicar.setToolTipText("Duplicar esta unidad y sus cambios · Ctrl+D");
        btnQuitar.setToolTipText("Quitar esta unidad · Supr");
        add(acciones, BorderLayout.SOUTH);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
    public void enfocar() {
        btnPersonalizar.requestFocusInWindow();
    }
}
