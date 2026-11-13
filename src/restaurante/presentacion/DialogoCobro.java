package restaurante.presentacion;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import restaurante.entidades.MetodoPago;

/** Confirmación de cobro: resumen del pedido, método de pago y cambio en efectivo. */
final class DialogoCobro {
    /** Resultado elegido por el empleado. {@code recibido} es nulo fuera del efectivo. */
    static final class Pago {
        final MetodoPago metodo;
        final Double recibido;

        Pago(MetodoPago metodo, Double recibido) {
            this.metodo = metodo;
            this.recibido = recibido;
        }
    }

    private DialogoCobro() {}

    /** Devuelve el pago confirmado o {@code null} si se cancela. */
    /** Contenido del diálogo y los controles que se consultan al confirmar. */
    static final class Vista {
        final JPanel panel;
        final JRadioButton[] opciones;
        final JTextField recibido;

        Vista(JPanel panel, JRadioButton[] opciones, JTextField recibido) {
            this.panel = panel;
            this.opciones = opciones;
            this.recibido = recibido;
        }
    }

    static Vista armar(JScrollPane resumen, double total) {
        resumen.setPreferredSize(new java.awt.Dimension(650, 260));
        JRadioButton[] opciones = new JRadioButton[MetodoPago.values().length];
        ButtonGroup grupo = new ButtonGroup();
        JPanel metodos = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        for (MetodoPago m : MetodoPago.values()) {
            JRadioButton opcion = new JRadioButton(m.toString(), m == MetodoPago.EFECTIVO);
            opciones[m.ordinal()] = opcion;
            grupo.add(opcion);
            metodos.add(opcion);
        }
        JTextField recibido = new JTextField(10);
        JLabel cambio = new JLabel();
        JPanel rapidos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        Runnable actualizar = () -> {
            boolean efectivo = opciones[MetodoPago.EFECTIVO.ordinal()].isSelected();
            recibido.setEnabled(efectivo);
            for (Component c : rapidos.getComponents()) c.setEnabled(efectivo);
            if (!efectivo) {
                cambio.setText("Se cobra " + Tema.moneda(total) + " sin cambio.");
                return;
            }
            Double valor = leer(recibido.getText());
            cambio.setText(valor == null || valor < total - 0.005
                            ? "Faltan " + Tema.moneda(total - (valor == null ? 0 : valor))
                            : "Cambio a devolver: " + Tema.moneda(valor - total));
        };
        JButton exacto = new JButton("Exacto");
        exacto.addActionListener(e -> recibido.setText(String.valueOf((long) Math.ceil(total))));
        rapidos.add(exacto);
        for (int billete : new int[] {20000, 50000, 100000}) {
            JButton boton = new JButton(Tema.moneda(billete));
            boton.addActionListener(e -> recibido.setText(String.valueOf(billete)));
            rapidos.add(boton);
        }
        recibido.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                actualizar.run();
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                actualizar.run();
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                actualizar.run();
            }
        });
        for (JRadioButton o : opciones) o.addActionListener(e -> actualizar.run());
        recibido.setText(String.valueOf((long) Math.ceil(total)));
        actualizar.run();

        JPanel pago = new JPanel(new GridLayout(0, 1, 0, 8));
        pago.add(new JLabel("Total a cobrar: " + Tema.moneda(total)));
        pago.add(metodos);
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila.add(new JLabel("Efectivo recibido:"));
        fila.add(recibido);
        pago.add(fila);
        pago.add(rapidos);
        pago.add(cambio);
        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.add(resumen, BorderLayout.CENTER);
        contenido.add(pago, BorderLayout.SOUTH);

        return new Vista(contenido, opciones, recibido);
    }

    static Pago pedir(Component padre, JScrollPane resumen, double total) {
        Vista vista = armar(resumen, total);
        JPanel contenido = vista.panel;
        JRadioButton[] opciones = vista.opciones;
        JTextField recibido = vista.recibido;
        while (true) {
            int respuesta = JOptionPane.showConfirmDialog(padre, contenido, "Confirmar cobro",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (respuesta != JOptionPane.OK_OPTION)
                return null;
            MetodoPago metodo = MetodoPago.EFECTIVO;
            for (MetodoPago m : MetodoPago.values())
                if (opciones[m.ordinal()].isSelected())
                    metodo = m;
            if (metodo != MetodoPago.EFECTIVO)
                return new Pago(metodo, null);
            Double valor = leer(recibido.getText());
            if (valor != null && valor >= total - 0.005)
                return new Pago(metodo, valor);
            JOptionPane.showMessageDialog(padre,
                    "El efectivo recibido no cubre el total del pedido.", "Efectivo insuficiente",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Admite "50.000", "50000" o "$ 50.000"; devuelve nulo si no hay un valor positivo. */
    static Double leer(String texto) {
        String digitos = texto == null ? "" : texto.replaceAll("[^0-9]", "");
        if (digitos.isEmpty() || digitos.length() > 12)
            return null;
        return Double.valueOf(digitos);
    }
}
