package restaurante.presentacion;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import restaurante.controlador.ControladorRestaurante;
import restaurante.entidades.*;

/** Formulario de una unidad: sus adicionales, ingredientes omitidos y comensal. */
public final class PersonalizarPlato extends JPanel {
    private final ControladorRestaurante controlador;
    private final DetallePedido original;
    private final JTextField comensal = new JTextField();
    private final JTextArea indicaciones = new JTextArea(3, 28);
    private final Map<String, JCheckBox> exclusiones = new LinkedHashMap<>();
    private final java.util.List<Plato> opciones = new ArrayList<>();
    private final DefaultTableModel modelo;
    private final JLabel resumen = new JLabel();
    private final JLabel precioFinal = new JLabel();

    public PersonalizarPlato(DetallePedido detalle, ControladorRestaurante controlador) {
        super(new BorderLayout(12, 12));
        this.original = detalle.copia();
        this.controlador = controlador;
        setPreferredSize(new Dimension(620, 530));
        JPanel encabezado = new JPanel(new GridLayout(0, 1, 0, 6));
        JLabel producto = new JLabel(
                detalle.getPlato().getNombre() + " · " + Tema.moneda(detalle.getPrecioUnitario()));
        producto.setFont(new Font("SansSerif", Font.BOLD, 20));
        encabezado.add(producto);
        JLabel etiqueta = new JLabel("Comensal o silla · solo se modifica esta unidad");
        etiqueta.setLabelFor(comensal);
        encabezado.add(etiqueta);
        comensal.putClientProperty("JTextField.placeholderText", "Ej.: Silla 3 / Ana");
        comensal.setText(detalle.getComensal());
        encabezado.add(comensal);
        add(encabezado, BorderLayout.NORTH);
        for (Plato p : controlador.getMenu())
            if (p.esAdicional())
                opciones.add(p);
        for (AdicionalPedido a : detalle.getAdicionales())
            if (opciones.stream().noneMatch(
                        p -> p.getCodigo().equalsIgnoreCase(a.getPlato().getCodigo())))
                opciones.add(a.getPlato());
        modelo = new DefaultTableModel(new Object[] {"Adicional para esta unidad",
                                               "Precio por porción", "Porciones (0 a 5)"},
                0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return columna == 2 && !"Bebidas".equals(original.getPlato().getCategoria());
            }
            @Override
            public Class<?> getColumnClass(int columna) {
                return columna == 2 ? Integer.class : String.class;
            }
        };
        for (Plato p : opciones) {
            int cantidad = 0;
            for (AdicionalPedido a : detalle.getAdicionales())
                if (a.getPlato().getCodigo().equalsIgnoreCase(p.getCodigo()))
                    cantidad = a.getCantidad();
            modelo.addRow(new Object[] {p.getNombre(), Tema.moneda(p.getPrecio()), cantidad});
        }
        JPanel centro = new JPanel(new BorderLayout(0, 12));
        JPanel adicionales = new JPanel(new GridLayout(0, 1, 0, 6));
        adicionales.setBorder(BorderFactory.createTitledBorder(
                "Adicionales para esta unidad · de 0 a 5 porciones"));
        for (int fila = 0; fila < opciones.size(); fila++) {
            final int indice = fila;
            Plato plato = opciones.get(fila);
            JPanel linea = new JPanel(new BorderLayout(12, 0));
            linea.add(new JLabel(plato.getNombre() + " · " + Tema.moneda(plato.getPrecio())),
                    BorderLayout.CENTER);
            JPanel cantidad = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            JButton menos = new JButton("−"), mas = new JButton("+");
            JLabel valor = new JLabel("", SwingConstants.CENTER);
            valor.setPreferredSize(new Dimension(24, 28));
            menos.getAccessibleContext().setAccessibleName(
                    "Quitar una porción de " + plato.getNombre());
            mas.getAccessibleContext().setAccessibleName(
                    "Agregar una porción de " + plato.getNombre());
            Runnable actualizar = () -> {
                int numero = ((Number) modelo.getValueAt(indice, 2)).intValue();
                valor.setText(String.valueOf(numero));
                menos.setEnabled(numero > 0 && modelo.isCellEditable(indice, 2));
                mas.setEnabled(numero < 5 && modelo.isCellEditable(indice, 2));
            };
            menos.addActionListener(e
                    -> modelo.setValueAt(
                            ((Number) modelo.getValueAt(indice, 2)).intValue() - 1, indice, 2));
            mas.addActionListener(e
                    -> modelo.setValueAt(
                            ((Number) modelo.getValueAt(indice, 2)).intValue() + 1, indice, 2));
            modelo.addTableModelListener(e -> {
                actualizar.run();
                actualizarResumen();
            });
            actualizar.run();
            cantidad.add(menos);
            cantidad.add(valor);
            cantidad.add(mas);
            linea.add(cantidad, BorderLayout.EAST);
            adicionales.add(linea);
        }
        centro.add(adicionales, BorderLayout.NORTH);
        JPanel sin = new JPanel(new GridLayout(0, 2, 8, 6));
        for (IngredienteReceta ingrediente :
                controlador.getReceta(detalle.getPlato().getCodigo())) {
            JCheckBox quitar = new JCheckBox("Sin " + ingrediente.getInsumo(),
                    detalle.getSinIngredientes().stream().anyMatch(
                            n -> n.equalsIgnoreCase(ingrediente.getInsumo())));
            quitar.setEnabled(!"Bebidas".equals(detalle.getPlato().getCategoria()));
            quitar.addItemListener(e -> actualizarResumen());
            exclusiones.put(ingrediente.getInsumo(), quitar);
            sin.add(quitar);
        }
        sin.setBorder(BorderFactory.createTitledBorder("Ingredientes que NO llevará esta unidad"));
        centro.add(sin, BorderLayout.CENTER);
        JScrollPane opcionesScroll = new JScrollPane(centro);
        opcionesScroll.setBorder(BorderFactory.createEmptyBorder());
        opcionesScroll.getVerticalScrollBar().setUnitIncrement(20);
        add(opcionesScroll, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout(0, 6));
        pie.add(new JLabel("Otras indicaciones de preparación (máximo 300 caracteres)"),
                BorderLayout.NORTH);
        indicaciones.setLineWrap(true);
        indicaciones.setWrapStyleWord(true);
        indicaciones.setText(detalle.getObservaciones());
        pie.add(new JScrollPane(indicaciones), BorderLayout.CENTER);
        JPanel confirmacion = new JPanel(new BorderLayout(8, 6));
        resumen.setForeground(Tema.TEXTO_SEC);
        precioFinal.setFont(new Font("SansSerif", Font.BOLD, 18));
        precioFinal.setForeground(Tema.ACENTO);
        confirmacion.add(resumen, BorderLayout.CENTER);
        confirmacion.add(precioFinal, BorderLayout.SOUTH);
        pie.add(confirmacion, BorderLayout.SOUTH);
        add(pie, BorderLayout.SOUTH);
        actualizarResumen();
    }

    private void actualizarResumen() {
        double total = original.getPrecioUnitario();
        java.util.List<String> cambios = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            int cantidad = ((Number) modelo.getValueAt(i, 2)).intValue();
            if (cantidad > 0) {
                total += cantidad * opciones.get(i).getPrecio();
                cambios.add(cantidad + " × " + opciones.get(i).getNombre());
            }
        }
        for (Map.Entry<String, JCheckBox> entrada : exclusiones.entrySet())
            if (entrada.getValue().isSelected())
                cambios.add("SIN " + entrada.getKey());
        resumen.setText("<html><div style='width:420px'>"
                + Tema.escapar(cambios.isEmpty() ? "Preparación original, sin adicionales"
                                                 : String.join(" · ", cambios))
                + "</div></html>");
        precioFinal.setText("Total de esta unidad: " + Tema.moneda(total));
    }

    public DetallePedido aplicar() throws IOException {
        DetallePedido detalle = original.copia();
        detalle.setCantidad(1);
        detalle.setComensal(comensal.getText());
        detalle.setObservaciones(indicaciones.getText());
        java.util.List<AdicionalPedido> adicionales = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            int cantidad = Integer.parseInt(String.valueOf(modelo.getValueAt(i, 2)));
            if (cantidad < 0 || cantidad > 5)
                throw new IllegalArgumentException("Cada adicional admite de 0 a 5 porciones.");
            if (cantidad > 0)
                adicionales.add(new AdicionalPedido(
                        opciones.get(i), cantidad, opciones.get(i).getPrecio()));
        }
        detalle.setAdicionales(adicionales);
        java.util.List<String> sin = new ArrayList<>();
        for (Map.Entry<String, JCheckBox> entrada : exclusiones.entrySet())
            if (entrada.getValue().isSelected())
                sin.add(entrada.getKey());
        detalle.setSinIngredientes(sin);
        return controlador.validarUnidad(detalle);
    }
}
