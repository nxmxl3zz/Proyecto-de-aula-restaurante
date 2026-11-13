package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntPredicate;

/** Vista de mesas, capacidad y agrupación para atender reuniones con un solo pedido. */
public final class PanelMesas extends javax.swing.JPanel {
    private final restaurante.controlador.ControladorRestaurante controlador;
    private IntPredicate tomarPedido;
    private final boolean[] estados = new boolean[10];
    private int seleccion = -1;
    private JButton[] mesas;
    public PanelMesas() {
        this(mesa -> true);
    }
    public PanelMesas(IntPredicate tomarPedido) {
        this(null, tomarPedido);
    }
    public PanelMesas(
            restaurante.controlador.ControladorRestaurante controlador, IntPredicate tomarPedido) {
        this.controlador = controlador;
        if (controlador != null)
            System.arraycopy(controlador.getMesas(), 0, estados, 0, 10);
        this.tomarPedido = tomarPedido;
        initComponents();
        Tema.preparar(this);
        encabezado.setOpaque(false);
        panelMesas.setOpaque(false);
        acciones.setOpaque(false);
        Tema.tarjeta(detalle);
        detalle.setPreferredSize(new Dimension(240, 0));
        panelMesas.setLayout(new GridBagLayout());
        JScrollPane plano = new JScrollPane(panelMesas);
        plano.setBorder(BorderFactory.createEmptyBorder());
        plano.getVerticalScrollBar().setUnitIncrement(24);
        plano.getViewport().setBackground(Tema.FONDO);
        add(plano, BorderLayout.CENTER);
        subtitulo.setText(
                "Libre / ocupada: estado · Borde claro: selección · Recuadro verde: mesas unidas");

        nota.setPreferredSize(new Dimension(210, 80));
        acciones.setLayout(new GridLayout(0, 1, 0, 8));
        acciones.setPreferredSize(new Dimension(0, 268));
        mesas = new JButton[] {
                mesa1, mesa2, mesa3, mesa4, mesa5, mesa6, mesa7, mesa8, mesa9, mesa10};
        for (int i = 0; i < mesas.length; i++) {
            final int indice = i;
            JButton boton = mesas[i];
            boton.setHorizontalTextPosition(SwingConstants.CENTER);
            boton.setVerticalTextPosition(SwingConstants.BOTTOM);
            boton.addActionListener(e -> {
                seleccion = indice;
                refrescar();
            });
        }
        btnOcupar.addActionListener(e -> cambiar(true));
        btnLiberar.addActionListener(e -> cambiar(false));
        btnPedido.addActionListener(e -> {
            if (seleccion >= 0 && this.tomarPedido.test(seleccion + 1))
                cambiar(true);
        });
        btnUnir.addActionListener(e -> elegirMesas());
        btnSeparar.addActionListener(e -> {
            if (seleccion < 0 || controlador == null)
                return;
            try {
                controlador.separarMesas(seleccion + 1);
                refrescar();
            } catch (java.io.IOException | SecurityException ex) {
                error(ex.getMessage());
            }
        });
        btnSillas.addActionListener(e -> configurarSillas());
        btnSillas.setVisible(controlador != null && controlador.esGerente());
        Tema.principal(btnPedido);
        refrescar();
    }
    private void cambiar(boolean ocupado) {
        if (seleccion < 0)
            return;
        try {
            if (controlador != null)
                controlador.guardarMesa(seleccion + 1, ocupado);
            estados[seleccion] = ocupado;
            refrescar();
        } catch (java.io.IOException | SecurityException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la mesa.\n" + ex.getMessage(),
                    "Archivo de mesas", JOptionPane.ERROR_MESSAGE);
        }
    }
    public void refrescar() {
        java.util.List<restaurante.entidades.Mesa> configuradas =
                controlador == null ? new java.util.ArrayList<>() : controlador.getDetalleMesas();
        if (controlador != null)
            System.arraycopy(controlador.getMesas(), 0, estados, 0, 10);
        else
            for (int i = 0; i < 10; i++)
                configuradas.add(new restaurante.entidades.Mesa(i + 1,
                        restaurante.entidades.Mesa.sillasIniciales(i + 1), estados[i], i + 1));
        for (int i = 0; i < mesas.length; i++) {
            restaurante.entidades.Mesa mesa = configuradas.get(i);
            String grupo = etiquetaGrupo(configuradas, mesa.getGrupo());
            mesas[i].setIcon(new IconoMesa(mesa.getSillas(), estados[i]));
            boolean abierto =
                    controlador != null && controlador.getPedidoAbiertoPorMesa(i + 1) != null;
            mesas[i].setText("<html><center><b>Mesa " + (i + 1) + "</b> · " + mesa.getSillas()
                    + " sillas<br><font color='" + (estados[i] ? "#E9AF52" : "#9BC8A5") + "'>"
                    + (abierto ? "CON PEDIDO"
                                    : estados[i] ? "OCUPADA"
                                                 : "LIBRE")
                    + "</font>" + (grupo.contains("+") ? "<br>Unida: " + grupo : "")
                    + "</center></html>");
            mesas[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                            i == seleccion ? Tema.TEXTO : Tema.BORDE, i == seleccion ? 3 : 1),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            mesas[i].setForeground(Tema.TEXTO);
            mesas[i].setPreferredSize(new Dimension(170, 160));
            mesas[i].getAccessibleContext().setAccessibleName("Mesa " + (i + 1) + ", "
                    + mesa.getSillas() + " sillas, " + (estados[i] ? "ocupada" : "libre"));
        }
        organizarGrupos(configuradas);
        lblSeleccion.setText(seleccion < 0 ? "Sin selección" : "Mesa " + (seleccion + 1));
        boolean abierto = seleccion >= 0 && controlador != null
                && controlador.getPedidoAbiertoPorMesa(seleccion + 1) != null;
        btnPedido.setText(abierto ? "Editar pedido abierto" : "Tomar pedido");
        btnPedido.setEnabled(seleccion >= 0);
        btnOcupar.setEnabled(seleccion >= 0 && !estados[seleccion]);
        btnLiberar.setEnabled(seleccion >= 0 && estados[seleccion] && !abierto);
        btnUnir.setEnabled(seleccion >= 0 && controlador != null && !estados[seleccion]);
        btnSillas.setEnabled(seleccion >= 0 && controlador != null && controlador.esGerente()
                && !estados[seleccion]);
        btnSeparar.setEnabled(seleccion >= 0 && !estados[seleccion]
                && etiquetaGrupo(configuradas, configuradas.get(seleccion).getGrupo())
                        .contains("+"));
        if (seleccion >= 0) {
            int grupo = configuradas.get(seleccion).getGrupo();
            int capacidad = 0;
            for (restaurante.entidades.Mesa mesa : configuradas)
                if (mesa.getGrupo() == grupo)
                    capacidad += mesa.getSillas();
            nota.setText("<html>Mesas: " + etiquetaGrupo(configuradas, grupo)
                    + "<br>Capacidad total: " + capacidad + " sillas<br>"
                    + (abierto ? "Pedido guardado; abre para editar."
                               : "Une las mesas antes de ocuparlas.")
                    + "</html>");
        } else
            nota.setText("<html>Selecciona una mesa para ver su capacidad.</html>");
    }
    private void organizarGrupos(java.util.List<restaurante.entidades.Mesa> configuradas) {
        panelMesas.removeAll();
        java.util.Set<Integer> dibujados = new java.util.HashSet<>();
        int fila = 0, columna = 0;
        java.util.List<restaurante.entidades.Mesa> ordenadas =
                new java.util.ArrayList<>(configuradas);
        ordenadas.sort(java.util.Comparator.comparingInt(
                mesa -> etiquetaGrupo(configuradas, mesa.getGrupo()).contains("+") ? 0 : 1));
        for (restaurante.entidades.Mesa mesa : ordenadas) {
            if (!dibujados.add(mesa.getGrupo()))
                continue;
            java.util.List<restaurante.entidades.Mesa> grupo = new java.util.ArrayList<>();
            for (restaurante.entidades.Mesa otra : configuradas)
                if (otra.getGrupo() == mesa.getGrupo())
                    grupo.add(otra);
            boolean unido = grupo.size() > 1;
            if (unido && columna > 0) {
                fila++;
                columna = 0;
            }
            Component tarjeta;
            if (unido) {
                JPanel reunion = new JPanel(new GridLayout(0, Math.min(3, grupo.size()), 10, 10));
                reunion.setOpaque(false);
                int capacidad =
                        grupo.stream().mapToInt(restaurante.entidades.Mesa::getSillas).sum();
                reunion.setBorder(
                        BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Tema.EXITO),
                                "Reunión · Mesas " + etiquetaGrupo(configuradas, mesa.getGrupo())
                                        + " · " + capacidad + " sillas",
                                0, 0, new Font("SansSerif", Font.BOLD, 14), Tema.EXITO));
                for (restaurante.entidades.Mesa miembro : grupo)
                    reunion.add(mesas[miembro.getNumero() - 1]);
                tarjeta = reunion;
            } else
                tarjeta = mesas[mesa.getNumero() - 1];
            GridBagConstraints posicion = new GridBagConstraints();
            posicion.gridx = columna;
            posicion.gridy = fila;
            posicion.gridwidth = unido ? 3 : 1;
            posicion.weightx = 1;
            posicion.fill = GridBagConstraints.HORIZONTAL;
            posicion.anchor = GridBagConstraints.NORTH;
            posicion.insets = new Insets(0, 0, 10, 10);
            panelMesas.add(tarjeta, posicion);
            if (unido || ++columna == 3) {
                fila++;
                columna = 0;
            }
        }
        GridBagConstraints espacio = new GridBagConstraints();
        espacio.gridx = 0;
        espacio.gridy = fila + 1;
        espacio.weighty = 1;
        panelMesas.add(Box.createVerticalGlue(), espacio);
        panelMesas.revalidate();
        panelMesas.repaint();
    }
    private String etiquetaGrupo(java.util.List<restaurante.entidades.Mesa> lista, int grupo) {
        java.util.List<String> numeros = new java.util.ArrayList<>();
        for (restaurante.entidades.Mesa mesa : lista)
            if (mesa.getGrupo() == grupo)
                numeros.add(String.valueOf(mesa.getNumero()));
        return String.join(" + ", numeros);
    }
    private void elegirMesas() {
        if (seleccion < 0 || controlador == null)
            return;
        JPanel formulario = new JPanel(new GridLayout(0, 1, 4, 4));
        java.util.List<JCheckBox> opciones = new java.util.ArrayList<>();
        for (restaurante.entidades.Mesa mesa : controlador.getDetalleMesas()) {
            JCheckBox opcion =
                    new JCheckBox("Mesa " + mesa.getNumero() + " · " + mesa.getSillas() + " sillas",
                            mesa.getNumero() == seleccion + 1);
            opcion.setEnabled(!mesa.estaOcupada());
            formulario.add(opcion);
            opciones.add(opcion);
        }
        if (JOptionPane.showConfirmDialog(this, formulario, "Unir mesas libres",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION)
            return;
        java.util.List<Integer> numeros = new java.util.ArrayList<>();
        for (int i = 0; i < opciones.size(); i++)
            if (opciones.get(i).isSelected())
                numeros.add(i + 1);
        try {
            controlador.unirMesas(numeros);
            refrescar();
        } catch (java.io.IOException | SecurityException ex) {
            error(ex.getMessage());
        }
    }
    private void configurarSillas() {
        if (seleccion < 0 || controlador == null)
            return;
        JSpinner cantidad = new JSpinner(new SpinnerNumberModel(
                controlador.getDetalleMesas().get(seleccion).getSillas(), 1, 20, 1));
        if (JOptionPane.showConfirmDialog(this, cantidad, "Sillas de la mesa " + (seleccion + 1),
                    JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION)
            return;
        try {
            cantidad.commitEdit();
            controlador.configurarSillas(seleccion + 1, ((Number) cantidad.getValue()).intValue());
            refrescar();
        } catch (java.io.IOException | SecurityException | java.text.ParseException ex) {
            error(ex.getMessage());
        }
    }
    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Mesas", JOptionPane.WARNING_MESSAGE);
    }
    public int ocupadas() {
        if (controlador != null)
            System.arraycopy(controlador.getMesas(), 0, estados, 0, 10);
        int total = 0;
        for (boolean estado : estados)
            if (estado)
                total++;
        return total;
    }

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
        subtitulo.setText("Capacidad, atención y unión de mesas para reuniones");
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
        acciones.setLayout(new java.awt.GridLayout(0, 1, 0, 12));
        btnPedido = new javax.swing.JButton();
        btnPedido.setText("Tomar pedido");
        acciones.add(btnPedido);
        btnOcupar = new javax.swing.JButton();
        btnOcupar.setText("Ocupar mesa");
        acciones.add(btnOcupar);
        btnLiberar = new javax.swing.JButton();
        btnLiberar.setText("Liberar mesa");
        acciones.add(btnLiberar);
        btnUnir = new javax.swing.JButton("Unir mesas"); acciones.add(btnUnir);
        btnSeparar = new javax.swing.JButton("Separar mesas"); acciones.add(btnSeparar);
        btnSillas = new javax.swing.JButton("Configurar sillas"); acciones.add(btnSillas);
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
    private javax.swing.JButton btnUnir;
    private javax.swing.JButton btnSeparar;
    private javax.swing.JButton btnSillas;
    private javax.swing.JLabel nota;
    // End of variables declaration//GEN-END:variables
}
