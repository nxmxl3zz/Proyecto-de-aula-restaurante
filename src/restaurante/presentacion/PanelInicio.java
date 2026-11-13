package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import restaurante.entidades.Pedido;

/** Resumen y accesos a las funciones habilitadas para cada rol. */
public final class PanelInicio extends javax.swing.JPanel {
    private final ListaVertical atenciones = new ListaVertical();
    private java.util.function.IntConsumer continuar;
    private boolean compacto;
    private java.util.function.IntConsumer avanzar;

    /** Acción para pasar un pedido abierto al siguiente estado de cocina. */
    public void alAvanzar(java.util.function.IntConsumer accion) {
        avanzar = accion;
    }

    public void alContinuar(java.util.function.IntConsumer accion) {
        continuar = accion;
    }
    public PanelInicio() {
        initComponents();
        Tema.preparar(this);
        titulo.setIcon(new Logotipo(48));
        titulo.setIconTextGap(14);
        encabezado.setOpaque(false);
        contenido.setOpaque(false);
        metricas.setOpaque(false);
        zonaInferior.setOpaque(false);
        botones.setOpaque(false);
        for (JPanel p :
                new JPanel[] {tarjetaVentas, tarjetaPedidos, tarjetaMesas, accesos, destacado})
            Tema.tarjeta(p);
        for (JLabel l : new JLabel[] {lblVentas, lblPedidos, lblMesas})
            l.setForeground(Tema.ACENTO);
        zonaInferior.removeAll();
        zonaInferior.setLayout(new BorderLayout(0, 18));
        botones.setLayout(new GridLayout(1, 0, 10, 0));
        botones.setPreferredSize(new Dimension(0, 44));
        zonaInferior.add(accesos, BorderLayout.NORTH);
        zonaInferior.add(destacado, BorderLayout.CENTER);
        listaAtenciones.setViewportView(atenciones);
        listaAtenciones.setBorder(BorderFactory.createEmptyBorder());
        listaAtenciones.getViewport().setBackground(Tema.TARJETA);
        lblVentasTitulo.setText("Ventas de hoy");
        lblPedidosTitulo.setText("Pedidos cobrados hoy");
        lblMesasTitulo.setText("Mesas disponibles ahora");
        lblEspecialidad.setText("Pedidos abiertos");
        btnPedido.setText("Tomar / continuar pedido");
        subtitulo.setText("La operación de hoy, a un clic");
        aviso.setText("Atajos: Alt+1 Inicio · Alt+2 Pedidos · Alt+3 Mesas · Tab para recorrer las "
                      + "acciones");

        Tema.principal(btnPedido);
    }
    @Override
    public void doLayout() {
        boolean reducir = getHeight() > 0 && getHeight() < 680;
        if (compacto != reducir) {
            compacto = reducir;
            lblAcciones.setVisible(!compacto);
            accesos.setBorder(
                    BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.BORDE),
                            BorderFactory.createEmptyBorder(
                                    compacto ? 10 : 16, 16, compacto ? 10 : 16, 16)));
            revalidate();
        }
        super.doLayout();
    }
    public void configurar(Runnable pedido, Runnable mesas, Runnable menu, Runnable inventario) {
        btnPedido.addActionListener(e -> pedido.run());
        btnMesas.addActionListener(e -> mesas.run());
        btnMenu.addActionListener(e -> menu.run());
        btnInventario.addActionListener(e -> inventario.run());
    }
    public void refrescar(List<Pedido> pedidos, int mesas) {
        refrescar(pedidos, mesas, java.util.Collections.emptyList());
    }
    public void refrescar(List<Pedido> pedidos, int mesas, List<Pedido> abiertos) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        lblVentas.setText(Tema.moneda(pedidos.stream()
                        .filter(p -> hoy.equals(p.getFecha()))
                        .mapToDouble(Pedido::calcularTotal)
                        .sum()));
        lblPedidos.setText(
                String.valueOf(pedidos.stream().filter(p -> hoy.equals(p.getFecha())).count()));
        lblMesas.setText((10 - mesas) + " de 10");
        lblProducto.setText(abiertos.size() + " pedidos pendientes de cobro");
        lblProducto.setFont(new Font("SansSerif", Font.PLAIN, 14));
        atenciones.removeAll();
        if (abiertos.isEmpty()) {
            JLabel vacio = new JLabel(
                    "No hay pedidos abiertos. Selecciona una mesa para comenzar la atención.");
            vacio.setForeground(Tema.TEXTO_SEC);
            vacio.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));
            atenciones.add(vacio);
        }
        for (Pedido pedido : abiertos) {
            JPanel fila = new JPanel(new BorderLayout(12, 0));
            fila.setOpaque(false);
            fila.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                    BorderFactory.createEmptyBorder(12, 0, 12, 0)));
            fila.add(new JLabel("#" + pedido.getNumero() + " · " + etiqueta(pedido) + " · " + pedido.getEtiquetaMesas() + " · "
                             + pedido.getDetalles().stream().mapToInt(d -> d.getCantidad()).sum()
                             + " unidades · " + Tema.moneda(pedido.calcularTotal())),
                    BorderLayout.CENTER);
            JButton abrir = new JButton("Continuar pedido");
            abrir.setEnabled(continuar != null);
            abrir.addActionListener(e -> continuar.accept(pedido.getNumero()));
            JPanel acciones = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
            acciones.setOpaque(false);
            String siguiente = accionSiguiente(pedido);
            if (siguiente != null) {
                JButton avance = new JButton(siguiente);
                avance.setEnabled(avanzar != null);
                avance.addActionListener(e -> avanzar.accept(pedido.getNumero()));
                acciones.add(avance);
            }
            acciones.add(abrir);
            fila.add(acciones, BorderLayout.EAST);
            fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
            fila.setAlignmentX(Component.LEFT_ALIGNMENT);
            atenciones.add(fila);
        }
        atenciones.revalidate();
        atenciones.repaint();
    }
    private static String etiqueta(Pedido pedido) {
        switch (pedido.getEstado()) {
            case EN_PREPARACION:
                return "En cocina";
            case SERVIDO:
                return "Servido";
            default:
                return "Pendiente";
        }
    }
    private static String accionSiguiente(Pedido pedido) {
        switch (pedido.getEstado()) {
            case PENDIENTE:
                return "A cocina";
            case EN_PREPARACION:
                return "Marcar servido";
            default:
                return null;
        }
    }
    public void permitirAdministracion(boolean permitido) {
        btnMenu.setVisible(permitido);
        btnInventario.setVisible(permitido);
        tarjetaVentas.setVisible(permitido);
        tarjetaPedidos.setVisible(permitido);
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
        listaAtenciones = new javax.swing.JScrollPane();
        destacado.add(listaAtenciones, java.awt.BorderLayout.CENTER);
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
    private javax.swing.JScrollPane listaAtenciones;
    private javax.swing.JLabel lblProducto;
    private javax.swing.JLabel aviso;
    // End of variables declaration//GEN-END:variables
}
