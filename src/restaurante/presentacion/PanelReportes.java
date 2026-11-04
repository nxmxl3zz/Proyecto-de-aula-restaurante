package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.table.DefaultTableModel;
import restaurante.entidades.Pedido;

public class PanelReportes extends javax.swing.JPanel {
    private Supplier<List<Pedido>> fuente;
    private DefaultTableModel modelo;
    public PanelReportes() { this(java.util.Collections::emptyList); }
    public PanelReportes(Supplier<List<Pedido>> fuente) {
        this.fuente=fuente; initComponents(); Tema.preparar(this);
        for(JPanel p:new JPanel[]{encabezado,titulos,contenido,metricas}) p.setOpaque(false);
        for(JPanel p:new JPanel[]{ventas,pedidos,unidades}) Tema.tarjeta(p);
        for(JLabel l:new JLabel[]{valorVentas,valorPedidos,valorUnidades}) l.setForeground(Tema.ACENTO);
        modelo=new DefaultTableModel(new Object[]{"Pedido","Fecha","Mesa","Unidades","Total"},0) {
            @Override public boolean isCellEditable(int row,int column) {return false;}
        };
        tabla.setModel(modelo); Tema.tabla(tabla);
        btnActualizar.addActionListener(e -> refrescar()); refrescar();
    }
    public void refrescar() {
        modelo.setRowCount(0); double total=0; int cantidad=0;
        for(Pedido pedido:fuente.get()) {
            int unidades=pedido.getDetalles().stream().mapToInt(d -> d.getCantidad()).sum();
            modelo.addRow(new Object[]{pedido.getNumero(),pedido.getFecha(),pedido.getMesa()==null?"Sin asignar":pedido.getMesa(),unidades,Tema.moneda(pedido.calcularTotal())});
            total+=pedido.calcularTotal(); cantidad+=unidades;
        }
        valorVentas.setText(Tema.moneda(total)); valorPedidos.setText(String.valueOf(modelo.getRowCount())); valorUnidades.setText(String.valueOf(cantidad));
        resumen.setText("Solo cobros confirmados · "+modelo.getRowCount()+" pedidos · "+Tema.moneda(total));
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        this.setLayout(new java.awt.BorderLayout(0, 22));
        encabezado = new javax.swing.JPanel();
        encabezado.setLayout(new java.awt.BorderLayout(12, 12));
        titulos = new javax.swing.JPanel();
        titulos.setLayout(new java.awt.BorderLayout(0, 5));
        titulo = new javax.swing.JLabel();
        titulo.setText("Reportes");
        titulo.setFont(new java.awt.Font("Serif", 1, 32));
        titulos.add(titulo, java.awt.BorderLayout.NORTH);
        subtitulo = new javax.swing.JLabel();
        subtitulo.setText("Historial de pedidos cobrados");
        subtitulo.setFont(new java.awt.Font("SansSerif", 0, 15));
        titulos.add(subtitulo, java.awt.BorderLayout.SOUTH);
        encabezado.add(titulos, java.awt.BorderLayout.WEST);
        btnActualizar = new javax.swing.JButton();
        btnActualizar.setText("Actualizar");
        encabezado.add(btnActualizar, java.awt.BorderLayout.EAST);
        this.add(encabezado, java.awt.BorderLayout.NORTH);
        contenido = new javax.swing.JPanel();
        contenido.setLayout(new java.awt.BorderLayout(0, 20));
        metricas = new javax.swing.JPanel();
        metricas.setLayout(new java.awt.GridLayout(1, 3, 16, 0));
        ventas = new javax.swing.JPanel();
        ventas.setLayout(new java.awt.BorderLayout(12, 12));
        lblVentas = new javax.swing.JLabel();
        lblVentas.setText("Ventas totales");
        lblVentas.setFont(new java.awt.Font("SansSerif", 0, 14));
        ventas.add(lblVentas, java.awt.BorderLayout.NORTH);
        valorVentas = new javax.swing.JLabel();
        valorVentas.setText("$ 0");
        valorVentas.setFont(new java.awt.Font("Serif", 1, 27));
        ventas.add(valorVentas, java.awt.BorderLayout.CENTER);
        metricas.add(ventas);
        pedidos = new javax.swing.JPanel();
        pedidos.setLayout(new java.awt.BorderLayout(12, 12));
        lblPedidos = new javax.swing.JLabel();
        lblPedidos.setText("Pedidos cobrados");
        lblPedidos.setFont(new java.awt.Font("SansSerif", 0, 14));
        pedidos.add(lblPedidos, java.awt.BorderLayout.NORTH);
        valorPedidos = new javax.swing.JLabel();
        valorPedidos.setText("0");
        valorPedidos.setFont(new java.awt.Font("Serif", 1, 27));
        pedidos.add(valorPedidos, java.awt.BorderLayout.CENTER);
        metricas.add(pedidos);
        unidades = new javax.swing.JPanel();
        unidades.setLayout(new java.awt.BorderLayout(12, 12));
        lblUnidades = new javax.swing.JLabel();
        lblUnidades.setText("Unidades vendidas");
        lblUnidades.setFont(new java.awt.Font("SansSerif", 0, 14));
        unidades.add(lblUnidades, java.awt.BorderLayout.NORTH);
        valorUnidades = new javax.swing.JLabel();
        valorUnidades.setText("0");
        valorUnidades.setFont(new java.awt.Font("Serif", 1, 27));
        unidades.add(valorUnidades, java.awt.BorderLayout.CENTER);
        metricas.add(unidades);
        contenido.add(metricas, java.awt.BorderLayout.NORTH);
        scroll = new javax.swing.JScrollPane();
        tabla = new javax.swing.JTable();
        scroll.setViewportView(tabla);
        contenido.add(scroll, java.awt.BorderLayout.CENTER);
        this.add(contenido, java.awt.BorderLayout.CENTER);
        resumen = new javax.swing.JLabel();
        resumen.setText("Solo se incluyen cobros confirmados.");
        resumen.setFont(new java.awt.Font("SansSerif", 0, 13));
        this.add(resumen, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel encabezado;
    private javax.swing.JPanel titulos;
    private javax.swing.JLabel titulo;
    private javax.swing.JLabel subtitulo;
    private javax.swing.JButton btnActualizar;
    private javax.swing.JPanel contenido;
    private javax.swing.JPanel metricas;
    private javax.swing.JPanel ventas;
    private javax.swing.JLabel lblVentas;
    private javax.swing.JLabel valorVentas;
    private javax.swing.JPanel pedidos;
    private javax.swing.JLabel lblPedidos;
    private javax.swing.JLabel valorPedidos;
    private javax.swing.JPanel unidades;
    private javax.swing.JLabel lblUnidades;
    private javax.swing.JLabel valorUnidades;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable tabla;
    private javax.swing.JLabel resumen;
    // End of variables declaration//GEN-END:variables
}
