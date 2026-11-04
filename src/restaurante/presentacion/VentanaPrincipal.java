package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;

public class VentanaPrincipal extends javax.swing.JFrame {
    private final restaurante.seguridad.Sesion usuario;
    private final Runnable alSalir;
    private PanelInicio panelInicio;
    private PanelMenu panelMenu;
    private PanelPedidos panelPedidos;
    private PanelMesas panelMesas;
    private PanelInventario panelInventario;
    private PanelReportes panelReportes;
    private JButton botonSeccionActiva;

    public VentanaPrincipal(restaurante.seguridad.Sesion usuario, restaurante.datos.DatosRestaurante datos, Runnable alSalir) {
        if(usuario==null) throw new IllegalArgumentException("Inicia sesión para continuar.");
        this.usuario=usuario; this.alSalir=alSalir;
        initComponents();
        setTitle("La Doble · Restaurante");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                if(!panelPedidos.tienePedidoPendiente() || JOptionPane.showConfirmDialog(VentanaPrincipal.this,"¿Salir y descartar el carrito sin cobrar?","Salir",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) dispose();
            }
        });
        setMinimumSize(new Dimension(1120, 720)); setSize(1380, 860); setLocationRelativeTo(null);
        panelBarra.setBackground(Tema.LATERAL);
        panelBarra.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,Tema.BORDE),BorderFactory.createEmptyBorder(8,20,8,20)));
        navegacion.setOpaque(false); sesion.setOpaque(false);
        panelPedidos = new PanelPedidos(datos,usuario);
        panelMenu = new PanelMenu(datos,usuario);
        panelInventario = new PanelInventario(datos,usuario);
        panelMesas = new PanelMesas(datos,usuario,mesa -> {
            if (!panelPedidos.asignarMesa(mesa)) return false;
            mostrarPedidos(); return true;
        });
        panelReportes = new PanelReportes(panelPedidos::getPedidosCobrados);
        panelInicio = new PanelInicio();
        panelInicio.configurar(this::mostrarPedidos, () -> mostrarPanel(panelMesas,btnMesas), () -> {if(usuario.esGerente()) mostrarPanel(panelMenu,btnMenu);}, () -> {if(usuario.esGerente()) mostrarPanel(panelInventario,btnInventario);});
        btnMesas.addActionListener(e -> mostrarPanel(panelMesas,btnMesas));
        btnInventario.addActionListener(e -> {if(usuario.esGerente()) mostrarPanel(panelInventario,btnInventario);});
        for(JButton b:new JButton[]{btnInicio,btnPedidos,btnMesas,btnMenu,btnInventario,btnReportes}) {
            b.putClientProperty("JButton.buttonType","borderless"); b.setFocusPainted(false);
        }
        lblRol.setText(usuario.getRol().toString());
        btnMenu.setVisible(usuario.esGerente()); btnInventario.setVisible(usuario.esGerente()); btnReportes.setVisible(usuario.esGerente());
        panelInicio.permitirAdministracion(usuario.esGerente());
        mostrarInicio();
    }
    private void mostrarInicio() {
        panelInicio.refrescar(panelPedidos.getPedidosCobrados(), panelMesas.ocupadas());
        mostrarPanel(panelInicio,btnInicio);
    }
    private void mostrarPedidos() { panelPedidos.refrescarCatalogo(); mostrarPanel(panelPedidos,btnPedidos); }
    private void mostrarPanel(JPanel panel, JButton boton) {
        if(panel==panelInventario)panelInventario.refrescar();
        panelContenido.removeAll(); panelContenido.add(panel,BorderLayout.CENTER);
        if (botonSeccionActiva != null) { botonSeccionActiva.setForeground(Tema.TEXTO); botonSeccionActiva.setBackground(Tema.LATERAL); }
        boton.setForeground(Tema.ACENTO); boton.setBackground(Tema.TARJETA); botonSeccionActiva=boton;
        panelContenido.revalidate(); panelContenido.repaint();
    }
    private void btnInicioActionPerformed(java.awt.event.ActionEvent evt) { mostrarInicio(); }
    private void btnPedidosActionPerformed(java.awt.event.ActionEvent evt) { mostrarPedidos(); }
    private void btnMenuActionPerformed(java.awt.event.ActionEvent evt) { if(usuario.esGerente()) mostrarPanel(panelMenu,btnMenu); }
    private void btnReportesActionPerformed(java.awt.event.ActionEvent evt) { if(usuario.esGerente()) {panelReportes.refrescar(); mostrarPanel(panelReportes,btnReportes);} }
    private void btnCSActionPerformed(java.awt.event.ActionEvent evt) {
        String mensaje=panelPedidos.tienePedidoPendiente()?"¿Cerrar sesión? Se descartará el carrito sin cobrar.":"¿Cerrar sesión y volver al inicio de sesión?";
        if(JOptionPane.showConfirmDialog(this,mensaje,"Cerrar sesión",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {alSalir.run();dispose();}
    }
    public static void main(String[] args) { restaurante.Main.main(args); }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        getContentPane().setLayout(new java.awt.BorderLayout(0, 0));
        panelBarra = new javax.swing.JPanel();
        panelBarra.setLayout(new java.awt.BorderLayout(20, 0));
        marca = new javax.swing.JLabel();
        marca.setText("La Doble");
        marca.setFont(new java.awt.Font("Serif", 1, 27));
        panelBarra.add(marca, java.awt.BorderLayout.WEST);
        navegacion = new javax.swing.JPanel();
        navegacion.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 3, 8));
        btnInicio = new javax.swing.JButton();
        btnInicio.setText("Inicio");
        btnInicio.addActionListener(this::btnInicioActionPerformed);
        navegacion.add(btnInicio);
        btnPedidos = new javax.swing.JButton();
        btnPedidos.setText("Pedidos");
        btnPedidos.addActionListener(this::btnPedidosActionPerformed);
        navegacion.add(btnPedidos);
        btnMesas = new javax.swing.JButton();
        btnMesas.setText("Mesas");
        navegacion.add(btnMesas);
        btnMenu = new javax.swing.JButton();
        btnMenu.setText("Menú");
        btnMenu.addActionListener(this::btnMenuActionPerformed);
        navegacion.add(btnMenu);
        btnInventario = new javax.swing.JButton();
        btnInventario.setText("Inventario");
        navegacion.add(btnInventario);
        btnReportes = new javax.swing.JButton();
        btnReportes.setText("Reportes");
        btnReportes.addActionListener(this::btnReportesActionPerformed);
        navegacion.add(btnReportes);
        panelBarra.add(navegacion, java.awt.BorderLayout.CENTER);
        sesion = new javax.swing.JPanel();
        sesion.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
        lblRol = new javax.swing.JLabel();
        lblRol.setText("Personal");
        lblRol.setFont(new java.awt.Font("SansSerif", 0, 12));
        sesion.add(lblRol);
        btnCS = new javax.swing.JButton();
        btnCS.setText("Salir");
        btnCS.addActionListener(this::btnCSActionPerformed);
        sesion.add(btnCS);
        panelBarra.add(sesion, java.awt.BorderLayout.EAST);
        getContentPane().add(panelBarra, java.awt.BorderLayout.NORTH);
        panelContenido = new javax.swing.JPanel();
        panelContenido.setLayout(new java.awt.BorderLayout(12, 12));
        getContentPane().add(panelContenido, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel panelBarra;
    private javax.swing.JLabel marca;
    private javax.swing.JPanel navegacion;
    private javax.swing.JButton btnInicio;
    private javax.swing.JButton btnPedidos;
    private javax.swing.JButton btnMesas;
    private javax.swing.JButton btnMenu;
    private javax.swing.JButton btnInventario;
    private javax.swing.JButton btnReportes;
    private javax.swing.JPanel sesion;
    private javax.swing.JLabel lblRol;
    private javax.swing.JButton btnCS;
    private javax.swing.JPanel panelContenido;
    // End of variables declaration//GEN-END:variables
}
