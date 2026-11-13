package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import restaurante.entidades.Plato;

/** Producto principal del catálogo con nombre completo, precio e ingredientes consultables. */
public final class TarjetaProducto extends javax.swing.JPanel {
    public static final int ALTO = 390;
    public static final int ALTO_COMPACTO = 280;
    private final Plato plato;
    private boolean compacta;
    private final JLabel disponibilidad = new JLabel(" ");
    private java.util.List<restaurante.entidades.IngredienteReceta> receta =
            java.util.Collections.emptyList();
    public TarjetaProducto() {
        this(new Plato("", "Plato", "Descripción", 0), () -> {});
    }
    public TarjetaProducto(Plato plato, Runnable agregar) {
        this.plato = plato;
        initComponents();
        Tema.tarjeta(this);
        informacion.setOpaque(false);
        pie.setOpaque(false);
        foto.setHorizontalAlignment(SwingConstants.CENTER);
        foto.setIcon(Imagenes.producto(plato, 206, 154));
        nombre.setFont(new Font("SansSerif", Font.BOLD, 17));
        nombre.setText(
                "<html><div style='width:160px'>" + escapar(plato.getNombre()) + "</div></html>");
        nombre.setPreferredSize(new Dimension(210, 44));
        nombre.setToolTipText(plato.getNombre());
        descripcion.setText("<html><div style='width:145px'>" + escapar(plato.getDescripcion())
                + "</div></html>");
        descripcion.setForeground(Tema.TEXTO_SEC);
        descripcion.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descripcion.setToolTipText(plato.getDescripcion());
        precio.setText(Tema.moneda(plato.getPrecio()));
        Tema.principal(btnAgregar);
        btnAgregar.setToolTipText("Agregar " + plato.getNombre());
        btnAgregar.setText("Agregar");
        btnAgregar.setMargin(new Insets(7, 8, 7, 8));
        btnAgregar.getAccessibleContext().setAccessibleName("Agregar " + plato.getNombre());
        precio.setFont(new Font("SansSerif", Font.BOLD, 16));
        disponibilidad.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel estado = new JPanel(new BorderLayout());
        estado.setOpaque(false);
        btnInsumos.setText("Ingredientes");
        btnInsumos.setMargin(new Insets(3, 3, 3, 3));
        btnInsumos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        estado.add(disponibilidad, BorderLayout.WEST);
        estado.add(btnInsumos, BorderLayout.EAST);
        pie.add(estado, BorderLayout.SOUTH);
        btnAgregar.addActionListener(e -> agregar.run());
        btnInsumos.putClientProperty("JButton.buttonType", "borderless");
        btnInsumos.addActionListener(e -> {
            StringBuilder detalle = new StringBuilder("Por cada " + plato.getNombre() + ":\n\n");
            for (restaurante.entidades.IngredienteReceta ingrediente : receta)
                detalle.append(ingrediente.getInsumo())
                        .append(": ")
                        .append(new java.text.DecimalFormat("0.###").format(
                                ingrediente.getCantidad()))
                        .append(" ")
                        .append(ingrediente.getUnidad())
                        .append("\n");
            detalle.append("\nLos insumos se descuentan al confirmar el cobro.");
            JOptionPane.showMessageDialog(
                    this, detalle.toString(), "Insumos del plato", JOptionPane.INFORMATION_MESSAGE);
        });
        setPreferredSize(new Dimension(230, ALTO));
    }
    public void configurarReceta(java.util.List<restaurante.entidades.IngredienteReceta> receta) {
        this.receta = receta;
        btnInsumos.setEnabled(!receta.isEmpty());
        btnAgregar.setEnabled(!receta.isEmpty());
        if (receta.isEmpty())
            btnInsumos.setText("Receta pendiente");
    }
    public void configurarDisponibilidad(String motivo) {
        boolean disponible = motivo == null;
        btnAgregar.setEnabled(disponible);
        disponibilidad.setText(disponible ? "Disponible" : "No disponible");
        disponibilidad.setForeground(disponible ? Tema.EXITO : Tema.ACENTO);
        disponibilidad.setToolTipText(motivo);
        if (!disponible)
            btnAgregar.setToolTipText(motivo);
    }
    public void agregarSiDisponible() {
        if (btnAgregar.isEnabled())
            btnAgregar.doClick();
    }
    public void configurarCompacta(boolean valor) {
        if (compacta == valor)
            return;
        compacta = valor;
        foto.setIcon(Imagenes.producto(plato, valor ? 140 : 206, valor ? 105 : 154));
        descripcion.setVisible(!valor);
        setToolTipText(plato.getDescripcion());
        setPreferredSize(new Dimension(230, valor ? ALTO_COMPACTO : ALTO));
        revalidate();
    }
    private String escapar(String texto) {
        return texto == null
                ? ""
                : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

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
