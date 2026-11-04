package restaurante.presentacion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import restaurante.entidades.DetallePedido;
import restaurante.entidades.Pedido;
import restaurante.entidades.Plato;
import restaurante.datos.CatalogoMenu;

/** Punto de venta editable visualmente desde PanelPedidos.form. */
public class PanelPedidos extends javax.swing.JPanel {
    private final DecimalFormat moneda = new DecimalFormat("$ #,##0", new DecimalFormatSymbols(new Locale("es", "CO")));
    private final List<DetallePedido> carrito = new ArrayList<>();
    private final restaurante.datos.DatosRestaurante datos;
    private final restaurante.seguridad.Sesion usuario;
    private DefaultTableModel modeloCarrito;
    private String categoriaActual = "Hamburguesas";
    private Integer mesaActual;

    /** Cambiar de mesa no debe trasladar silenciosamente un carrito pendiente. */
    public boolean asignarMesa(int mesa) {
        if (!carrito.isEmpty() && (mesaActual == null || mesaActual != mesa)) {
            JOptionPane.showMessageDialog(this, "Cobra o vacía el pedido actual antes de cambiar de mesa.",
                    "Pedido pendiente", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        mesaActual = mesa;
        lblMesa.setText("Mesa " + mesa);
        selectorMesa.setSelectedIndex(mesa);
        return true;
    }

    public List<Pedido> getPedidosCobrados() {
        return datos==null?java.util.Collections.emptyList():datos.getPedidos();
    }

    public PanelPedidos() {this(null,null);}
    public PanelPedidos(restaurante.datos.DatosRestaurante datos,restaurante.seguridad.Sesion usuario) {
        this.datos=datos;this.usuario=usuario;
        initComponents();

        modeloCarrito = new DefaultTableModel(new String[]{"Producto", "Cant.", "Subtotal"},0) {
            @Override public boolean isCellEditable(int row,int column) {return false;}
        };
        tablaCarrito.setModel(modeloCarrito);
        tablaCarrito.getSelectionModel().addListSelectionListener(e -> {
            boolean seleccionado=tablaCarrito.getSelectedRow()>=0;
            btnMas.setEnabled(seleccionado);btnMenos.setEnabled(seleccionado);btnQuitar.setEnabled(seleccionado);
        });
        campoBusqueda.putClientProperty("JTextField.placeholderText","Buscar producto…");
        campoBusqueda.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refrescarCatalogo(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refrescarCatalogo(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refrescarCatalogo(); }
        });
        for(int i=0;i<=10;i++) selectorMesa.addItem(i==0?"Sin mesa":"Mesa "+i);
        selectorMesa.addActionListener(e -> {
            Integer elegida=selectorMesa.getSelectedIndex()==0?null:selectorMesa.getSelectedIndex();
            if(java.util.Objects.equals(elegida,mesaActual)) return;
            if(!carrito.isEmpty()) {
                JOptionPane.showMessageDialog(this,"Cobra o vacía el pedido antes de cambiar de mesa.");
                selectorMesa.setSelectedIndex(mesaActual==null?0:mesaActual); return;
            }
            mesaActual=elegida; lblMesa.setText(elegida==null?"Sin mesa":"Mesa "+elegida);
        });
        btnMas.addActionListener(e -> cambiarCantidad(1)); btnMenos.addActionListener(e -> cambiarCantidad(-1));
        btnVaciar.addActionListener(e -> {
            if(!carrito.isEmpty() && JOptionPane.showConfirmDialog(this,"¿Vaciar el pedido actual?","Vaciar pedido",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                carrito.clear(); actualizarCarrito();
            }
        });
        scrollProductos.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {ajustarCatalogo();}
        });
        aplicarEstilo();
        mostrarCategoria("Hamburguesas", btnHamburguesas);
        actualizarCarrito();
    }

    private void aplicarEstilo() {
        setOpaque(true);
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        Tema.preparar(this);
        contenedorProductos.setOpaque(false);
        encabezado.setOpaque(false); titulos.setOpaque(false); zonaMesa.setOpaque(false); ajustes.setOpaque(false); controlesCantidad.setOpaque(false);
        Tema.principal(btnCobrar);
        panelContenido.setOpaque(false);
        panelZonaMenu.setOpaque(false);
        panelCategorias.setOpaque(false);
        panelProductos.setOpaque(false);
        panelZonaLineas.setOpaque(false);
        panelResumen.setOpaque(false);
        lineaTotal.setOpaque(false);
        panelAcciones.setOpaque(false);
        panelPedido.setOpaque(true);
        panelPedido.setBackground(Tema.TARJETA);
        panelPedido.setPreferredSize(new Dimension(350, 0));
        panelPedido.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE.brighter(), 2),
                BorderFactory.createEmptyBorder(16, 16, 14, 16)));

        lblPedidoActual.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblPedidoActual.setForeground(Tema.TEXTO);
        lblMesa.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lblMesa.setForeground(Tema.TEXTO_SEC);
        lblTotalTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTotalTitulo.setForeground(Tema.TEXTO);
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTotal.setForeground(Tema.TEXTO);
        lblVacio.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblVacio.setForeground(Tema.TEXTO_SEC);
        lblVacio.setHorizontalAlignment(SwingConstants.CENTER);

        for (JButton boton : new JButton[]{btnHamburguesas, btnCombos, btnBebidas, btnExtras}) {
            boton.setFont(new Font("SansSerif", Font.PLAIN, 14));
            boton.setFocusPainted(false);
            boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boton.putClientProperty("JButton.buttonType", "roundRect");
            boton.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        }
        estilizarBoton(btnQuitar, Tema.TARJETA, Tema.TEXTO, false);
        btnQuitar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE.brighter(), 2), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        estilizarBoton(btnCobrar, Tema.ACENTO, Tema.FONDO, true);

        tablaCarrito.setBackground(Tema.TARJETA);
        tablaCarrito.setForeground(Tema.TEXTO);
        tablaCarrito.setSelectionBackground(new Color(0x55432B));
        tablaCarrito.setSelectionForeground(Tema.TEXTO);
        tablaCarrito.setGridColor(Tema.BORDE);
        tablaCarrito.setRowHeight(38);
        tablaCarrito.setShowVerticalLines(false);
        tablaCarrito.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCarrito.getTableHeader().setBackground(Tema.TARJETA);
        tablaCarrito.getTableHeader().setForeground(Tema.TEXTO_SEC);
        tablaCarrito.getTableHeader().setFont(new Font("SansSerif", Font.PLAIN, 13));
        tablaCarrito.getTableHeader().setReorderingAllowed(false);
        tablaCarrito.getColumnModel().getColumn(0).setPreferredWidth(120);
        tablaCarrito.getColumnModel().getColumn(1).setPreferredWidth(42);
        tablaCarrito.getColumnModel().getColumn(2).setPreferredWidth(100);
        scrollCarrito.setBorder(BorderFactory.createEmptyBorder());
        scrollCarrito.getViewport().setBackground(Tema.TARJETA);
        scrollProductos.setBorder(BorderFactory.createEmptyBorder());
        scrollProductos.getViewport().setOpaque(false);
        scrollProductos.setOpaque(false);
        scrollProductos.getVerticalScrollBar().setUnitIncrement(16);
    }

    private void estilizarBoton(JButton boton, Color fondo, Color texto, boolean principal) {
        boton.setFont(new Font("SansSerif", principal ? Font.BOLD : Font.PLAIN, 14));
        boton.setForeground(texto);
        boton.setBackground(fondo);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void mostrarCategoria(String categoria, JButton botonActivo) {
        categoriaActual = categoria;
        for (JButton boton : new JButton[]{btnHamburguesas, btnCombos, btnBebidas, btnExtras}) {
            boolean activo = boton == botonActivo;
            boton.setBackground(activo ? Tema.ACENTO : Tema.TARJETA);
            boton.setForeground(activo ? Tema.FONDO : Tema.TEXTO_SEC);
            boton.setBorder(BorderFactory.createLineBorder(activo ? Tema.ACENTO : Tema.BORDE, activo ? 2 : 1));
        }
        panelProductos.removeAll();
        for (Plato plato : CatalogoMenu.porCategoria(categoria)) {
            String busqueda=campoBusqueda.getText().trim().toLowerCase(Locale.ROOT);
            if(!(plato.getNombre()+" "+plato.getCodigo()).toLowerCase(Locale.ROOT).contains(busqueda)) continue;
            TarjetaProducto tarjeta=new TarjetaProducto(plato, () -> agregarProducto(plato));
            if(datos!=null)tarjeta.configurarReceta(datos.getReceta(plato.getCodigo()));
            panelProductos.add(tarjeta);
        }
        ajustarCatalogo();
        panelProductos.revalidate();
        panelProductos.repaint();
    }

    private void ajustarCatalogo() {
        int ancho=scrollProductos.getViewport().getWidth(); if(ancho<=0)ancho=740;
        int columnas=ancho>=710?3:ancho>=450?2:1;
        ((java.awt.GridLayout)panelProductos.getLayout()).setColumns(columnas);
        int filas=(panelProductos.getComponentCount()+columnas-1)/columnas;
        panelProductos.setPreferredSize(new Dimension(ancho,Math.max(0,filas*TarjetaProducto.ALTO+Math.max(0,filas-1)*12)));
        panelProductos.revalidate();
    }

    /** Recarga los platos compartidos después de volver desde la pantalla Menú. */
    public void refrescarCatalogo() {
        JButton boton = btnHamburguesas;
        if ("Combos".equals(categoriaActual)) boton = btnCombos;
        else if ("Bebidas".equals(categoriaActual)) boton = btnBebidas;
        else if ("Extras".equals(categoriaActual)) boton = btnExtras;
        mostrarCategoria(categoriaActual, boton);
    }

    private void agregarProducto(Plato plato) {
        for (DetallePedido linea : carrito) {
            if (linea.getPlato().getCodigo().equals(plato.getCodigo())) {
                linea.setCantidad(linea.getCantidad() + 1);
                actualizarCarrito();
                return;
            }
        }
        carrito.add(new DetallePedido(plato, 1, plato.getPrecio()));
        actualizarCarrito();
    }

    private void actualizarCarrito() {
        modeloCarrito.setRowCount(0);
        double total = 0;
        for (DetallePedido detalle : carrito) {
            modeloCarrito.addRow(new Object[]{detalle.getPlato().getNombre(), detalle.getCantidad(), moneda.format(detalle.calcularSubtotal())});
            total += detalle.calcularSubtotal();
        }
        lblTotal.setText(moneda.format(total));
        lblVacio.setText(carrito.isEmpty()?"Elige productos para comenzar":"Selecciona una línea para ajustar su cantidad");
        btnCobrar.setText("Cobrar "+moneda.format(total));
        boolean seleccionado=tablaCarrito.getSelectedRow()>=0;
        btnMas.setEnabled(seleccionado);btnMenos.setEnabled(seleccionado);btnQuitar.setEnabled(seleccionado);
    }

    private void cambiarCantidad(int cambio) {
        int fila=tablaCarrito.getSelectedRow(); if(fila<0) return;
        DetallePedido detalle=carrito.get(fila); int cantidad=detalle.getCantidad()+cambio;
        if(cantidad<=0) carrito.remove(fila); else detalle.setCantidad(cantidad);
        actualizarCarrito();
        if(fila<carrito.size()) tablaCarrito.setRowSelectionInterval(fila,fila);
    }
    public boolean tienePedidoPendiente() { return !carrito.isEmpty(); }

    private void quitarSeleccion() {
        int fila = tablaCarrito.getSelectedRow();
        if (fila < 0) {
            if (!carrito.isEmpty()) JOptionPane.showMessageDialog(this, "Selecciona un producto del pedido para quitarlo.", "Pedido actual", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        carrito.remove(fila);
        actualizarCarrito();
    }

    private void cobrarPedido() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un producto antes de cobrar.", "Pedido vacío", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        double total = carrito.stream().mapToDouble(DetallePedido::calcularSubtotal).sum();
        int respuesta = JOptionPane.showConfirmDialog(this, "¿Cobrar " + moneda.format(total) + " y registrar el pedido?",
                "Confirmar cobro", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) return;
        if(datos==null) {JOptionPane.showMessageDialog(this,"Abre la aplicación desde el inicio de sesión.");return;}
        Pedido pedido;
        try {pedido=datos.registrarPedido(usuario,mesaActual,carrito);}
        catch(java.io.IOException ex) {
            JOptionPane.showMessageDialog(this,"No se pudo guardar el pedido. El carrito sigue disponible.\n"+ex.getMessage(),"No se registró el cobro",JOptionPane.ERROR_MESSAGE);return;
        }
        carrito.clear();
        actualizarCarrito();
        JOptionPane.showMessageDialog(this, "Pedido #" + pedido.getNumero() + " cobrado correctamente. Insumos descontados.", "Cobro completado", JOptionPane.INFORMATION_MESSAGE);
    }

    private void btnHamburguesasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHamburguesasActionPerformed
        mostrarCategoria("Hamburguesas", btnHamburguesas);
    }//GEN-LAST:event_btnHamburguesasActionPerformed

    private void btnCombosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCombosActionPerformed
        mostrarCategoria("Combos", btnCombos);
    }//GEN-LAST:event_btnCombosActionPerformed

    private void btnBebidasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBebidasActionPerformed
        mostrarCategoria("Bebidas", btnBebidas);
    }//GEN-LAST:event_btnBebidasActionPerformed

    private void btnExtrasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExtrasActionPerformed
        mostrarCategoria("Extras", btnExtras);
    }//GEN-LAST:event_btnExtrasActionPerformed

    private void btnQuitarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuitarActionPerformed
        quitarSeleccion();
    }//GEN-LAST:event_btnQuitarActionPerformed

    private void btnCobrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCobrarActionPerformed
        cobrarPedido();
    }//GEN-LAST:event_btnCobrarActionPerformed

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        this.setLayout(new java.awt.BorderLayout(0, 18));
        encabezado = new javax.swing.JPanel();
        encabezado.setLayout(new java.awt.BorderLayout(16, 8));
        titulos = new javax.swing.JPanel();
        titulos.setLayout(new java.awt.BorderLayout(0, 4));
        titulo = new javax.swing.JLabel();
        titulo.setText("Nuevo pedido");
        titulo.setFont(new java.awt.Font("Serif", 1, 30));
        titulos.add(titulo, java.awt.BorderLayout.NORTH);
        subtitulo = new javax.swing.JLabel();
        subtitulo.setText("Selecciona los productos");
        subtitulo.setFont(new java.awt.Font("SansSerif", 0, 14));
        titulos.add(subtitulo, java.awt.BorderLayout.SOUTH);
        encabezado.add(titulos, java.awt.BorderLayout.WEST);
        campoBusqueda = new javax.swing.JTextField();
        campoBusqueda.setColumns(22);
        encabezado.add(campoBusqueda, java.awt.BorderLayout.EAST);
        this.add(encabezado, java.awt.BorderLayout.NORTH);
        panelContenido = new javax.swing.JPanel();
        panelContenido.setLayout(new java.awt.BorderLayout(20, 0));
        panelZonaMenu = new javax.swing.JPanel();
        panelZonaMenu.setLayout(new java.awt.BorderLayout(0, 16));
        panelCategorias = new javax.swing.JPanel();
        panelCategorias.setLayout(new java.awt.GridLayout(1, 4, 8, 0));
        btnHamburguesas = new javax.swing.JButton();
        btnHamburguesas.setText("Hamburguesas");
        btnHamburguesas.addActionListener(this::btnHamburguesasActionPerformed);
        panelCategorias.add(btnHamburguesas);
        btnCombos = new javax.swing.JButton();
        btnCombos.setText("Combos");
        btnCombos.addActionListener(this::btnCombosActionPerformed);
        panelCategorias.add(btnCombos);
        btnBebidas = new javax.swing.JButton();
        btnBebidas.setText("Bebidas");
        btnBebidas.addActionListener(this::btnBebidasActionPerformed);
        panelCategorias.add(btnBebidas);
        btnExtras = new javax.swing.JButton();
        btnExtras.setText("Extras");
        btnExtras.addActionListener(this::btnExtrasActionPerformed);
        panelCategorias.add(btnExtras);
        panelZonaMenu.add(panelCategorias, java.awt.BorderLayout.NORTH);
        scrollProductos = new javax.swing.JScrollPane();
        panelProductos = new javax.swing.JPanel();
        panelProductos.setLayout(new java.awt.GridLayout(0, 3, 12, 12));

        contenedorProductos = new javax.swing.JPanel(new java.awt.BorderLayout());
        contenedorProductos.add(panelProductos,java.awt.BorderLayout.NORTH);
        scrollProductos.setViewportView(contenedorProductos);
        scrollProductos.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panelZonaMenu.add(scrollProductos, java.awt.BorderLayout.CENTER);
        panelContenido.add(panelZonaMenu, java.awt.BorderLayout.CENTER);
        panelPedido = new javax.swing.JPanel();
        panelPedido.setLayout(new java.awt.BorderLayout(0, 14));
        panelEncabezadoPedido = new javax.swing.JPanel();
        panelEncabezadoPedido.setLayout(new java.awt.BorderLayout(0, 10));
        lblPedidoActual = new javax.swing.JLabel();
        lblPedidoActual.setText("Pedido actual");
        lblPedidoActual.setFont(new java.awt.Font("Serif", 1, 25));
        panelEncabezadoPedido.add(lblPedidoActual, java.awt.BorderLayout.NORTH);
        zonaMesa = new javax.swing.JPanel();
        zonaMesa.setLayout(new java.awt.BorderLayout(12, 12));
        lblMesa = new javax.swing.JLabel();
        lblMesa.setText("Sin mesa");
        lblMesa.setFont(new java.awt.Font("SansSerif", 0, 14));
        zonaMesa.add(lblMesa, java.awt.BorderLayout.WEST);
        selectorMesa = new javax.swing.JComboBox<>();
        zonaMesa.add(selectorMesa, java.awt.BorderLayout.EAST);
        panelEncabezadoPedido.add(zonaMesa, java.awt.BorderLayout.SOUTH);
        panelPedido.add(panelEncabezadoPedido, java.awt.BorderLayout.NORTH);
        panelZonaLineas = new javax.swing.JPanel();
        panelZonaLineas.setLayout(new java.awt.BorderLayout(0, 12));
        scrollCarrito = new javax.swing.JScrollPane();
        tablaCarrito = new javax.swing.JTable();
        scrollCarrito.setViewportView(tablaCarrito);
        panelZonaLineas.add(scrollCarrito, java.awt.BorderLayout.CENTER);
        ajustes = new javax.swing.JPanel();
        ajustes.setLayout(new java.awt.BorderLayout(0, 10));
        lblVacio = new javax.swing.JLabel();
        lblVacio.setText("Elige productos para comenzar");
        lblVacio.setFont(new java.awt.Font("SansSerif", 0, 13));
        ajustes.add(lblVacio, java.awt.BorderLayout.NORTH);
        controlesCantidad = new javax.swing.JPanel();
        controlesCantidad.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        btnMenos = new javax.swing.JButton();
        btnMenos.setText("−");
        controlesCantidad.add(btnMenos);
        btnMas = new javax.swing.JButton();
        btnMas.setText("+");
        controlesCantidad.add(btnMas);
        btnQuitar = new javax.swing.JButton();
        btnQuitar.setText("Quitar");
        btnQuitar.addActionListener(this::btnQuitarActionPerformed);
        controlesCantidad.add(btnQuitar);
        ajustes.add(controlesCantidad, java.awt.BorderLayout.SOUTH);
        panelZonaLineas.add(ajustes, java.awt.BorderLayout.SOUTH);
        panelPedido.add(panelZonaLineas, java.awt.BorderLayout.CENTER);
        panelResumen = new javax.swing.JPanel();
        panelResumen.setLayout(new java.awt.BorderLayout(0, 14));
        lineaTotal = new javax.swing.JPanel();
        lineaTotal.setLayout(new java.awt.BorderLayout(12, 12));
        lblTotalTitulo = new javax.swing.JLabel();
        lblTotalTitulo.setText("Total");
        lblTotalTitulo.setFont(new java.awt.Font("Serif", 1, 24));
        lineaTotal.add(lblTotalTitulo, java.awt.BorderLayout.WEST);
        lblTotal = new javax.swing.JLabel();
        lblTotal.setText("$ 0");
        lblTotal.setFont(new java.awt.Font("Serif", 1, 24));
        lineaTotal.add(lblTotal, java.awt.BorderLayout.EAST);
        panelResumen.add(lineaTotal, java.awt.BorderLayout.NORTH);
        panelAcciones = new javax.swing.JPanel();
        panelAcciones.setLayout(new java.awt.GridLayout(2, 1, 0, 10));
        btnCobrar = new javax.swing.JButton();
        btnCobrar.setText("Cobrar");
        btnCobrar.addActionListener(this::btnCobrarActionPerformed);
        panelAcciones.add(btnCobrar);
        btnVaciar = new javax.swing.JButton();
        btnVaciar.setText("Vaciar pedido");
        panelAcciones.add(btnVaciar);
        panelResumen.add(panelAcciones, java.awt.BorderLayout.SOUTH);
        panelPedido.add(panelResumen, java.awt.BorderLayout.SOUTH);
        panelContenido.add(panelPedido, java.awt.BorderLayout.EAST);
        this.add(panelContenido, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel encabezado;
    private javax.swing.JPanel titulos;
    private javax.swing.JLabel titulo;
    private javax.swing.JLabel subtitulo;
    private javax.swing.JTextField campoBusqueda;
    private javax.swing.JPanel panelContenido;
    private javax.swing.JPanel panelZonaMenu;
    private javax.swing.JPanel panelCategorias;
    private javax.swing.JButton btnHamburguesas;
    private javax.swing.JButton btnCombos;
    private javax.swing.JButton btnBebidas;
    private javax.swing.JButton btnExtras;
    private javax.swing.JScrollPane scrollProductos;
    private javax.swing.JPanel panelProductos;
    private javax.swing.JPanel contenedorProductos;
    private javax.swing.JPanel panelPedido;
    private javax.swing.JPanel panelEncabezadoPedido;
    private javax.swing.JLabel lblPedidoActual;
    private javax.swing.JPanel zonaMesa;
    private javax.swing.JLabel lblMesa;
    private javax.swing.JComboBox<String> selectorMesa;
    private javax.swing.JPanel panelZonaLineas;
    private javax.swing.JScrollPane scrollCarrito;
    private javax.swing.JTable tablaCarrito;
    private javax.swing.JPanel ajustes;
    private javax.swing.JLabel lblVacio;
    private javax.swing.JPanel controlesCantidad;
    private javax.swing.JButton btnMenos;
    private javax.swing.JButton btnMas;
    private javax.swing.JButton btnQuitar;
    private javax.swing.JPanel panelResumen;
    private javax.swing.JPanel lineaTotal;
    private javax.swing.JLabel lblTotalTitulo;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JPanel panelAcciones;
    private javax.swing.JButton btnCobrar;
    private javax.swing.JButton btnVaciar;
    // End of variables declaration//GEN-END:variables
}
