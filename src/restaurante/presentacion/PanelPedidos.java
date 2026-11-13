package restaurante.presentacion;

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
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import restaurante.entidades.DetallePedido;
import restaurante.entidades.Pedido;
import restaurante.entidades.Plato;
import restaurante.controlador.ControladorRestaurante;

/** Punto de venta editable visualmente desde PanelPedidos.form. */
public final class PanelPedidos extends javax.swing.JPanel {
    private final DecimalFormat moneda =
            new DecimalFormat("$ #,##0", new DecimalFormatSymbols(new Locale("es", "CO")));
    private final List<DetallePedido> carrito = new ArrayList<>();
    private final ControladorRestaurante controlador;
    private static final String MAS_VENDIDOS = "Más vendidos";
    private static final int CUPO_MAS_VENDIDOS = 8;
    private final JButton btnTop = new JButton(MAS_VENDIDOS);
    private final JButton btnCocina = new JButton("Enviar a cocina");
    private String categoriaActual = "Hamburguesas";
    private Integer mesaActual;
    private Integer pedidoAbierto;
    private boolean modificado;
    private boolean ajustandoMesa;
    private final javax.swing.JLabel estadoCatalogo = new javax.swing.JLabel();

    /** Cambiar de mesa no debe trasladar silenciosamente un carrito pendiente. */
    public boolean asignarMesa(int mesa) {
        return seleccionarMesa(mesa);
    }

    private boolean seleccionarMesa(Integer mesa) {
        Integer principal = mesa;
        if (controlador != null && mesa != null)
            principal = controlador.getDetalleMesas().get(mesa - 1).getGrupo();
        if (java.util.Objects.equals(principal, mesaActual))
            return true;
        if (tienePedidoPendiente()) {
            JOptionPane.showMessageDialog(this,
                    "Guarda, cobra o vacía el pedido actual antes de cambiar de mesa.",
                    "Pedido pendiente", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        Pedido abierto = controlador == null || mesa == null
                ? null
                : controlador.getPedidoAbiertoPorMesa(mesa);
        if (abierto != null)
            return cargarPedidoAbierto(abierto.getNumero());
        reiniciarPedido(principal);
        return true;
    }

    private void reiniciarPedido(Integer mesa) {
        carrito.clear();
        pedidoAbierto = null;
        modificado = false;
        cambiarMesa(mesa);
        actualizarCarrito();
    }

    private void cambiarMesa(Integer mesa) {
        if (controlador != null && mesa != null)
            mesa = controlador.getDetalleMesas().get(mesa - 1).getGrupo();
        mesaActual = mesa;
        ajustandoMesa = true;
        selectorMesa.setSelectedIndex(mesa == null ? 0 : mesa);
        selectorMesa.setEnabled(pedidoAbierto == null);
        ajustandoMesa = false;
        String etiqueta = mesa == null ? "Sin mesa" : "Mesa " + mesa;
        if (controlador != null && mesa != null) {
            java.util.List<String> numeros = new ArrayList<>();
            for (restaurante.entidades.Mesa actual : controlador.getDetalleMesas())
                if (actual.getGrupo() == mesa)
                    numeros.add(String.valueOf(actual.getNumero()));
            etiqueta = (numeros.size() == 1 ? "Mesa " : "Mesas ") + String.join(" + ", numeros);
        }
        lblMesa.setText(etiqueta);
        lblPedidoActual.setText(
                pedidoAbierto == null ? "Pedido actual" : "Pedido abierto #" + pedidoAbierto);
        titulo.setText(pedidoAbierto == null ? "Nuevo pedido" : "Editar pedido #" + pedidoAbierto);
        subtitulo.setText(pedidoAbierto == null ? "Selecciona los productos"
                                                : "Guarda los cambios o confirma el cobro.");
    }

    /** Recupera una atención guardada y permite modificar sus productos antes del cobro. */
    public boolean cargarPedidoAbierto(int numero) {
        if (controlador == null)
            return false;
        if (tienePedidoPendiente()) {
            JOptionPane.showMessageDialog(
                    this, "Guarda, cobra o vacía los cambios actuales antes de abrir otro pedido.");
            return false;
        }
        for (Pedido pedido : controlador.getPedidosAbiertos())
            if (pedido.getNumero() == numero) {
                carrito.clear();
                for (DetallePedido detalle : pedido.getDetalles())
                    for (int unidad = 0; unidad < detalle.getCantidad(); unidad++) {
                        DetallePedido individual = detalle.copia();
                        individual.setCantidad(1);
                        carrito.add(individual);
                    }
                pedidoAbierto = numero;
                modificado = false;
                cambiarMesa(pedido.getMesa());
                actualizarCarrito();
                return true;
            }
        JOptionPane.showMessageDialog(this, "El pedido abierto ya no está disponible.");
        return false;
    }

    public Pedido guardarPedidoActual() throws java.io.IOException {
        if (controlador == null)
            throw new java.io.IOException("Inicia sesión para guardar el pedido.");
        Pedido pedido = controlador.guardarPedidoAbierto(pedidoAbierto, mesaActual, carrito);
        pedidoAbierto = pedido.getNumero();
        modificado = false;
        cambiarMesa(pedido.getMesa());
        actualizarCarrito();
        lblVacio.setText("Pedido guardado; cambios a salvo.");
        return pedido;
    }

    private void mostrarPedidosAbiertos() {
        if (controlador == null)
            return;
        java.util.List<Pedido> abiertos = controlador.getPedidosAbiertos();
        if (abiertos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay pedidos abiertos guardados.");
            return;
        }
        String[] opciones = new String[abiertos.size()];
        for (int i = 0; i < abiertos.size(); i++)
            opciones[i] = "#" + abiertos.get(i).getNumero() + " · "
                    + abiertos.get(i).getEtiquetaMesas() + " · "
                    + moneda.format(abiertos.get(i).calcularTotal());
        Object elegida = JOptionPane.showInputDialog(this,
                "Selecciona un pedido para editarlo:", "Pedidos abiertos",
                JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        for (int i = 0; i < opciones.length; i++)
            if (opciones[i].equals(elegida)) {
                cargarPedidoAbierto(abiertos.get(i).getNumero());
                break;
            }
    }

    private void nuevoPedido() {
        if (tienePedidoPendiente()) {
            JOptionPane.showMessageDialog(this,
                    "Guarda, cobra o vacía los cambios actuales antes de abrir un pedido nuevo.");
            return;
        }
        reiniciarPedido(null);
    }

    public List<Pedido> getPedidosCobrados() {
        return controlador == null ? java.util.Collections.emptyList() : controlador.getPedidos();
    }

    public PanelPedidos() {
        this(null);
    }
    public PanelPedidos(ControladorRestaurante controlador) {
        this.controlador = controlador;
        initComponents();
        for (JButton boton : new JButton[] {btnSalchipapas, btnPerros, btnPizzas, btnPlatos})
            boton.addActionListener(e -> mostrarCategoria(boton.getText(), boton));

        btnTop.addActionListener(e -> mostrarCategoria(MAS_VENDIDOS, btnTop));
        panelCategorias.add(btnTop, 0);
        btnCocina.setToolTipText("Guarda el pedido y lo marca en preparación");
        btnCocina.addActionListener(e -> enviarACocina());
        gestionPedidos.add(btnCocina, 1);
        campoBusqueda.putClientProperty("JTextField.placeholderText", "Buscar producto…");
        campoBusqueda.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                refrescarCatalogo();
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                refrescarCatalogo();
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                refrescarCatalogo();
            }
        });
        for (int i = 0; i <= 10; i++) selectorMesa.addItem(i == 0 ? "Sin mesa" : "Mesa " + i);
        selectorMesa.addActionListener(e -> {
            if (ajustandoMesa)
                return;
            Integer elegida =
                    selectorMesa.getSelectedIndex() == 0 ? null : selectorMesa.getSelectedIndex();
            if (!seleccionarMesa(elegida))
                cambiarMesa(mesaActual);
        });
        btnGuardar.addActionListener(e -> {
            try {
                guardarPedidoActual();
            } catch (java.io.IOException | SecurityException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "No se guardó el pedido",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
        btnAbrir.addActionListener(e -> mostrarPedidosAbiertos());
        btnNuevo.addActionListener(e -> nuevoPedido());
        btnResumen.addActionListener(e
                -> JOptionPane.showMessageDialog(this, panelResumenPedido(),
                        "Detalle del pedido por unidad", JOptionPane.PLAIN_MESSAGE));
        btnVaciar.addActionListener(e -> {
            if ((!carrito.isEmpty() || pedidoAbierto != null)
                    && JOptionPane.showConfirmDialog(this,
                               pedidoAbierto == null ? "¿Vaciar el pedido actual?"
                                                     : "¿Cancelar el pedido guardado y vaciar sus "
                                               + "productos? No se registrará una venta.",
                               "Vaciar pedido", JOptionPane.YES_NO_OPTION)
                            == JOptionPane.YES_OPTION) {
                try {
                    if (pedidoAbierto != null && controlador != null)
                        controlador.cancelarPedidoAbierto(pedidoAbierto);
                    reiniciarPedido(mesaActual);
                } catch (java.io.IOException | SecurityException ex) {
                    JOptionPane.showMessageDialog(
                            this, "No se canceló el pedido.\n" + ex.getMessage());
                }
            }
        });
        scrollProductos.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ajustarCatalogo();
            }
        });
        Tema.atajo(this, "control F", () -> campoBusqueda.requestFocusInWindow());
        Tema.atajo(this, "control S", () -> {
            if (btnGuardar.isEnabled())
                btnGuardar.doClick();
        });
        campoBusqueda.setToolTipText(
                "Buscar en todo el menú · Ctrl+F · Enter agrega el primer resultado");
        campoBusqueda.addActionListener(e -> {
            if (panelProductos.getComponentCount() > 0)
                ((TarjetaProducto) panelProductos.getComponent(0)).agregarSiDisponible();
        });
        aplicarEstilo();

        mostrarCategoria("Hamburguesas", btnHamburguesas);
        actualizarCarrito();
    }

    private void aplicarEstilo() {
        setOpaque(true);
        setBackground(Tema.FONDO);
        gestionPedidos.setOpaque(false);
        JPanel busqueda = new JPanel(new java.awt.GridBagLayout());
        busqueda.setOpaque(false);
        campoBusqueda.setPreferredSize(new Dimension(240, 38));
        busqueda.add(campoBusqueda);
        encabezado.add(busqueda, java.awt.BorderLayout.EAST);
        panelEncabezadoPedido.setOpaque(false);
        ((java.awt.BorderLayout) panelPedido.getLayout()).setVgap(10);
        ((java.awt.BorderLayout) panelZonaLineas.getLayout()).setVgap(8);
        ((java.awt.BorderLayout) panelResumen.getLayout()).setVgap(8);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        Tema.preparar(this);
        contenedorProductos.setOpaque(false);
        encabezado.setOpaque(false);
        titulos.setOpaque(false);
        zonaMesa.setOpaque(false);
        ajustes.setOpaque(false);
        Tema.principal(btnCobrar);
        panelContenido.setOpaque(false);
        panelZonaMenu.setOpaque(false);
        estadoCatalogo.setForeground(Tema.TEXTO_SEC);
        panelZonaMenu.add(estadoCatalogo, java.awt.BorderLayout.SOUTH);
        panelCategorias.setOpaque(false);
        panelProductos.setOpaque(false);
        panelZonaLineas.setOpaque(false);
        panelResumen.setOpaque(false);
        lineaTotal.setOpaque(false);
        panelAcciones.setOpaque(false);
        panelPedido.setOpaque(true);
        panelPedido.setBackground(Tema.TARJETA);
        panelPedido.setPreferredSize(new Dimension(390, 0));
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

        for (JButton boton : botonesCategorias()) {
            boton.setFont(new Font("SansSerif", Font.PLAIN, 14));
            boton.setFocusPainted(true);
            boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boton.putClientProperty("JButton.buttonType", "roundRect");
            boton.setPreferredSize(new Dimension(125, 36));
        }
        estilizarBoton(btnCobrar, Tema.ACENTO, Tema.FONDO, true);

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
        boton.setFocusPainted(true);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void mostrarCategoria(String categoria, JButton botonActivo) {
        categoriaActual = categoria;
        String busqueda = campoBusqueda.getText().trim().toLowerCase(Locale.ROOT);
        for (JButton boton : botonesCategorias()) {
            boolean activo = boton == botonActivo && busqueda.isEmpty();
            boton.setBackground(activo ? Tema.ACENTO : Tema.TARJETA);
            boton.setForeground(activo ? Tema.FONDO : Tema.TEXTO_SEC);
            boton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(activo ? Tema.ACENTO : Tema.BORDE, 1),
                    BorderFactory.createEmptyBorder(7, 6, 7, 6)));
        }
        panelProductos.removeAll();
        boolean destacados = MAS_VENDIDOS.equals(categoria);
        for (Plato plato : controlador == null ? java.util.Collections.<Plato>emptyList()
                         : destacados && busqueda.isEmpty() ? masVendidos()
                                                            : controlador.getMenu()) {
            if (plato.esAdicional())
                continue;
            if (busqueda.isEmpty() && !destacados && !categoria.equals(plato.getCategoria()))
                continue;
            if (!(plato.getNombre() + " " + plato.getCodigo())
                            .toLowerCase(Locale.ROOT)
                            .contains(busqueda))
                continue;
            TarjetaProducto tarjeta = new TarjetaProducto(plato, () -> agregarProducto(plato));
            if (controlador != null) {
                tarjeta.configurarReceta(controlador.getReceta(plato.getCodigo()));
                List<DetallePedido> propuesta = getCopiaCon(plato);
                try {
                    controlador.comprobarExistencias(propuesta);
                    tarjeta.configurarDisponibilidad(null);
                } catch (java.io.IOException ex) {
                    tarjeta.configurarDisponibilidad(ex.getMessage());
                }
            }

            panelProductos.add(tarjeta);
        }
        ajustarCatalogo();
        estadoCatalogo.setText(panelProductos.getComponentCount() == 0
                        ? "Sin resultados. Prueba otro nombre o código."
                        : panelProductos.getComponentCount() + " productos · "
                                + (busqueda.isEmpty() ? categoria : "Resultados en todo el menú"));
        panelProductos.revalidate();
        panelProductos.repaint();
    }

    private void ajustarCatalogo() {
        boolean compacto = getHeight() > 0 && getHeight() < 720;
        subtitulo.setVisible(!compacto);
        panelAcciones.setLayout(new java.awt.GridLayout(compacto ? 1 : 2, compacto ? 2 : 1, 8, 8));
        for (java.awt.Component producto : panelProductos.getComponents())
            ((TarjetaProducto) producto).configurarCompacta(compacto);
        int ancho = scrollProductos.getViewport().getWidth();
        if (ancho <= 0)
            ancho = 740;
        int columnas = Math.max(1, ancho / 250);
        ((java.awt.GridLayout) panelProductos.getLayout()).setColumns(columnas);
        int filas = (panelProductos.getComponentCount() + columnas - 1) / columnas;
        panelProductos.setPreferredSize(new Dimension(ancho,
                Math.max(0,
                        filas * (compacto ? TarjetaProducto.ALTO_COMPACTO : TarjetaProducto.ALTO)
                                + Math.max(0, filas - 1) * 12)));
        panelProductos.revalidate();
    }

    /** Recarga los platos compartidos después de volver desde la pantalla Menú. */
    public void refrescarCatalogo() {
        cambiarMesa(mesaActual);
        mostrarCategoria(categoriaActual);
    }

    /** Los productos más pedidos según el historial; sin historial, el orden del menú. */
    private List<Plato> masVendidos() {
        java.util.Map<String, Integer> unidades = new java.util.HashMap<>();
        for (Pedido pedido : controlador.getPedidos())
            for (DetallePedido d : pedido.getDetalles())
                unidades.merge(d.getPlato().getCodigo().toLowerCase(Locale.ROOT), d.getCantidad(),
                        Integer::sum);
        List<Plato> ordenados = new ArrayList<>();
        for (Plato p : controlador.getMenu())
            if (!p.esAdicional())
                ordenados.add(p);
        ordenados.sort((x, y) -> Integer.compare(
                unidades.getOrDefault(y.getCodigo().toLowerCase(Locale.ROOT), 0),
                unidades.getOrDefault(x.getCodigo().toLowerCase(Locale.ROOT), 0)));
        return new ArrayList<>(ordenados.subList(0, Math.min(CUPO_MAS_VENDIDOS, ordenados.size())));
    }

    private Pedido abiertoActual() {
        if (controlador == null || pedidoAbierto == null)
            return null;
        for (Pedido p : controlador.getPedidosAbiertos())
            if (p.getNumero() == pedidoAbierto)
                return p;
        return null;
    }

    private void enviarACocina() {
        if (controlador == null || carrito.isEmpty())
            return;
        try {
            if (modificado || pedidoAbierto == null)
                guardarPedidoActual();
            Pedido abierto = abiertoActual();
            if (abierto != null && abierto.getEstado() == restaurante.entidades.EstadoPedido.PENDIENTE)
                controlador.avanzarEstadoPedidoAbierto(abierto.getNumero());
            actualizarCarrito();
        } catch (java.io.IOException | SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se envió a cocina",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private String etiquetaEstado(Pedido abierto) {
        if (abierto == null)
            return "";
        switch (abierto.getEstado()) {
            case EN_PREPARACION:
                return " · En cocina";
            case SERVIDO:
                return " · Servido";
            default:
                return "";
        }
    }

    private JButton[] botonesCategorias() {
        return new JButton[] {btnTop, btnHamburguesas, btnCombos, btnSalchipapas, btnPerros, btnPizzas,
                btnPlatos, btnBebidas};
    }
    public void mostrarCategoria(String categoria) {
        for (JButton boton : botonesCategorias())
            if (boton.getText().equals(categoria)) {
                mostrarCategoria(categoria, boton);
                return;
            }
    }

    private void agregarProducto(Plato plato) {
        if (plato.esAdicional())
            return;
        if (controlador != null) {
            try {
                controlador.comprobarExistencias(getCopiaCon(plato));
            } catch (java.io.IOException ex) {
                lblVacio.setText("No se agregó: revisa las existencias.");
                lblVacio.setToolTipText(ex.getMessage());
                refrescarCatalogo();
                return;
            }
        }
        modificado = true;

        carrito.add(new DetallePedido(plato, 1, plato.getPrecio()));
        actualizarCarrito();
        enfocarUnidad(carrito.size() - 1);
    }

    private List<DetallePedido> getCopiaCon(Plato plato) {
        List<DetallePedido> propuesta = new ArrayList<>(getUnidades());
        propuesta.add(new DetallePedido(plato, 1, plato.getPrecio()));
        return propuesta;
    }

    private void actualizarCarrito() {
        listaUnidades.removeAll();
        double total = 0;
        for (int i = 0; i < carrito.size(); i++) {
            final int unidad = i;
            DetallePedido detalle = carrito.get(i);
            listaUnidades.add(new TarjetaUnidad(i + 1, detalle,
                    ()
                            -> personalizarSeleccion(unidad),
                    () -> duplicarUnidad(unidad), () -> quitarUnidad(unidad)));
            total += detalle.calcularSubtotal();
        }
        if (carrito.isEmpty()) {
            javax.swing.JLabel ayuda = new javax.swing.JLabel(
                    "<html><div style='width:220px'><b>Tu pedido comienza aquí</b><br><br>Agrega "
                    + "un plato del menú. Sus adicionales e indicaciones aparecerán dentro de su "
                    + "unidad.</div></html>");
            ayuda.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));
            ayuda.setForeground(Tema.TEXTO_SEC);
            listaUnidades.add(ayuda);
        }
        lblTotal.setText(moneda.format(total));
        lblVacio.setText(modificado             ? "● Cambios sin guardar"
                        : pedidoAbierto != null ? "✓ Pedido guardado" + etiquetaEstado(abiertoActual())
                                                : "Sin productos");
        lblVacio.setForeground(modificado ? Tema.ACENTO : Tema.EXITO);
        lblVacio.setToolTipText(null);
        btnCobrar.setText("Cobrar " + moneda.format(total));
        btnCobrar.setEnabled(controlador != null && total > 0);
        btnGuardar.setEnabled(controlador != null && !carrito.isEmpty() && modificado);
        btnVaciar.setEnabled(!carrito.isEmpty() || pedidoAbierto != null);
        Pedido enCurso = abiertoActual();
        btnCocina.setEnabled(controlador != null && !carrito.isEmpty()
                && (enCurso == null
                        || enCurso.getEstado() == restaurante.entidades.EstadoPedido.PENDIENTE));
        btnResumen.setEnabled(!carrito.isEmpty());
        listaUnidades.revalidate();
        listaUnidades.repaint();
        mostrarCategoria(categoriaActual);
    }

    private void enfocarUnidad(int unidad) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (unidad >= 0 && unidad < carrito.size()) {
                JPanel tarjeta = (JPanel) listaUnidades.getComponent(unidad);
                tarjeta.scrollRectToVisible(
                        new java.awt.Rectangle(0, 0, tarjeta.getWidth(), tarjeta.getHeight()));
                ((TarjetaUnidad) tarjeta).enfocar();
            }
        });
    }
    public List<DetallePedido> getUnidades() {
        List<DetallePedido> copia = new ArrayList<>();
        for (DetallePedido d : carrito) copia.add(d.copia());
        return java.util.Collections.unmodifiableList(copia);
    }
    public void personalizarUnidad(int unidad, DetallePedido configurado)
            throws java.io.IOException {
        if (controlador == null || unidad < 0 || unidad >= carrito.size())
            throw new java.io.IOException("Selecciona una unidad del pedido.");
        if (configurado == null || configurado.getPlato() == null
                || !carrito.get(unidad).getPlato().getCodigo().equals(
                        configurado.getPlato().getCodigo()))
            throw new java.io.IOException(
                    "La personalización debe corresponder al plato seleccionado.");
        carrito.set(unidad, controlador.validarUnidad(configurado));
        modificado = true;
        actualizarCarrito();
        enfocarUnidad(unidad);
    }
    private void personalizarSeleccion(int unidad) {
        if (unidad < 0 || controlador == null)
            return;
        PersonalizarPlato formulario = new PersonalizarPlato(carrito.get(unidad), controlador);
        javax.swing.JDialog dialogo =
                new javax.swing.JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                        "Personalizar unidad " + (unidad + 1),
                        java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel contenido = new JPanel(new java.awt.BorderLayout(12, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(formulario, java.awt.BorderLayout.CENTER);
        JPanel pie = new JPanel(new java.awt.BorderLayout(8, 8));
        javax.swing.JLabel error = new javax.swing.JLabel(" ");
        error.setForeground(new Color(0xED998B));
        JPanel acciones = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton cancelar = new JButton("Cancelar"), aplicar = new JButton("Aplicar a esta unidad");
        Tema.principal(aplicar);
        cancelar.addActionListener(e -> dialogo.dispose());
        aplicar.addActionListener(e -> {
            try {
                personalizarUnidad(unidad, formulario.aplicar());
                dialogo.dispose();
            } catch (java.io.IOException | IllegalArgumentException | SecurityException ex) {
                error.setText("<html><div style='width:440px'>" + Tema.escapar(ex.getMessage())
                        + "</div></html>");
                dialogo.pack();
            }
        });
        acciones.add(cancelar);
        acciones.add(aplicar);
        pie.add(error, java.awt.BorderLayout.CENTER);
        pie.add(acciones, java.awt.BorderLayout.SOUTH);
        contenido.add(pie, java.awt.BorderLayout.SOUTH);
        dialogo.setContentPane(contenido);
        dialogo.getRootPane().setDefaultButton(aplicar);
        Tema.atajo(contenido, "ESCAPE", dialogo::dispose);
        dialogo.pack();
        Dimension pantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        dialogo.setSize(Math.min(dialogo.getWidth(), pantalla.width - 40),
                Math.min(dialogo.getHeight(), pantalla.height - 48));
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }
    private javax.swing.JScrollPane panelResumenPedido() {
        javax.swing.JTextArea texto =
                new javax.swing.JTextArea(ResumenPedido.texto(lblMesa.getText(), getUnidades()));
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setCaretPosition(0);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(texto);
        scroll.setPreferredSize(new Dimension(650, 400));
        return scroll;
    }

    private void duplicarUnidad(int unidad) {
        DetallePedido copia = carrito.get(unidad).copia();
        List<DetallePedido> propuesta = new ArrayList<>(getUnidades());
        propuesta.add(copia);
        try {
            if (controlador != null)
                controlador.comprobarExistencias(propuesta);
            carrito.add(unidad + 1, copia);
            modificado = true;
            actualizarCarrito();
            enfocarUnidad(unidad + 1);
        } catch (java.io.IOException ex) {
            lblVacio.setText("No se duplicó: existencias insuficientes.");
            lblVacio.setToolTipText(ex.getMessage());
        }
    }
    public boolean tienePedidoPendiente() {
        return modificado && (!carrito.isEmpty() || pedidoAbierto != null);
    }
    private void quitarUnidad(int unidad) {
        carrito.remove(unidad);
        modificado = true;
        actualizarCarrito();
        enfocarUnidad(Math.min(unidad, carrito.size() - 1));
    }

    private void cobrarPedido() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un producto antes de cobrar.",
                    "Pedido vacío", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (controlador == null) {
            JOptionPane.showMessageDialog(this, "Abre la aplicación desde el inicio de sesión.");
            return;
        }
        double totalCobro = 0;
        for (DetallePedido d : carrito) totalCobro += d.calcularSubtotal();
        DialogoCobro.Pago pago = DialogoCobro.pedir(this, panelResumenPedido(), totalCobro);
        if (pago == null)
            return;
        Pedido pedido;
        try {
            pedido = pedidoAbierto == null
                    ? controlador.registrarPedido(mesaActual, carrito, pago.metodo, pago.recibido)
                    : controlador.cobrarPedidoAbierto(
                            pedidoAbierto, carrito, pago.metodo, pago.recibido);
        } catch (java.io.IOException | SecurityException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el pedido. El carrito sigue disponible.\n"
                            + ex.getMessage(),
                    "No se registró el cobro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        reiniciarPedido(mesaActual);
        String detallePago = pedido.getMetodoPago() == null ? "" : " · " + pedido.getMetodoPago()
                + (pedido.getCambio() > 0 ? " · cambio " + Tema.moneda(pedido.getCambio()) : "");
        lblVacio.setText("✓ Pedido #" + pedido.getNumero() + " cobrado correctamente" + detallePago);
    }

    private void btnHamburguesasActionPerformed(
            java.awt.event.ActionEvent evt) { // GEN-FIRST:event_btnHamburguesasActionPerformed
        mostrarCategoria("Hamburguesas", btnHamburguesas);
    } // GEN-LAST:event_btnHamburguesasActionPerformed

    private void btnCombosActionPerformed(
            java.awt.event.ActionEvent evt) { // GEN-FIRST:event_btnCombosActionPerformed
        mostrarCategoria("Combos", btnCombos);
    } // GEN-LAST:event_btnCombosActionPerformed

    private void btnBebidasActionPerformed(
            java.awt.event.ActionEvent evt) { // GEN-FIRST:event_btnBebidasActionPerformed
        mostrarCategoria("Bebidas", btnBebidas);
    } // GEN-LAST:event_btnBebidasActionPerformed

    private void btnCobrarActionPerformed(
            java.awt.event.ActionEvent evt) { // GEN-FIRST:event_btnCobrarActionPerformed
        cobrarPedido();
    } // GEN-LAST:event_btnCobrarActionPerformed

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
        gestionPedidos = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        btnGuardar = new javax.swing.JButton("Guardar pedido");
        btnAbrir = new javax.swing.JButton("Pedidos abiertos");
        btnNuevo = new javax.swing.JButton("Nuevo pedido");
        btnResumen = new javax.swing.JButton("Ver pedido");
        gestionPedidos.add(btnGuardar); gestionPedidos.add(btnAbrir); gestionPedidos.add(btnNuevo);gestionPedidos.add(btnResumen);
        encabezado.add(gestionPedidos, java.awt.BorderLayout.SOUTH);
        this.add(encabezado, java.awt.BorderLayout.NORTH);
        panelContenido = new javax.swing.JPanel();
        panelContenido.setLayout(new java.awt.BorderLayout(20, 0));
        panelZonaMenu = new javax.swing.JPanel();
        panelZonaMenu.setLayout(new java.awt.BorderLayout(0, 16));
        panelCategorias = new javax.swing.JPanel();
        panelCategorias.setLayout(new java.awt.GridLayout(0, 4, 8, 8));
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
        btnSalchipapas = new javax.swing.JButton("Salchipapas");
        btnPerros = new javax.swing.JButton("Perros calientes");
        btnPizzas = new javax.swing.JButton("Pizzas");
        btnPlatos = new javax.swing.JButton("Platos");
        for(JButton boton:new JButton[]{btnSalchipapas,btnPerros,btnPizzas,btnPlatos}){
            panelCategorias.add(boton);
        }
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
        listaUnidades = new ListaVertical();
        scrollCarrito.setViewportView(listaUnidades);
        panelZonaLineas.add(scrollCarrito, java.awt.BorderLayout.CENTER);
        ajustes = new javax.swing.JPanel();
        ajustes.setLayout(new java.awt.BorderLayout(0, 10));
        lblVacio = new javax.swing.JLabel();
        lblVacio.setText("Elige productos para comenzar");
        lblVacio.setFont(new java.awt.Font("SansSerif", 0, 13));
        ajustes.add(lblVacio, java.awt.BorderLayout.NORTH);
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
    private javax.swing.JPanel gestionPedidos;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnAbrir;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JButton btnResumen;
    private javax.swing.JPanel panelContenido;
    private javax.swing.JPanel panelZonaMenu;
    private javax.swing.JPanel panelCategorias;
    private javax.swing.JButton btnHamburguesas;
    private javax.swing.JButton btnCombos;
    private javax.swing.JButton btnBebidas;
    private javax.swing.JButton btnSalchipapas;
    private javax.swing.JButton btnPerros;
    private javax.swing.JButton btnPizzas;
    private javax.swing.JButton btnPlatos;
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
    private ListaVertical listaUnidades;
    private javax.swing.JPanel ajustes;
    private javax.swing.JLabel lblVacio;
    private javax.swing.JPanel panelResumen;
    private javax.swing.JPanel lineaTotal;
    private javax.swing.JLabel lblTotalTitulo;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JPanel panelAcciones;
    private javax.swing.JButton btnCobrar;
    private javax.swing.JButton btnVaciar;
    // End of variables declaration//GEN-END:variables
}
