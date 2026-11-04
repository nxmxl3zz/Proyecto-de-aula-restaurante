package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import restaurante.entidades.Plato;

public class TarjetaProducto extends javax.swing.JPanel {
    public static final int ALTO=370;
    private java.util.List<restaurante.entidades.IngredienteReceta> receta=java.util.Collections.emptyList();
    public TarjetaProducto() { this(new Plato("", "Plato", "Descripción", 0), () -> {}); }
    public TarjetaProducto(Plato plato, Runnable agregar) {
        initComponents(); Tema.tarjeta(this);
        informacion.setOpaque(false); pie.setOpaque(false);
        foto.setHorizontalAlignment(SwingConstants.CENTER);
        foto.setIcon(Imagenes.cargar(Imagenes.archivo(plato), 180, 134));
        nombre.setText(plato.getNombre());
        descripcion.setText("<html><div style='width:145px'>" + escapar(plato.getDescripcion()) + "</div></html>");
        descripcion.setForeground(Tema.TEXTO_SEC);
        descripcion.setToolTipText(plato.getDescripcion());
        precio.setText(Tema.moneda(plato.getPrecio()));
        Tema.principal(btnAgregar); btnAgregar.setToolTipText("Agregar " + plato.getNombre());
        btnAgregar.addActionListener(e -> agregar.run());
        btnInsumos.putClientProperty("JButton.buttonType","borderless");
        btnInsumos.addActionListener(e -> {
            StringBuilder detalle=new StringBuilder("Por cada "+plato.getNombre()+":\n\n");
            for(restaurante.entidades.IngredienteReceta ingrediente:receta)
                detalle.append(ingrediente.getInsumo()).append(": ").append(new java.text.DecimalFormat("0.###").format(ingrediente.getCantidad())).append(" ").append(ingrediente.getUnidad()).append("\n");
            detalle.append("\nLos insumos se descuentan al confirmar el cobro.");
            JOptionPane.showMessageDialog(this,detalle.toString(),"Insumos del plato",JOptionPane.INFORMATION_MESSAGE);
        });
        setPreferredSize(new Dimension(230, ALTO));
    }
    public void configurarReceta(java.util.List<restaurante.entidades.IngredienteReceta> receta) {
        this.receta=receta;btnInsumos.setEnabled(!receta.isEmpty());btnAgregar.setEnabled(!receta.isEmpty());
        if(receta.isEmpty())btnInsumos.setText("Receta pendiente");
    }
    private String escapar(String texto) { return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        this.setLayout(new java.awt.BorderLayout(0, 10));
        foto = new javax.swing.JLabel();
        foto.setText("");
        foto.setFont(new java.awt.Font("SansSerif", 0, 14));
        this.add(foto, java.awt.BorderLayout.NORTH);
        informacion = new javax.swing.JPanel();
        informacion.setLayout(new java.awt.BorderLayout(0, 5));
        nombre = new javax.swing.JLabel();
        nombre.setText("Nombre del plato");
        nombre.setFont(new java.awt.Font("Serif", 1, 22));
        informacion.add(nombre, java.awt.BorderLayout.NORTH);
        descripcion = new javax.swing.JLabel();
        descripcion.setText("Descripción");
        descripcion.setFont(new java.awt.Font("SansSerif", 0, 12));
        informacion.add(descripcion, java.awt.BorderLayout.CENTER);
        pie = new javax.swing.JPanel();
        pie.setLayout(new java.awt.BorderLayout(12, 12));
        precio = new javax.swing.JLabel();
        precio.setText("$ 0");
        precio.setFont(new java.awt.Font("SansSerif", 1, 18));
        pie.add(precio, java.awt.BorderLayout.WEST);
        btnAgregar = new javax.swing.JButton();
        btnAgregar.setText("+");
        pie.add(btnAgregar, java.awt.BorderLayout.EAST);
        btnInsumos = new javax.swing.JButton();btnInsumos.setText("Ver insumos");
        pie.add(btnInsumos,java.awt.BorderLayout.SOUTH);
        informacion.add(pie, java.awt.BorderLayout.SOUTH);
        this.add(informacion, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel foto;
    private javax.swing.JPanel informacion;
    private javax.swing.JLabel nombre;
    private javax.swing.JLabel descripcion;
    private javax.swing.JPanel pie;
    private javax.swing.JLabel precio;
    private javax.swing.JButton btnAgregar;
    private javax.swing.JButton btnInsumos;
    // End of variables declaration//GEN-END:variables
}
