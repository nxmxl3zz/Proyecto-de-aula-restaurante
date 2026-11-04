package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntPredicate;

public class PanelMesas extends javax.swing.JPanel {
    private final restaurante.datos.DatosRestaurante datos;
    private final restaurante.seguridad.Sesion usuario;
    private IntPredicate tomarPedido;
    private final boolean[] estados = new boolean[10];
    private int seleccion = -1;
    private JButton[] mesas;
    public PanelMesas() { this(mesa -> true); }
    public PanelMesas(IntPredicate tomarPedido) {this(null,null,tomarPedido);}
    public PanelMesas(restaurante.datos.DatosRestaurante datos,restaurante.seguridad.Sesion usuario,IntPredicate tomarPedido) {
        this.datos=datos;this.usuario=usuario;
        if(datos!=null)System.arraycopy(datos.getMesas(),0,estados,0,10);
        this.tomarPedido=tomarPedido; initComponents(); Tema.preparar(this);
        encabezado.setOpaque(false); panelMesas.setOpaque(false); acciones.setOpaque(false); Tema.tarjeta(detalle);
        detalle.setPreferredSize(new Dimension(225,0));
        mesas=new JButton[]{mesa1,mesa2,mesa3,mesa4,mesa5,mesa6,mesa7,mesa8,mesa9,mesa10};
        for (int i=0;i<mesas.length;i++) {
            final int indice=i; JButton boton=mesas[i];
            boton.setIcon(Imagenes.cargar("mesa",96,96));
            boton.setHorizontalTextPosition(SwingConstants.CENTER); boton.setVerticalTextPosition(SwingConstants.BOTTOM);
            boton.addActionListener(e -> {seleccion=indice; refrescar();});
        }
        btnOcupar.addActionListener(e -> cambiar(true)); btnLiberar.addActionListener(e -> cambiar(false));
        btnPedido.addActionListener(e -> { if(seleccion>=0 && this.tomarPedido.test(seleccion+1)) cambiar(true); });
        Tema.principal(btnPedido); refrescar();
    }
    private void cambiar(boolean ocupado) {
        if(seleccion<0)return;
        try {if(datos!=null)datos.guardarMesa(usuario,seleccion+1,ocupado);estados[seleccion]=ocupado;refrescar();}
        catch(java.io.IOException ex){JOptionPane.showMessageDialog(this,"No se pudo guardar la mesa.\n"+ex.getMessage(),"Archivo de mesas",JOptionPane.ERROR_MESSAGE);}
    }
    private void refrescar() {
        for(int i=0;i<mesas.length;i++) {
            mesas[i].setText("<html><center>Mesa "+(i+1)+"<br>"+(estados[i]?"Ocupada":"Libre")+"</center></html>");
            mesas[i].setBorder(BorderFactory.createLineBorder(i==seleccion?Tema.ACENTO:Tema.BORDE,i==seleccion?2:1));
            mesas[i].setForeground(estados[i]?Tema.ACENTO:Tema.TEXTO);
        }
        lblSeleccion.setText(seleccion<0?"Sin selección":"Mesa "+(seleccion+1));
        btnPedido.setEnabled(seleccion>=0); btnOcupar.setEnabled(seleccion>=0&&!estados[seleccion]); btnLiberar.setEnabled(seleccion>=0&&estados[seleccion]);
    }
    public int ocupadas() { int total=0; for(boolean estado:estados) if(estado) total++; return total; }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        this.setLayout(new java.awt.BorderLayout(22, 20));
        encabezado = new javax.swing.JPanel();
        encabezado.setLayout(new java.awt.BorderLayout(0, 6));
        titulo = new javax.swing.JLabel();
        titulo.setText("Mesas");
        titulo.setFont(new java.awt.Font("Serif", 1, 32));
        encabezado.add(titulo, java.awt.BorderLayout.NORTH);
        subtitulo = new javax.swing.JLabel();
        subtitulo.setText("Selecciona una mesa para tomar un pedido");
        subtitulo.setFont(new java.awt.Font("SansSerif", 0, 15));
        encabezado.add(subtitulo, java.awt.BorderLayout.SOUTH);
        this.add(encabezado, java.awt.BorderLayout.NORTH);
        panelMesas = new javax.swing.JPanel();
        panelMesas.setLayout(new java.awt.GridLayout(2, 5, 12, 12));
        mesa1 = new javax.swing.JButton();
        mesa1.setText("Mesa 1");
        panelMesas.add(mesa1);
        mesa2 = new javax.swing.JButton();
        mesa2.setText("Mesa 2");
        panelMesas.add(mesa2);
        mesa3 = new javax.swing.JButton();
        mesa3.setText("Mesa 3");
        panelMesas.add(mesa3);
        mesa4 = new javax.swing.JButton();
        mesa4.setText("Mesa 4");
        panelMesas.add(mesa4);
        mesa5 = new javax.swing.JButton();
        mesa5.setText("Mesa 5");
        panelMesas.add(mesa5);
        mesa6 = new javax.swing.JButton();
        mesa6.setText("Mesa 6");
        panelMesas.add(mesa6);
        mesa7 = new javax.swing.JButton();
        mesa7.setText("Mesa 7");
        panelMesas.add(mesa7);
        mesa8 = new javax.swing.JButton();
        mesa8.setText("Mesa 8");
        panelMesas.add(mesa8);
        mesa9 = new javax.swing.JButton();
        mesa9.setText("Mesa 9");
        panelMesas.add(mesa9);
        mesa10 = new javax.swing.JButton();
        mesa10.setText("Mesa 10");
        panelMesas.add(mesa10);
        this.add(panelMesas, java.awt.BorderLayout.CENTER);
        detalle = new javax.swing.JPanel();
        detalle.setLayout(new java.awt.BorderLayout(0, 18));
        lblSeleccion = new javax.swing.JLabel();
        lblSeleccion.setText("Selecciona una mesa");
        lblSeleccion.setFont(new java.awt.Font("Serif", 1, 24));
        detalle.add(lblSeleccion, java.awt.BorderLayout.NORTH);
        acciones = new javax.swing.JPanel();
        acciones.setLayout(new java.awt.GridLayout(3, 1, 0, 12));
        btnPedido = new javax.swing.JButton();
        btnPedido.setText("Tomar pedido");
        acciones.add(btnPedido);
        btnOcupar = new javax.swing.JButton();
        btnOcupar.setText("Ocupar mesa");
        acciones.add(btnOcupar);
        btnLiberar = new javax.swing.JButton();
        btnLiberar.setText("Liberar mesa");
        acciones.add(btnLiberar);
        zonaAcciones = new javax.swing.JPanel(new java.awt.BorderLayout());
        zonaAcciones.setOpaque(false);
        zonaAcciones.add(acciones,java.awt.BorderLayout.NORTH);
        detalle.add(zonaAcciones, java.awt.BorderLayout.CENTER);
        nota = new javax.swing.JLabel();
        nota.setText("Liberación manual");
        nota.setFont(new java.awt.Font("SansSerif", 0, 12));
        detalle.add(nota, java.awt.BorderLayout.SOUTH);
        this.add(detalle, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel encabezado;
    private javax.swing.JLabel titulo;
    private javax.swing.JLabel subtitulo;
    private javax.swing.JPanel panelMesas;
    private javax.swing.JButton mesa1;
    private javax.swing.JButton mesa2;
    private javax.swing.JButton mesa3;
    private javax.swing.JButton mesa4;
    private javax.swing.JButton mesa5;
    private javax.swing.JButton mesa6;
    private javax.swing.JButton mesa7;
    private javax.swing.JButton mesa8;
    private javax.swing.JButton mesa9;
    private javax.swing.JButton mesa10;
    private javax.swing.JPanel detalle;
    private javax.swing.JLabel lblSeleccion;
    private javax.swing.JPanel acciones;
    private javax.swing.JPanel zonaAcciones;
    private javax.swing.JButton btnPedido;
    private javax.swing.JButton btnOcupar;
    private javax.swing.JButton btnLiberar;
    private javax.swing.JLabel nota;
    // End of variables declaration//GEN-END:variables
}
