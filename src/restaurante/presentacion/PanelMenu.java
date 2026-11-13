package restaurante.presentacion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import restaurante.entidades.Plato;
import restaurante.controlador.ControladorRestaurante;

/** Pantalla de administración del menú; el diseño visual se edita en PanelMenu.form. */
public final class PanelMenu extends javax.swing.JPanel {
    private final ControladorRestaurante controlador;
    private DefaultTableModel modeloPlatos;
    private TableRowSorter<DefaultTableModel> ordenadorFilas;
    private final List<Plato> platos = new java.util.ArrayList<>();
    private final DecimalFormat formatoPrecio =
            new DecimalFormat("$ #,##0.##", new DecimalFormatSymbols(new Locale("es", "CO")));

    public PanelMenu() {
        this(null);
    }
    public PanelMenu(ControladorRestaurante controlador) {
        this.controlador = controlador;
        if (controlador != null)
            platos.addAll(controlador.getMenu());
        initComponents();
        aplicarEstilo();
        configurarTabla();
        conectarEventos();
        recargarTabla();
        btnNuevo.setEnabled(controlador != null);
    }

    private void aplicarEstilo() {
        setOpaque(true);
        setBackground(Tema.FONDO);
        Tema.preparar(this);
        panelZonaSuperior.setOpaque(false);
        panelEncabezado.setOpaque(false);
        panelHerramientas.setOpaque(false);
        panelTabla.setOpaque(false);
        panelPie.setOpaque(false);
        panelAcciones.setOpaque(false);

        etiquetaTitulo.setFont(new Font("SansSerif", Font.BOLD, 28));
        etiquetaTitulo.setForeground(Tema.TEXTO);
        etiquetaSubtitulo.setForeground(Tema.TEXTO_SEC);
        etiquetaConteo.setForeground(Tema.TEXTO_SEC);

        campoBusqueda.putClientProperty(
                "JTextField.placeholderText", "Buscar plato por nombre o código");
        campoBusqueda.setPreferredSize(new Dimension(100, 42));
        campoBusqueda.setBackground(Tema.LATERAL);
        campoBusqueda.setForeground(Tema.TEXTO);
        campoBusqueda.setCaretColor(Tema.TEXTO);
        Tema.atajo(this, "control F", () -> campoBusqueda.requestFocusInWindow());
        Tema.atajo(tablaPlatos, "control E", () -> {
            if (btnEditar.isEnabled())
                btnEditar.doClick();
        });
        campoBusqueda.setToolTipText("Buscar plato · Ctrl+F");

        estilizarBotonPrincipal(btnNuevo);
        estilizarBotonSecundario(btnEditar);
        estilizarBotonPeligro(btnEliminar);
        Tema.principal(btnNuevo);
        btnReceta.setEnabled(false);
        btnReceta.addActionListener(e -> configurarReceta());
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);

        scrollPlatos.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        scrollPlatos.getViewport().setBackground(Tema.TARJETA);
        tablaPlatos.setBackground(Tema.TARJETA);
        tablaPlatos.setForeground(Tema.TEXTO);
        tablaPlatos.setSelectionBackground(new Color(0x55432B));
        tablaPlatos.setSelectionForeground(Tema.TEXTO);
        tablaPlatos.setGridColor(Tema.BORDE);
        tablaPlatos.setRowHeight(64);
        tablaPlatos.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable tabla, Object valor,
                    boolean seleccion, boolean foco, int fila, int columna) {
                super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila, columna);
                int modelo = tabla.convertRowIndexToModel(fila);
                Plato plato = buscarPlato(String.valueOf(modeloPlatos.getValueAt(modelo, 0)));
                setIcon(plato == null ? null : Imagenes.cargar(Imagenes.archivo(plato), 54, 44));
                setIconTextGap(10);
                setText("<html><div style='width:130px'>" + Tema.escapar(String.valueOf(valor))
                        + "</div></html>");
                setToolTipText(String.valueOf(valor));
                return this;
            }
        });
        tablaPlatos.setShowVerticalLines(false);
        tablaPlatos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tablaPlatos.getTableHeader().setBackground(Tema.LATERAL);
        tablaPlatos.getTableHeader().setForeground(Tema.TEXTO_SEC);
        tablaPlatos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tablaPlatos.getTableHeader().setPreferredSize(new Dimension(0, 42));
        tablaPlatos.getTableHeader().setReorderingAllowed(false);
        tablaPlatos.getColumnModel().getColumn(0).setPreferredWidth(80);
        tablaPlatos.getColumnModel().getColumn(1).setPreferredWidth(260);
        tablaPlatos.getColumnModel().getColumn(1).setMinWidth(245);
        tablaPlatos.getColumnModel().getColumn(2).setPreferredWidth(115);
        tablaPlatos.getColumnModel().getColumn(3).setPreferredWidth(290);
        tablaPlatos.getColumnModel().getColumn(4).setPreferredWidth(100);
        tablaPlatos.getColumnModel().getColumn(4).setCellRenderer(new RendererPrecio());
    }

    private void estilizarBotonPrincipal(javax.swing.JButton boton) {
        boton.setBackground(Tema.ACENTO);
        boton.setForeground(Color.WHITE);
        boton.setFont(new Font("SansSerif", Font.BOLD, 14));
        boton.setFocusPainted(true);
        boton.setMargin(new java.awt.Insets(8, 14, 8, 14));
        boton.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void estilizarBotonSecundario(javax.swing.JButton boton) {
        boton.setBackground(Tema.TARJETA);
        boton.setForeground(Tema.TEXTO);
        boton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        boton.setFocusPainted(true);
        boton.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void estilizarBotonPeligro(javax.swing.JButton boton) {
        boton.setBackground(Tema.TARJETA);
        boton.setForeground(new Color(0xFF7B83));
        boton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        boton.setFocusPainted(true);
        boton.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void configurarTabla() {
        modeloPlatos = (DefaultTableModel) tablaPlatos.getModel();
        ordenadorFilas = new TableRowSorter<>(modeloPlatos);
        tablaPlatos.setRowSorter(ordenadorFilas);
    }

    private void conectarEventos() {
        campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrarPlatos();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrarPlatos();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrarPlatos();
            }
        });

        tablaPlatos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean seleccionado = controlador != null && tablaPlatos.getSelectedRow() >= 0;
                btnReceta.setEnabled(seleccionado);
                btnEditar.setEnabled(seleccionado);
                btnEliminar.setEnabled(seleccionado);
            }
        });

        tablaPlatos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (controlador != null && e.getClickCount() == 2
                        && tablaPlatos.getSelectedRow() >= 0) {
                    editarPlato();
                }
            }
        });
    }

    private void filtrarPlatos() {
        String texto = campoBusqueda.getText().trim();
        if (texto.isEmpty()) {
            ordenadorFilas.setRowFilter(null);
        } else {
            ordenadorFilas.setRowFilter(
                    RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(texto)));
        }
        actualizarContador();
    }

    private void actualizarContador() {
        int cantidad = tablaPlatos.getRowCount();
        etiquetaConteo.setText(cantidad + (cantidad == 1 ? " plato" : " platos"));
    }

    private void nuevoPlato() {
        mostrarFormularioPlato(null);
    }

    private void editarPlato() {
        int filaVista = tablaPlatos.getSelectedRow();
        if (filaVista >= 0) {
            int filaModelo = tablaPlatos.convertRowIndexToModel(filaVista);
            String codigo = String.valueOf(modeloPlatos.getValueAt(filaModelo, 0));
            mostrarFormularioPlato(buscarPlato(codigo));
        }
    }

    private void mostrarFormularioPlato(Plato platoActual) {
        boolean editar = platoActual != null;
        JTextField codigo = new JTextField(18);
        JTextField nombre = new JTextField(18);
        JComboBox<String> categoria =
                new JComboBox<>(ControladorRestaurante.getCategorias().toArray(new String[0]));
        JTextArea descripcion = new JTextArea(3, 18);
        JTextField precio = new JTextField(18);
        descripcion.setLineWrap(true);
        descripcion.setWrapStyleWord(true);

        if (editar) {
            codigo.setText(platoActual.getCodigo());
            nombre.setText(platoActual.getNombre());
            categoria.setSelectedItem(platoActual.getCategoria());
            descripcion.setText(platoActual.getDescripcion());
            precio.setText(String.valueOf(platoActual.getPrecio()));
        }

        JPanel formulario = new JPanel(new GridLayout(5, 2, 10, 10));
        formulario.setPreferredSize(new Dimension(410, 230));
        formulario.add(new JLabel("Código"));
        formulario.add(codigo);
        formulario.add(new JLabel("Nombre del plato"));
        formulario.add(nombre);
        formulario.add(new JLabel("Categoría"));
        formulario.add(categoria);
        formulario.add(new JLabel("Descripción"));
        formulario.add(new javax.swing.JScrollPane(descripcion));
        formulario.add(new JLabel("Precio"));
        formulario.add(precio);

        int respuesta = JOptionPane.showConfirmDialog(this, formulario,
                editar ? "Editar plato" : "Nuevo plato", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION)
            return;

        String codigoTexto = codigo.getText().trim();
        String nombreTexto = nombre.getText().trim();
        String categoriaTexto = String.valueOf(categoria.getSelectedItem());
        String descripcionTexto = descripcion.getText().trim();
        double precioValor;
        try {
            precioValor = Double.parseDouble(precio.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            mostrarError("Escribe un precio válido, por ejemplo 24000 o 24000,50.");
            return;
        }

        if (codigoTexto.isEmpty() || nombreTexto.isEmpty() || descripcionTexto.isEmpty()) {
            mostrarError("Completa el código, el nombre y la descripción del plato.");
            return;
        }
        if (!Double.isFinite(precioValor) || precioValor <= 0) {
            mostrarError("El precio debe ser mayor que cero.");
            return;
        }
        if (codigoDuplicado(codigoTexto, platoActual)) {
            mostrarError("Ya existe un plato con ese código.");
            return;
        }

        Plato platoActualizado =
                new Plato(codigoTexto, nombreTexto, descripcionTexto, precioValor, categoriaTexto);
        List<Plato> nuevos = new java.util.ArrayList<>(platos);
        if (editar) {
            int indice = nuevos.indexOf(platoActual);
            if (indice >= 0)
                nuevos.set(indice, platoActualizado);
        } else
            nuevos.add(platoActualizado);
        if (!guardarMenu(nuevos))
            return;
        platos.clear();
        platos.addAll(nuevos);
        recargarTabla();
    }

    private boolean codigoDuplicado(String codigo, Plato platoIgnorado) {
        for (Plato plato : platos) {
            if (plato != platoIgnorado && codigo.equalsIgnoreCase(plato.getCodigo())) {
                return true;
            }
        }
        return false;
    }

    private Plato buscarPlato(String codigo) {
        for (Plato plato : platos) {
            if (codigo.equalsIgnoreCase(plato.getCodigo()))
                return plato;
        }
        return null;
    }

    private void recargarTabla() {
        modeloPlatos.setRowCount(0);
        for (Plato plato : platos) {
            modeloPlatos.addRow(new Object[] {plato.getCodigo(), plato.getNombre(),
                    plato.getCategoria(), plato.getDescripcion(), plato.getPrecio()});
        }
        actualizarContador();
    }

    private void eliminarPlato() {
        int filaVista = tablaPlatos.getSelectedRow();
        if (filaVista < 0)
            return;
        int confirmar =
                JOptionPane.showConfirmDialog(this, "¿Quieres eliminar el plato seleccionado?",
                        "Eliminar plato", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmar == JOptionPane.YES_OPTION) {
            int filaModelo = tablaPlatos.convertRowIndexToModel(filaVista);
            String codigo = String.valueOf(modeloPlatos.getValueAt(filaModelo, 0));
            Plato plato = buscarPlato(codigo);
            List<Plato> nuevos = new java.util.ArrayList<>(platos);
            if (plato != null)
                nuevos.remove(plato);
            if (!guardarMenu(nuevos))
                return;
            platos.clear();
            platos.addAll(nuevos);
            recargarTabla();
        }
    }

    private void configurarReceta() {
        int fila = tablaPlatos.getSelectedRow();
        if (fila < 0 || controlador == null)
            return;
        String codigo = String.valueOf(
                modeloPlatos.getValueAt(tablaPlatos.convertRowIndexToModel(fila), 0));
        List<restaurante.entidades.Insumo> inventario = controlador.getInventario();
        if (inventario.isEmpty()) {
            mostrarError("Agrega primero los insumos al inventario.");
            return;
        }
        List<restaurante.entidades.IngredienteReceta> receta = controlador.getReceta(codigo);
        DefaultTableModel modelo =
                new DefaultTableModel(new Object[] {"Insumo", "Unidad", "Cantidad por plato"}, 0) {
                    @Override
                    public boolean isCellEditable(int fila, int columna) {
                        return columna == 2;
                    }
                    @Override
                    public Class<?> getColumnClass(int columna) {
                        return columna == 2 ? Double.class : String.class;
                    }
                };
        for (restaurante.entidades.Insumo insumo : inventario) {
            double cantidad = 0;
            for (restaurante.entidades.IngredienteReceta ingrediente : receta)
                if (ingrediente.getInsumo().equalsIgnoreCase(insumo.getNombre()))
                    cantidad = ingrediente.getCantidad();
            modelo.addRow(new Object[] {insumo.getNombre(), insumo.getUnidad(), cantidad});
        }
        javax.swing.JTable tabla = new javax.swing.JTable(modelo);
        Tema.tabla(tabla);
        JPanel formulario = new JPanel(new BorderLayout(0, 12));
        formulario.add(
                new JLabel("Cantidad por una unidad del plato. Usa 0 para excluir un insumo."),
                BorderLayout.NORTH);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(620, 400));
        formulario.add(scroll, BorderLayout.CENTER);
        while (JOptionPane.showConfirmDialog(this, formulario, "Receta / insumos · " + codigo,
                       JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION) {
            if (tabla.isEditing() && !tabla.getCellEditor().stopCellEditing()) {
                mostrarError("Escribe una cantidad numérica válida.");
                continue;
            }
            List<restaurante.entidades.IngredienteReceta> ingredientes =
                    new java.util.ArrayList<>();
            try {
                for (int i = 0; i < modelo.getRowCount(); i++) {
                    double cantidad = ((Number) modelo.getValueAt(i, 2)).doubleValue();
                    if (!Double.isFinite(cantidad) || cantidad < 0)
                        throw new IllegalArgumentException(
                                "Las cantidades deben ser finitas y no negativas.");
                    if (cantidad > 0)
                        ingredientes.add(new restaurante.entidades.IngredienteReceta(
                                String.valueOf(modelo.getValueAt(i, 0)),
                                String.valueOf(modelo.getValueAt(i, 1)), cantidad));
                }
                controlador.guardarReceta(codigo, ingredientes);
                return;
            } catch (java.io.IOException | IllegalArgumentException | SecurityException ex) {
                mostrarError(ex.getMessage());
            }
        }
    }

    private boolean guardarMenu(List<Plato> nuevos) {
        if (controlador == null) {
            throw new IllegalStateException("La vista previa del menú no permite guardar cambios.");
        }
        try {
            controlador.guardarMenu(nuevos);
            return true;
        } catch (java.io.IOException | SecurityException ex) {
            mostrarError(
                    "No se pudo guardar el menú. No se aplicaron cambios.\n" + ex.getMessage());
            return false;
        }
    }
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this, mensaje, "Revisa los datos", JOptionPane.WARNING_MESSAGE);
    }

    private class RendererPrecio extends DefaultTableCellRenderer {
        RendererPrecio() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value instanceof Number ? formatoPrecio.format(((Number) value).doubleValue())
                                            : "");
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
            return this;
        }
    }

    /** This method is called from within the constructor to initialize the form. */
    @SuppressWarnings("unchecked")

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelZonaSuperior = new javax.swing.JPanel();
        panelEncabezado = new javax.swing.JPanel();
        etiquetaTitulo = new javax.swing.JLabel();
        etiquetaSubtitulo = new javax.swing.JLabel();
        panelHerramientas = new javax.swing.JPanel();
        campoBusqueda = new javax.swing.JTextField();
        btnNuevo = new javax.swing.JButton();
        panelTabla = new javax.swing.JPanel();
        scrollPlatos = new javax.swing.JScrollPane();
        tablaPlatos = new javax.swing.JTable();
        panelPie = new javax.swing.JPanel();
        etiquetaConteo = new javax.swing.JLabel();
        panelAcciones = new javax.swing.JPanel();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnReceta = new javax.swing.JButton();
        btnReceta.setText("Receta / insumos");

        setBackground(Tema.FONDO);
        setLayout(new java.awt.BorderLayout());

        panelZonaSuperior.setOpaque(false);
        panelZonaSuperior.setLayout(new java.awt.BorderLayout(0, 20));

        panelEncabezado.setOpaque(false);
        panelEncabezado.setLayout(new java.awt.BorderLayout(0, 6));

        etiquetaTitulo.setFont(new java.awt.Font("SansSerif", 1, 28)); // NOI18N
        etiquetaTitulo.setForeground(Tema.TEXTO);
        etiquetaTitulo.setText("Menú");
        panelEncabezado.add(etiquetaTitulo, java.awt.BorderLayout.PAGE_START);

        etiquetaSubtitulo.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        etiquetaSubtitulo.setForeground(Tema.TEXTO_SEC);
        etiquetaSubtitulo.setText("Administra los platos disponibles en el restaurante.");
        etiquetaSubtitulo.setToolTipText("");
        panelEncabezado.add(etiquetaSubtitulo, java.awt.BorderLayout.CENTER);

        panelZonaSuperior.add(panelEncabezado, java.awt.BorderLayout.PAGE_START);

        panelHerramientas.setOpaque(false);
        panelHerramientas.setLayout(new java.awt.BorderLayout(12, 0));

        campoBusqueda.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        panelHerramientas.add(campoBusqueda, java.awt.BorderLayout.CENTER);

        btnNuevo.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        btnNuevo.setText("+  Nuevo plato");
        btnNuevo.addActionListener(this::btnNuevoActionPerformed);
        panelHerramientas.add(btnNuevo, java.awt.BorderLayout.LINE_END);

        panelZonaSuperior.add(panelHerramientas, java.awt.BorderLayout.PAGE_END);

        add(panelZonaSuperior, java.awt.BorderLayout.PAGE_START);

        panelTabla.setOpaque(false);
        panelTabla.setLayout(new java.awt.BorderLayout(0, 12));

        tablaPlatos.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        tablaPlatos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Código", "Plato", "Categoría", "Descripción", "Precio"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaPlatos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        scrollPlatos.setViewportView(tablaPlatos);

        panelTabla.add(scrollPlatos, java.awt.BorderLayout.CENTER);

        panelPie.setOpaque(false);
        panelPie.setLayout(new java.awt.BorderLayout());

        etiquetaConteo.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        etiquetaConteo.setForeground(Tema.TEXTO_SEC);
        etiquetaConteo.setText("0 platos");
        panelPie.add(etiquetaConteo, java.awt.BorderLayout.LINE_START);

        panelAcciones.setOpaque(false);
        panelAcciones.setLayout(new java.awt.BorderLayout(8, 0));
        panelAcciones.add(btnReceta, java.awt.BorderLayout.LINE_START);

        btnEditar.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        btnEditar.setText("Editar");
        btnEditar.addActionListener(this::btnEditarActionPerformed);
        panelAcciones.add(btnEditar, java.awt.BorderLayout.CENTER);

        btnEliminar.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(this::btnEliminarActionPerformed);
        panelAcciones.add(btnEliminar, java.awt.BorderLayout.LINE_END);

        panelPie.add(panelAcciones, java.awt.BorderLayout.LINE_END);

        panelTabla.add(panelPie, java.awt.BorderLayout.PAGE_END);

        add(panelTabla, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        nuevoPlato();
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        editarPlato();
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        eliminarPlato();
    }//GEN-LAST:event_btnEliminarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnReceta;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JTextField campoBusqueda;
    private javax.swing.JLabel etiquetaConteo;
    private javax.swing.JLabel etiquetaSubtitulo;
    private javax.swing.JLabel etiquetaTitulo;
    private javax.swing.JPanel panelAcciones;
    private javax.swing.JPanel panelEncabezado;
    private javax.swing.JPanel panelHerramientas;
    private javax.swing.JPanel panelPie;
    private javax.swing.JPanel panelTabla;
    private javax.swing.JPanel panelZonaSuperior;
    private javax.swing.JScrollPane scrollPlatos;
    private javax.swing.JTable tablaPlatos;
    // End of variables declaration//GEN-END:variables
}
