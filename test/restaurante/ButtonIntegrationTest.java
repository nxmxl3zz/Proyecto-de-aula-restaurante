package restaurante;

import java.awt.*;
import java.lang.reflect.*;
import javax.swing.*;
import restaurante.presentacion.*;
import restaurante.datos.CatalogoMenu;
import com.formdev.flatlaf.*;
import restaurante.datos.DatosRestaurante;
import restaurante.seguridad.*;
import java.nio.file.*;

public class ButtonIntegrationTest {
 static int checks;
 static Object field(Object o,String n) throws Exception { Field f=o.getClass().getDeclaredField(n); f.setAccessible(true); return f.get(o); }
 static JButton button(Object o,String n) throws Exception { return (JButton)field(o,n); }
 static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); checks++; System.out.println("PASS " + message); }
 static JOptionPane pane(Container c) { for(Component x:c.getComponents()) { if(x instanceof JOptionPane)return (JOptionPane)x; if(x instanceof Container){JOptionPane p=pane((Container)x);if(p!=null)return p;}} return null; }
 static void dialogClick(JButton b,int answer,int expected) {
  final int[] seen={0};
  Timer t=new Timer(100,e->{for(Window w:Window.getWindows())if(w instanceof JDialog && w.isShowing()){JOptionPane p=pane((Container)w);if(p!=null){seen[0]++;p.setValue(answer);}}});
  t.start(); try { b.doClick(0); } finally {t.stop();}
  check(seen[0]==expected,"dialog action "+b.getText()+" ("+seen[0]+" dialogs)");
 }
 static void fillClick(JButton b, java.util.function.Consumer<JOptionPane> fill,int expected) {
  final int[] seen={0};
  Timer t=new Timer(100,e->{for(Window w:Window.getWindows())if(w instanceof JDialog && w.isShowing()){
   JOptionPane p=pane((Container)w);if(p!=null){if(seen[0]++==0)fill.accept(p);p.setValue(JOptionPane.OK_OPTION);}
  }});
  t.start();try{b.doClick(0);}finally{t.stop();}
  check(seen[0]==expected,"form action "+b.getText());
 }
 static void run() throws Exception {
  Path carpeta=Files.createTempDirectory("la-doble-ui-test-");
  DatosRestaurante datos=new DatosRestaurante(carpeta);
  Autenticacion auth=new Autenticacion(carpeta);
  Sesion gerente=auth.autenticar(Rol.GERENTE,"gerente123".toCharArray());
  Sesion empleado=auth.autenticar(Rol.EMPLEADO,"empleado123".toCharArray());
  final int[] logouts={0};
  VentanaPrincipal frame=new VentanaPrincipal(gerente,datos,()->logouts[0]++);
  try {
   JPanel content=(JPanel)field(frame,"panelContenido");
   for(String n:new String[]{"btnMenu","btnPedidos","btnInicio"}) {
    button(frame,n).doClick(0);
    String target=n.equals("btnMenu")?"panelMenu":n.equals("btnPedidos")?"panelPedidos":"panelInicio";
    check(content.getComponent(0)==field(frame,target),"navigation "+n);
   }
   for(String n:new String[]{"btnMesas","btnInventario","btnReportes"}) {
    button(frame,n).doClick(0);
    String target=n.equals("btnMesas")?"panelMesas":n.equals("btnInventario")?"panelInventario":"panelReportes";
    check(content.getComponent(0)==field(frame,target),"new navigation "+n);
   }
   Object mesas=field(frame,"panelMesas");
   check(!button(mesas,"btnPedido").isEnabled(),"table actions require selection");
   button(mesas,"mesa1").doClick(0);button(mesas,"btnOcupar").doClick(0);
   check(datos.getMesas()[0],"occupy table");
   button(mesas,"btnLiberar").doClick(0);
   check(!datos.getMesas()[0],"release table");
   button(mesas,"btnPedido").doClick(0);
   check(content.getComponent(0)==field(frame,"panelPedidos") && "Mesa 1".equals(((JLabel)field(field(frame,"panelPedidos"),"lblMesa")).getText()),"take order navigates and assigns table");
   Object inventory=field(frame,"panelInventario"); JTable stock=(JTable)field(inventory,"tabla");
   int initialStockRows=stock.getRowCount();
   check(!button(inventory,"btnAjustar").isEnabled(),"stock actions require selection");
   fillClick(button(inventory,"btnAgregar"),p->{
    JPanel form=(JPanel)p.getMessage(); ((JTextField)form.getComponent(1)).setText("Pan");
    ((JSpinner)form.getComponent(5)).setValue(12.0);
   },1);
   check(stock.getRowCount()==initialStockRows+1 && ((Number)stock.getValueAt(initialStockRows,2)).doubleValue()==12.0,"add stock");
   fillClick(button(inventory,"btnAgregar"),p->{
    JPanel form=(JPanel)p.getMessage(); ((JTextField)form.getComponent(1)).setText("pan");
   },2);
   check(stock.getRowCount()==initialStockRows+1,"duplicate stock rejected");
   fillClick(button(inventory,"btnAgregar"),p->{},2);
   check(stock.getRowCount()==initialStockRows+1,"blank stock name rejected");
   stock.setRowSelectionInterval(initialStockRows,initialStockRows);
   fillClick(button(inventory,"btnAjustar"),p->((JSpinner)p.getMessage()).setValue(7.5),1);
   check(((Number)stock.getValueAt(initialStockRows,2)).doubleValue()==7.5,"adjust stock quantity");
   fillClick(button(inventory,"btnAjustar"),p->{
    JSpinner spinner=(JSpinner)p.getMessage(); ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setText("-1");
   },2);
   check(((Number)stock.getValueAt(initialStockRows,2)).doubleValue()==7.5,"negative stock rejected");
   dialogClick(button(inventory,"btnEliminar"),JOptionPane.NO_OPTION,1);
   check(stock.getRowCount()==initialStockRows+1,"cancel stock deletion");
   dialogClick(button(inventory,"btnEliminar"),JOptionPane.YES_OPTION,1);
   check(stock.getRowCount()==initialStockRows,"confirm stock deletion");
   Object menu=field(frame,"panelMenu"); JTable table=(JTable)field(menu,"tablaPlatos");
   check(!button(menu,"btnEditar").isEnabled()&&!button(menu,"btnEliminar").isEnabled(),"edit/delete disabled without selection");
   table.setRowSelectionInterval(0,0);
   check(button(menu,"btnEditar").isEnabled()&&button(menu,"btnEliminar").isEnabled(),"edit/delete enabled after selection");
   dialogClick(button(menu,"btnEditar"),JOptionPane.CANCEL_OPTION,1);
   dialogClick(button(menu,"btnNuevo"),JOptionPane.CANCEL_OPTION,1);
   table.setRowSelectionInterval(0,0);
   int rows=table.getRowCount(); dialogClick(button(menu,"btnEliminar"),JOptionPane.NO_OPTION,1);
   check(table.getRowCount()==rows,"cancel deletion preserves menu");
   JTextField search=(JTextField)field(menu,"campoBusqueda"); search.setText("NO_SUCH_PRODUCT_123");
   check(table.getRowCount()==0,"menu search filters rows"); search.setText(""); check(table.getRowCount()==rows,"clearing search restores rows");
   fillClick(button(menu,"btnNuevo"),p->{
    JPanel form=(JPanel)p.getMessage();((JTextField)form.getComponent(1)).setText("TEST_GUI");
    ((JTextField)form.getComponent(3)).setText("Plato de prueba");
    ((JTextArea)((JScrollPane)form.getComponent(7)).getViewport().getView()).setText("Descripción de prueba");
    ((JTextField)form.getComponent(9)).setText("12000");
   },1);
   check(datos.getMenu().stream().anyMatch(p->p.getCodigo().equals("TEST_GUI")&&p.getPrecio()==12000),"new menu dish saved to TXT");
   table.setRowSelectionInterval(table.getRowCount()-1,table.getRowCount()-1);
   fillClick(button(menu,"btnReceta"),p->{
    JPanel form=(JPanel)p.getMessage();JTable ingredients=(JTable)((JScrollPane)form.getComponent(1)).getViewport().getView();
    for(int i=0;i<ingredients.getRowCount();i++) {
     if("Pan hamburguesa".equals(ingredients.getValueAt(i,0)))ingredients.setValueAt(1.0,i,2);
     if("Medallones de carne".equals(ingredients.getValueAt(i,0)))ingredients.setValueAt(2.0,i,2);
    }
   },1);
   check(datos.getReceta("TEST_GUI").stream().anyMatch(i->i.getInsumo().equals("Medallones de carne")&&i.getCantidad()==2),"manager chooses and saves ingredients for a new dish");
   table.setRowSelectionInterval(table.getRowCount()-1,table.getRowCount()-1);
   fillClick(button(menu,"btnEditar"),p->{
    JPanel form=(JPanel)p.getMessage();((JTextField)form.getComponent(9)).setText("12500");
   },1);
   check(datos.getMenu().stream().anyMatch(p->p.getCodigo().equals("TEST_GUI")&&p.getPrecio()==12500),"menu price edit saved to TXT");
   table.setRowSelectionInterval(table.getRowCount()-1,table.getRowCount()-1);
   fillClick(button(menu,"btnEditar"),p->{
    JPanel form=(JPanel)p.getMessage();((JTextField)form.getComponent(9)).setText("NaN");
   },2);
   check(datos.getMenu().stream().anyMatch(p->p.getCodigo().equals("TEST_GUI")&&p.getPrecio()==12500),"invalid menu price does not modify saved dish");
   table.setRowSelectionInterval(table.getRowCount()-1,table.getRowCount()-1);
   dialogClick(button(menu,"btnEliminar"),JOptionPane.YES_OPTION,1);
   check(datos.getMenu().stream().noneMatch(p->p.getCodigo().equals("TEST_GUI")),"menu deletion saved to TXT");
   Object orders=field(frame,"panelPedidos"); JPanel products=(JPanel)field(orders,"panelProductos");
   for(String category:new String[]{"Hamburguesas","Combos","Bebidas","Extras"}) {
    button(orders,"btn"+category).doClick(0);
    check(products.getComponentCount()==CatalogoMenu.porCategoria(category).size() && products.getComponentCount()>0,"category "+category);
   }
   double potatoesBefore=datos.getInventario().stream().filter(i->i.getNombre().equals("Papas")).findFirst().get().getCantidad();
   dialogClick(button(products.getComponent(0),"btnInsumos"),JOptionPane.OK_OPTION,1);
   check(datos.getInventario().stream().filter(i->i.getNombre().equals("Papas")).findFirst().get().getCantidad()==potatoesBefore,"viewing ingredients does not consume stock");
   JButton product=button(products.getComponent(0),"btnAgregar"); product.doClick(0); product.doClick(0);
   JTable cart=(JTable)field(orders,"tablaCarrito");
   check(cart.getRowCount()==1&&((Number)cart.getValueAt(0,1)).intValue()==2,"product clicks add and increment quantity");
   button(frame,"btnMesas").doClick(0); button(mesas,"mesa2").doClick(0);
   dialogClick(button(mesas,"btnPedido"),JOptionPane.OK_OPTION,1);
   check(!datos.getMesas()[1] && "Mesa 1".equals(((JLabel)field(orders,"lblMesa")).getText()),"pending cart prevents reassignment and occupying another table");
   cart.setRowSelectionInterval(0,0);button(orders,"btnMas").doClick(0);
   check(((Number)cart.getValueAt(0,1)).intValue()==3,"increment selected quantity");
   button(orders,"btnMenos").doClick(0);
   check(((Number)cart.getValueAt(0,1)).intValue()==2,"decrement selected quantity");
   JTextField productSearch=(JTextField)field(orders,"campoBusqueda");productSearch.setText("NO_PRODUCT_123");
   check(products.getComponentCount()==0,"product search hides nonmatching cards");productSearch.setText("");
   check(products.getComponentCount()>0,"clearing product search restores cards");
   cart.setRowSelectionInterval(0,0);button(orders,"btnQuitar").doClick(0);
   check(cart.getRowCount()==0,"Quitar removes selected line");
   dialogClick(button(orders,"btnCobrar"),JOptionPane.OK_OPTION,1);
   product.doClick(0);dialogClick(button(orders,"btnCobrar"),JOptionPane.NO_OPTION,1);
   check(cart.getRowCount()==1,"cancel checkout preserves cart");
   check(datos.getInventario().stream().filter(i->i.getNombre().equals("Papas")).findFirst().get().getCantidad()==potatoesBefore,"cancel checkout preserves stock");
   dialogClick(button(orders,"btnCobrar"),JOptionPane.YES_OPTION,2);
   check(cart.getRowCount()==0&&datos.getPedidos().size()==1,"checkout records order and clears cart");
   check(datos.getInventario().stream().filter(i->i.getNombre().equals("Papas")).findFirst().get().getCantidad()==potatoesBefore-150,"confirmed checkout consumes one portion of fries");
   button(frame,"btnInventario").doClick(0);
   boolean updated=false;for(int i=0;i<stock.getRowCount();i++)if("Papas".equals(stock.getValueAt(i,0)))updated=((Number)stock.getValueAt(i,2)).doubleValue()==potatoesBefore-150;
   check(updated,"inventory screen refreshes quantities after checkout");
   Object reports=field(frame,"panelReportes"); button(frame,"btnReportes").doClick(0);
   javax.swing.table.DefaultTableModel report=(javax.swing.table.DefaultTableModel)field(reports,"modelo");
   check(report.getRowCount()==1 && ((Number)report.getValueAt(0,2)).intValue()==1 && ((Number)report.getValueAt(0,3)).intValue()==1,"reports include charged order, table and quantity");
   check(((JLabel)field(reports,"resumen")).getText().contains(report.getValueAt(0,4).toString()),"report revenue matches checkout");
   button(reports,"btnActualizar").doClick(0);check(report.getRowCount()==1,"refresh report without duplicating sales");
   frame.addNotify();
   dialogClick(button(frame,"btnCS"),JOptionPane.NO_OPTION,1);
   check(frame.isDisplayable(),"cancel close keeps session open");
   dialogClick(button(frame,"btnCS"),JOptionPane.YES_OPTION,1);
   check(!frame.isDisplayable(),"confirm close disposes window");
   check(logouts[0]==1,"logout calls login navigation once");
   final Sesion[] ingresada={null};
   VentanaLogin login=new VentanaLogin(auth,sesion->ingresada[0]=sesion);
   try {
    JComboBox<?> rol=(JComboBox<?>)field(login,"selectorRol");
    JPasswordField clave=(JPasswordField)field(login,"campoClave");
    clave.setText("wrong");button(login,"btnEntrar").doClick(0);
    check(ingresada[0]==null&&((JLabel)field(login,"lblError")).getText().contains("incorrecta"),"invalid password stays at login");
    check(clave.getPassword().length==0,"login clears password after attempt");
    rol.setSelectedItem(Rol.GERENTE);clave.setText("empleado123");button(login,"btnEntrar").doClick(0);
    check(ingresada[0]==null,"employee password cannot log in as manager");
    clave.setText("gerente123");button(login,"btnEntrar").doClick(0);
    check(ingresada[0]!=null&&ingresada[0].esGerente(),"manager can log in");
   } finally {login.dispose();}
   VentanaPrincipal staff=new VentanaPrincipal(empleado,datos,()->{});
   try {
    JPanel staffContent=(JPanel)field(staff,"panelContenido");Component before=staffContent.getComponent(0);
    for(String admin:new String[]{"btnMenu","btnInventario","btnReportes"}) {
     check(!button(staff,admin).isVisible(),"employee cannot see "+admin);
     button(staff,admin).doClick(0);
     check(staffContent.getComponent(0)==before,"employee cannot invoke "+admin);
    }
    button(staff,"btnPedidos").doClick(0);
    check(staffContent.getComponent(0)==field(staff,"panelPedidos"),"employee can take orders");
    check(((PanelPedidos)field(staff,"panelPedidos")).getPedidosCobrados().size()==1,"new session retains completed orders");
   } finally {staff.dispose();}
   VentanaPrincipal again=new VentanaPrincipal(gerente,datos,()->{});
   try {
    Object orders2=field(again,"panelPedidos"); JPanel cards=(JPanel)field(orders2,"panelProductos");
    button(cards.getComponent(0),"btnAgregar").doClick(0);
    Path archivo=carpeta.resolve("pedidos.txt"),respaldo=carpeta.resolve("pedidos.backup");
    Files.move(archivo,respaldo);Files.createDirectory(archivo);Files.write(archivo.resolve("obstaculo"),java.util.Arrays.asList("bloqueado"));
    try {
     dialogClick(button(orders2,"btnCobrar"),JOptionPane.YES_OPTION,2);
     check(((JTable)field(orders2,"tablaCarrito")).getRowCount()==1,"failed save preserves GUI cart");
     check(datos.getPedidos().size()==1,"failed save does not add a sale");
    } finally {Files.delete(archivo.resolve("obstaculo"));Files.delete(archivo);Files.move(respaldo,archivo);}
    dialogClick(button(orders2,"btnVaciar"),JOptionPane.YES_OPTION,1);
    check(!((PanelPedidos)orders2).tienePedidoPendiente(),"clear cart action");
   } finally {again.dispose();}
  } finally {frame.dispose();datos.close();}
 }
 public static void main(String[] args) throws Exception {
  try {
   if(args.length>0&&args[0].equals("metal")) UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
   else { FlatLaf.registerCustomDefaultsSource("restaurante.themes");FlatDarkLaf.setup(); }
   SwingUtilities.invokeAndWait(()->{try{run();}catch(Exception e){throw new RuntimeException(e);}});
   System.out.println("TOTAL "+checks+" checks passed; theme="+UIManager.getLookAndFeel().getName());System.exit(0);
  }catch(Throwable e){e.printStackTrace();System.exit(1);}
 }
}
