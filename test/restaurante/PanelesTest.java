package restaurante;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import restaurante.controlador.ControladorRestaurante;
import restaurante.entidades.*;
import restaurante.presentacion.*;
import restaurante.seguridad.*;
import restaurante.servicios.ServicioRestaurante;

/** Prueba los eventos de paneles reales y los pinta sin necesitar un servidor gráfico. */
public final class PanelesTest {
    private static int pruebas;
    private static Object campo(Object objeto, String nombre) throws Exception {
        Field field = objeto.getClass().getDeclaredField(nombre);
        field.setAccessible(true);
        return field.get(objeto);
    }
    private static JButton boton(Object objeto, String nombre) throws Exception { return (JButton) campo(objeto, nombre); }
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        pruebas++; System.out.println("OK " + mensaje);
    }
    private static void distribuir(Container contenedor) {
        contenedor.doLayout();
        for (Component hijo : contenedor.getComponents()) if (hijo instanceof Container) distribuir((Container) hijo);
    }
    private static void capturar(JPanel panel, String nombre) throws Exception {
        panel.addNotify();
        panel.setSize(1320, 780); distribuir(panel);
        BufferedImage imagen = new BufferedImage(1320,780,BufferedImage.TYPE_INT_RGB);
        Graphics2D grafica = imagen.createGraphics();
        panel.printAll(grafica); grafica.dispose();
        Path carpeta = Paths.get("build/capturas-prueba"); Files.createDirectories(carpeta);
        ImageIO.write(imagen,"png",carpeta.resolve(nombre+".png").toFile());
        panel.removeNotify();
        comprobar(imagen.getWidth()==1320,"panel renderizado: "+nombre);
    }
    private static void ejecutar() throws Exception {
        Path carpeta=Files.createTempDirectory("la-doble-paneles-");
        try(ServicioRestaurante servicio=new ServicioRestaurante(carpeta)) {
            DatosPrueba.stockClasico(servicio);
            Autenticacion acceso=new Autenticacion(carpeta);
            Sesion gerente=acceso.autenticar(Rol.GERENTE,"gerente123".toCharArray());
            ControladorRestaurante controlador=new ControladorRestaurante(servicio,gerente);
            PanelPedidos pedidos=new PanelPedidos(controlador);
            JPanel productos=(JPanel)campo(pedidos,"panelProductos");
            comprobar(productos.getComponentCount()==3,"panel muestra las tres hamburguesas");
            JTextField buscar=(JTextField)campo(pedidos,"campoBusqueda");
            buscar.setText("H001"); comprobar(productos.getComponentCount()==1,"búsqueda de producto por código");
            boton(productos.getComponent(0),"btnAgregar").doClick();
            boton(productos.getComponent(0),"btnAgregar").doClick();
            JPanel carrito=(JPanel)campo(pedidos,"listaUnidades");
            comprobar(pedidos.getUnidades().size()==2 && pedidos.getUnidades().get(0).getCantidad()==1,"cada clic crea una unidad independiente");
            boton(carrito.getComponent(0),"btnDuplicar").doClick();
            comprobar(pedidos.getUnidades().size()==3,"evento de duplicar una unidad");
            boton(carrito.getComponent(1),"btnQuitar").doClick();
            comprobar(pedidos.getUnidades().size()==2,"evento de retirar una unidad");
            boton(carrito.getComponent(0),"btnQuitar").doClick();comprobar(pedidos.getUnidades().size()==1,"quitar afecta solo a la unidad elegida");
            boton(carrito.getComponent(0),"btnQuitar").doClick();comprobar(pedidos.getUnidades().size()==0,"evento de quitar producto");
            comprobar(servicio.getPedidos().isEmpty(),"editar carrito no genera cobros");
            buscar.setText("");capturar(pedidos,"pedidos");
            PanelMenu menu=new PanelMenu(controlador);
            JTable tablaMenu=(JTable)campo(menu,"tablaPlatos");
            comprobar(tablaMenu.getRowCount()==22,"menú administra los veintidós productos y adicionales");
            ((JTextField)campo(menu,"campoBusqueda")).setText("NO_EXISTE");
            comprobar(tablaMenu.getRowCount()==0,"búsqueda de menú filtra filas");
            ((JTextField)campo(menu,"campoBusqueda")).setText("");
            tablaMenu.setRowSelectionInterval(0,0);
            comprobar(boton(menu,"btnEditar").isEnabled() && boton(menu,"btnReceta").isEnabled(),"selección habilita acciones de menú");
            capturar(menu,"menu");
            PanelMesas mesas=new PanelMesas(controlador,numero->true);
            boton(mesas,"mesa1").doClick();boton(mesas,"btnOcupar").doClick();
            comprobar(controlador.getMesas()[0] && mesas.ocupadas()==1,"evento de ocupar mesa persiste");
            boton(mesas,"btnLiberar").doClick();
            comprobar(!controlador.getMesas()[0],"evento de liberar mesa persiste");
            capturar(mesas,"mesas");
            PanelInventario inventario=new PanelInventario(controlador);
            JTable stock=(JTable)campo(inventario,"tabla");
            comprobar(stock.getRowCount()==servicio.getInventario().size(),"inventario refleja los insumos guardados");
            Plato plato=controlador.getMenu().get(0);
            controlador.registrarPedido(1,java.util.Arrays.asList(new DetallePedido(plato,1,plato.getPrecio())));
            inventario.refrescar();
            boolean descuento=false;
            for(int i=0;i<stock.getRowCount();i++)if("Pan hamburguesa".equals(stock.getValueAt(i,0)))descuento=((Number)stock.getValueAt(i,2)).doubleValue()==19;
            comprobar(descuento,"panel de inventario refleja el cobro");
            capturar(inventario,"inventario");
            PanelReportes reportes=new PanelReportes(controlador::getReporteVentas);
            JTable ventas=(JTable)campo(reportes,"tabla");
            comprobar(ventas.getRowCount()==1,"panel de reportes muestra venta confirmada");
            boton(reportes,"btnActualizar").doClick();comprobar(ventas.getRowCount()==1,"actualizar reporte no duplica ventas");
            capturar(reportes,"reportes");
            PanelInicio inicio=new PanelInicio();inicio.refrescar(controlador.getPedidos(),mesas.ocupadas());
            comprobar("1".equals(((JLabel)campo(inicio,"lblPedidos")).getText()),"inicio actualiza indicador de pedidos");
            inicio.permitirAdministracion(false);
            comprobar(!boton(inicio,"btnMenu").isVisible() && !boton(inicio,"btnInventario").isVisible(),"inicio oculta administración al empleado");
            inicio.permitirAdministracion(true);capturar(inicio,"inicio");
            controlador.configurarSillas(3,6);controlador.unirMesas(java.util.Arrays.asList(1,3));mesas.refrescar();
            comprobar(boton(mesas,"mesa3").getText().contains("1 + 3"),"las tarjetas muestran las mesas unidas");
            comprobar(((JLabel)campo(mesas,"nota")).getText().contains("8 sillas"),"la vista suma la capacidad de la reunión");
            comprobar(pedidos.asignarMesa(3) && ((JLabel)campo(pedidos,"lblMesa")).getText().equals("Mesas 1 + 3"),"pedidos muestra todo el grupo seleccionado");
            boton(productos.getComponent(0),"btnAgregar").doClick();boton(pedidos,"btnGuardar").doClick();
            int abierto=controlador.getPedidosAbiertos().get(0).getNumero();
            comprobar(controlador.getPedidosAbiertos().size()==1,"botón guardar conserva un pedido abierto");
            comprobar(!pedidos.tienePedidoPendiente(),"pedido guardado no se advierte como cambio sin guardar");
            boton(pedidos,"btnNuevo").doClick();
            comprobar(pedidos.getUnidades().size()==0 && controlador.getPedidosAbiertos().size()==1,"nuevo pedido conserva las atenciones guardadas");
            comprobar(pedidos.asignarMesa(3) && pedidos.getUnidades().size()==1,"seleccionar una mesa unida recupera su pedido");
            boton(carrito.getComponent(0),"btnDuplicar").doClick();
            comprobar(pedidos.tienePedidoPendiente(),"edición detecta cambios todavía no guardados");
            boton(pedidos,"btnGuardar").doClick();
            comprobar(controlador.getPedidosAbiertos().get(0).getDetalles().size()==2,"guardar edición conserva dos unidades independientes");
            comprobar(controlador.getInventario().stream().anyMatch(i->i.getNombre().equals("Pan hamburguesa")&&i.getCantidad()==19),"editar desde Swing no descuenta inventario");
            mesas.refrescar();
            comprobar(boton(mesas,"btnPedido").getText().contains("Editar")&&!boton(mesas,"btnLiberar").isEnabled(),"mesa con pedido abierto ofrece edición y evita liberación accidental");
            capturar(pedidos,"pedido-abierto");capturar(mesas,"mesas-unidas");
            controlador.cobrarPedidoAbierto(abierto,controlador.getPedidosAbiertos().get(0).getDetalles());reportes.refrescar();
            comprobar(ventas.getRowCount()==2 && "Mesas 1 + 3".equals(ventas.getValueAt(1,2)),"reporte conserva las mesas de la reunión cobrada");
            capturar(reportes,"reporte-reunion");
            ImageIO.write(new Logotipo(512).imagen(),"png",Paths.get("build/capturas-prueba/logotipo.png").toFile());
        }
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless","true");
        Tema.instalar();
        SwingUtilities.invokeAndWait(()-> {try {ejecutar();}catch(Exception ex){throw new RuntimeException(ex);}});
        System.out.println("TOTAL PANELES: "+pruebas+" comprobaciones.");
    }
}
