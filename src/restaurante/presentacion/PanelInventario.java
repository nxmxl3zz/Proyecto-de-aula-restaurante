package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;

public class PanelInventario extends javax.swing.JPanel {
    private final restaurante.datos.DatosRestaurante datos;
    private final restaurante.seguridad.Sesion usuario;
    private DefaultTableModel modelo;
    public PanelInventario() {this(null,null);}
    public PanelInventario(restaurante.datos.DatosRestaurante datos,restaurante.seguridad.Sesion usuario) {
        this.datos=datos;this.usuario=usuario;
        initComponents(); Tema.preparar(this);
        for(JPanel p:new JPanel[]{encabezado,titulos,pie,acciones}) p.setOpaque(false);
        modelo=new DefaultTableModel(new Object[]{"Insumo","Unidad","Existencias"},0) {
            @Override public boolean isCellEditable(int row,int column) {return false;}
        };
        tabla.setModel(modelo);
        tabla.getColumnModel().getColumn(2).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            private final java.text.DecimalFormat formato=new java.text.DecimalFormat("0.###");
            @Override public Component getTableCellRendererComponent(javax.swing.JTable t,Object valor,boolean seleccion,boolean foco,int fila,int columna) {
                super.getTableCellRendererComponent(t,valor,seleccion,foco,fila,columna);
                setHorizontalAlignment(SwingConstants.RIGHT);setText(formato.format(((Number)valor).doubleValue()));return this;
            }
        }); Tema.tabla(tabla); tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            btnAjustar.setEnabled(tabla.getSelectedRow()>=0); btnEliminar.setEnabled(tabla.getSelectedRow()>=0);
        });
        btnAjustar.setEnabled(false); btnEliminar.setEnabled(false);
        Tema.principal(btnAgregar); btnEliminar.setForeground(new Color(0xED8171));
        btnAgregar.addActionListener(e -> agregar()); btnAjustar.addActionListener(e -> ajustar());
        btnEliminar.addActionListener(e -> {
            int fila=tabla.getSelectedRow();
            if(fila>=0 && JOptionPane.showConfirmDialog(this,"¿Eliminar el insumo seleccionado?","Eliminar insumo",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {modelo.removeRow(fila);persistir();}
        });
        refrescar();
    }
    private void agregar() {
        JTextField nombre = new JTextField();
        JTextField unidad = new JTextField("unidades");
        JSpinner cantidad = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000000000.0, 1.0));
        JPanel formulario = new JPanel(new GridLayout(3, 2, 8, 8));
        formulario.add(new JLabel("Insumo")); formulario.add(nombre);
        formulario.add(new JLabel("Unidad (kg, litros, unidades…)")); formulario.add(unidad);
        formulario.add(new JLabel("Existencias iniciales")); formulario.add(cantidad);
        if (JOptionPane.showConfirmDialog(this, formulario, "Agregar insumo", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try { cantidad.commitEdit(); } catch (java.text.ParseException ex) { error("Escribe una cantidad válida."); return; }
        String texto = nombre.getText().trim();
        if (texto.isEmpty() || unidad.getText().trim().isEmpty()) { error("Completa el nombre y la unidad."); return; }
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (texto.equalsIgnoreCase(String.valueOf(modelo.getValueAt(i, 0)))) { error("Ese insumo ya existe."); return; }
        }
        modelo.addRow(new Object[]{texto, unidad.getText().trim(), ((Number) cantidad.getValue()).doubleValue()});
        persistir();
    }

    private void ajustar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        JSpinner cantidad = new JSpinner(new SpinnerNumberModel(((Number) modelo.getValueAt(fila, 2)).doubleValue(),
                0.0, 1000000000.0, 1.0));
        if (JOptionPane.showConfirmDialog(this, cantidad, "Nueva existencia total", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try { cantidad.commitEdit(); } catch (java.text.ParseException ex) { error("Escribe una cantidad válida y no negativa."); return; }
        modelo.setValueAt(((Number) cantidad.getValue()).doubleValue(), fila, 2);
        persistir();
    }

    public void refrescar() {
        if(datos==null)return;
        modelo.setRowCount(0);
        for(restaurante.entidades.Insumo i:datos.getInventario())modelo.addRow(new Object[]{i.getNombre(),i.getUnidad(),i.getCantidad()});
    }
    private void persistir() {
        if(datos==null)return;
        java.util.List<restaurante.entidades.Insumo> insumos=new java.util.ArrayList<>();
        for(int i=0;i<modelo.getRowCount();i++)insumos.add(new restaurante.entidades.Insumo(String.valueOf(modelo.getValueAt(i,0)),String.valueOf(modelo.getValueAt(i,1)),((Number)modelo.getValueAt(i,2)).doubleValue()));
        try {datos.guardarInventario(usuario,insumos);}
        catch(java.io.IOException ex){refrescar();error("No se guardaron los cambios.\n"+ex.getMessage());}
    }
    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Revisa los datos", JOptionPane.WARNING_MESSAGE);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        this.setLayout(new java.awt.BorderLayout(0, 20));
        encabezado = new javax.swing.JPanel();
        encabezado.setLayout(new java.awt.BorderLayout(12, 12));
        titulos = new javax.swing.JPanel();
        titulos.setLayout(new java.awt.BorderLayout(0, 6));
        titulo = new javax.swing.JLabel();
        titulo.setText("Inventario");
        titulo.setFont(new java.awt.Font("Serif", 1, 32));
        titulos.add(titulo, java.awt.BorderLayout.NORTH);
        subtitulo = new javax.swing.JLabel();
        subtitulo.setText("Existencias disponibles · descuento automático al cobrar");
        subtitulo.setFont(new java.awt.Font("SansSerif", 0, 15));
        titulos.add(subtitulo, java.awt.BorderLayout.SOUTH);
        encabezado.add(titulos, java.awt.BorderLayout.WEST);
        btnAgregar = new javax.swing.JButton();
        btnAgregar.setText("+ Agregar insumo");
        encabezado.add(btnAgregar, java.awt.BorderLayout.EAST);
        this.add(encabezado, java.awt.BorderLayout.NORTH);
        scroll = new javax.swing.JScrollPane();
        tabla = new javax.swing.JTable();
        scroll.setViewportView(tabla);
        this.add(scroll, java.awt.BorderLayout.CENTER);
        pie = new javax.swing.JPanel();
        pie.setLayout(new java.awt.BorderLayout(12, 12));
        acciones = new javax.swing.JPanel();
        acciones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        btnAjustar = new javax.swing.JButton();
        btnAjustar.setText("Ajustar existencias");
        acciones.add(btnAjustar);
        btnEliminar = new javax.swing.JButton();
        btnEliminar.setText("Eliminar insumo");
        acciones.add(btnEliminar);
        pie.add(acciones, java.awt.BorderLayout.WEST);
        nota = new javax.swing.JLabel();
        nota.setText("Cada cobro descuenta los ingredientes de la receta del plato.");
        nota.setFont(new java.awt.Font("SansSerif", 0, 12));
        pie.add(nota, java.awt.BorderLayout.SOUTH);
        this.add(pie, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel encabezado;
    private javax.swing.JPanel titulos;
    private javax.swing.JLabel titulo;
    private javax.swing.JLabel subtitulo;
    private javax.swing.JButton btnAgregar;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable tabla;
    private javax.swing.JPanel pie;
    private javax.swing.JPanel acciones;
    private javax.swing.JButton btnAjustar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JLabel nota;
    // End of variables declaration//GEN-END:variables
}
