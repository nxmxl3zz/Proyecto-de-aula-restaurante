package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import restaurante.entidades.Pedido;

public class PanelInicio extends javax.swing.JPanel {
    public PanelInicio() {
        initComponents(); Tema.preparar(this);
        encabezado.setOpaque(false); contenido.setOpaque(false); metricas.setOpaque(false); zonaInferior.setOpaque(false); botones.setOpaque(false);
        for (JPanel p : new JPanel[]{tarjetaVentas,tarjetaPedidos,tarjetaMesas,accesos,destacado}) Tema.tarjeta(p);
        for (JLabel l : new JLabel[]{lblVentas,lblPedidos,lblMesas}) l.setForeground(Tema.ACENTO);
        foto.setIcon(Imagenes.cargar("la-doble",300,260)); foto.setHorizontalAlignment(SwingConstants.CENTER);
        Tema.principal(btnPedido);
    }
    public void configurar(Runnable pedido,Runnable mesas,Runnable menu,Runnable inventario) {
        btnPedido.addActionListener(e -> pedido.run()); btnMesas.addActionListener(e -> mesas.run());
        btnMenu.addActionListener(e -> menu.run()); btnInventario.addActionListener(e -> inventario.run());
    }
    public void refrescar(List<Pedido> pedidos,int mesas) {
        lblVentas.setText(Tema.moneda(pedidos.stream().mapToDouble(Pedido::calcularTotal).sum()));
        lblPedidos.setText(String.valueOf(pedidos.size())); lblMesas.setText(mesas+" de 10");
    }
    public void permitirAdministracion(boolean permitido) {
        btnMenu.setVisible(permitido); btnInventario.setVisible(permitido);
        tarjetaVentas.setVisible(permitido); tarjetaPedidos.setVisible(permitido);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        this.setLayout(new java.awt.BorderLayout(18, 22));
        encabezado = new javax.swing.JPanel();
        encabezado.setLayout(new java.awt.BorderLayout(0, 5));
        titulo = new javax.swing.JLabel();
        titulo.setText("Bienvenido a La Doble");
        titulo.setFont(new java.awt.Font("Serif", 1, 32));
        encabezado.add(titulo, java.awt.BorderLayout.NORTH);
        subtitulo = new javax.swing.JLabel();
        subtitulo.setText("¿Qué necesitas hacer?");
        subtitulo.setFont(new java.awt.Font("SansSerif", 0, 16));
        encabezado.add(subtitulo, java.awt.BorderLayout.SOUTH);
        this.add(encabezado, java.awt.BorderLayout.NORTH);
        contenido = new javax.swing.JPanel();
        contenido.setLayout(new java.awt.BorderLayout(18, 22));
        metricas = new javax.swing.JPanel();
        metricas.setLayout(new java.awt.GridLayout(1, 3, 18, 0));
        tarjetaVentas = new javax.swing.JPanel();
        tarjetaVentas.setLayout(new java.awt.BorderLayout(12, 12));
        lblVentasTitulo = new javax.swing.JLabel();
        lblVentasTitulo.setText("Ventas guardadas");
        lblVentasTitulo.setFont(new java.awt.Font("SansSerif", 0, 14));
        tarjetaVentas.add(lblVentasTitulo, java.awt.BorderLayout.NORTH);
        lblVentas = new javax.swing.JLabel();
        lblVentas.setText("$ 0");
        lblVentas.setFont(new java.awt.Font("Serif", 1, 28));
        tarjetaVentas.add(lblVentas, java.awt.BorderLayout.CENTER);
        metricas.add(tarjetaVentas);
        tarjetaPedidos = new javax.swing.JPanel();
        tarjetaPedidos.setLayout(new java.awt.BorderLayout(12, 12));
        lblPedidosTitulo = new javax.swing.JLabel();
        lblPedidosTitulo.setText("Pedidos cobrados");
        lblPedidosTitulo.setFont(new java.awt.Font("SansSerif", 0, 14));
        tarjetaPedidos.add(lblPedidosTitulo, java.awt.BorderLayout.NORTH);
        lblPedidos = new javax.swing.JLabel();
        lblPedidos.setText("0");
        lblPedidos.setFont(new java.awt.Font("Serif", 1, 28));
        tarjetaPedidos.add(lblPedidos, java.awt.BorderLayout.CENTER);
        metricas.add(tarjetaPedidos);
        tarjetaMesas = new javax.swing.JPanel();
        tarjetaMesas.setLayout(new java.awt.BorderLayout(12, 12));
        lblMesasTitulo = new javax.swing.JLabel();
        lblMesasTitulo.setText("Mesas ocupadas");
        lblMesasTitulo.setFont(new java.awt.Font("SansSerif", 0, 14));
        tarjetaMesas.add(lblMesasTitulo, java.awt.BorderLayout.NORTH);
        lblMesas = new javax.swing.JLabel();
        lblMesas.setText("0 de 10");
        lblMesas.setFont(new java.awt.Font("Serif", 1, 28));
        tarjetaMesas.add(lblMesas, java.awt.BorderLayout.CENTER);
        metricas.add(tarjetaMesas);
        contenido.add(metricas, java.awt.BorderLayout.NORTH);
        zonaInferior = new javax.swing.JPanel();
        zonaInferior.setLayout(new java.awt.GridLayout(1, 2, 22, 0));
        accesos = new javax.swing.JPanel();
        accesos.setLayout(new java.awt.BorderLayout(0, 18));
        lblAcciones = new javax.swing.JLabel();
        lblAcciones.setText("Acciones rápidas");
        lblAcciones.setFont(new java.awt.Font("Serif", 1, 25));
        accesos.add(lblAcciones, java.awt.BorderLayout.NORTH);
        botones = new javax.swing.JPanel();
        botones.setLayout(new java.awt.GridLayout(2, 2, 12, 12));
        btnPedido = new javax.swing.JButton();
        btnPedido.setText("Nuevo pedido");
        botones.add(btnPedido);
        btnMesas = new javax.swing.JButton();
        btnMesas.setText("Ver mesas");
        botones.add(btnMesas);
        btnMenu = new javax.swing.JButton();
        btnMenu.setText("Administrar menú");
        botones.add(btnMenu);
        btnInventario = new javax.swing.JButton();
        btnInventario.setText("Ver inventario");
        botones.add(btnInventario);
        accesos.add(botones, java.awt.BorderLayout.CENTER);
        zonaInferior.add(accesos);
        destacado = new javax.swing.JPanel();
        destacado.setLayout(new java.awt.BorderLayout(0, 12));
        lblEspecialidad = new javax.swing.JLabel();
        lblEspecialidad.setText("Especialidad de la casa");
        lblEspecialidad.setFont(new java.awt.Font("Serif", 1, 24));
        destacado.add(lblEspecialidad, java.awt.BorderLayout.NORTH);
        foto = new javax.swing.JLabel();
        foto.setText("");
        foto.setFont(new java.awt.Font("SansSerif", 0, 14));
        destacado.add(foto, java.awt.BorderLayout.CENTER);
        lblProducto = new javax.swing.JLabel();
        lblProducto.setText("La Doble · $ 28.900");
        lblProducto.setFont(new java.awt.Font("SansSerif", 1, 21));
        destacado.add(lblProducto, java.awt.BorderLayout.SOUTH);
        zonaInferior.add(destacado);
        contenido.add(zonaInferior, java.awt.BorderLayout.CENTER);
        this.add(contenido, java.awt.BorderLayout.CENTER);
        aviso = new javax.swing.JLabel();
        aviso.setText("Los pedidos confirmados se guardan en archivos TXT.");
        aviso.setFont(new java.awt.Font("SansSerif", 0, 13));
        this.add(aviso, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel encabezado;
    private javax.swing.JLabel titulo;
    private javax.swing.JLabel subtitulo;
    private javax.swing.JPanel contenido;
    private javax.swing.JPanel metricas;
    private javax.swing.JPanel tarjetaVentas;
    private javax.swing.JLabel lblVentasTitulo;
    private javax.swing.JLabel lblVentas;
    private javax.swing.JPanel tarjetaPedidos;
    private javax.swing.JLabel lblPedidosTitulo;
    private javax.swing.JLabel lblPedidos;
    private javax.swing.JPanel tarjetaMesas;
    private javax.swing.JLabel lblMesasTitulo;
    private javax.swing.JLabel lblMesas;
    private javax.swing.JPanel zonaInferior;
    private javax.swing.JPanel accesos;
    private javax.swing.JLabel lblAcciones;
    private javax.swing.JPanel botones;
    private javax.swing.JButton btnPedido;
    private javax.swing.JButton btnMesas;
    private javax.swing.JButton btnMenu;
    private javax.swing.JButton btnInventario;
    private javax.swing.JPanel destacado;
    private javax.swing.JLabel lblEspecialidad;
    private javax.swing.JLabel foto;
    private javax.swing.JLabel lblProducto;
    private javax.swing.JLabel aviso;
    // End of variables declaration//GEN-END:variables
}
