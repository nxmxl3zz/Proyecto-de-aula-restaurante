package restaurante;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import restaurante.datos.*;
import restaurante.servicios.ServicioRestaurante;
import restaurante.entidades.*;
import restaurante.seguridad.*;

/** Pruebas independientes de la interfaz; todos los archivos se crean en una carpeta temporal. */
public final class DatosRestauranteTest {
    private static int pruebas;
    private static void comprobar(boolean condicion,String mensaje) {
        if(!condicion)throw new AssertionError(mensaje);pruebas++;System.out.println("OK "+mensaje);
    }
    private interface Accion {void ejecutar() throws Exception;}
    private static void rechazar(Class<? extends Throwable> tipo,Accion accion,String mensaje) throws Exception {
        try {accion.ejecutar();}catch(Throwable ex){if(tipo.isInstance(ex)){comprobar(true,mensaje);return;}throw ex;}
        throw new AssertionError(mensaje);
    }
    private static Sesion entrar(Path carpeta,Rol rol,String clave) throws Exception {
        return new Autenticacion(carpeta).autenticar(rol,clave.toCharArray());
    }
    public static void main(String[] args) throws Exception {
        if(args.length==2&&"leer".equals(args[0])) {
            try(ServicioRestaurante datos=new ServicioRestaurante(Paths.get(args[1]))) {
                if(datos.getPedidos().size()!=2||datos.getPedidos().get(1).getNumero()!=2)throw new AssertionError("No se recuperó el historial en el nuevo proceso.");
            }return;
        }
        Path carpeta=Files.createTempDirectory("la-doble-datos-test-");
        Sesion empleado,gerente;
        Plato original=new Plato("H001","Hamburguesa ñ | especial","Línea uno\nLínea dos \\ salsa",10000,"Hamburguesas");
        List<DetallePedido> carrito=new ArrayList<>();carrito.add(new DetallePedido(original,2,10000));
        try(ServicioRestaurante datos=new ServicioRestaurante(carpeta)) {
            empleado=entrar(carpeta,Rol.EMPLEADO,"empleado123");gerente=entrar(carpeta,Rol.GERENTE,"gerente123");
            comprobar(empleado!=null&&!empleado.esGerente(),"autenticación de empleado");
            comprobar(gerente!=null&&gerente.esGerente(),"autenticación de gerente");
            comprobar(entrar(carpeta,Rol.GERENTE,"empleado123")==null,"contraseña no sirve para otro rol");
            comprobar(entrar(carpeta,Rol.EMPLEADO,"")==null,"contraseña vacía rechazada");
            comprobar(datos.getPedidos().isEmpty(),"primer inicio sin ventas falsas");
            List<Plato> base=datos.getMenu();base.get(0).setPrecio(10000);datos.guardarMenu(gerente,base);
            rechazar(IOException.class,()->datos.registrarPedido(empleado,null,Arrays.asList(new DetallePedido(original,1,1))),"el precio de una línea debe coincidir con el menú");
            Map<String,Double> precios=new HashMap<>();for(Plato p:CatalogoMenu.getPlatos())precios.put(p.getCodigo(),p.getPrecio());
            for(String[] combo:new String[][]{{"C001","H001"},{"C002","H002"},{"C003","H003"}})comprobar(precios.get(combo[0])<precios.get(combo[1])+precios.get("E001")+precios.get("B001"),"el combo "+combo[0]+" cuesta menos que sus artículos por separado");
            rechazar(IOException.class,()->new ServicioRestaurante(carpeta),"bloqueo contra dos aplicaciones simultáneas");
            rechazar(SecurityException.class,()->datos.guardarMenu(empleado,datos.getMenu()),"empleado no modifica el menú");
            rechazar(SecurityException.class,()->datos.guardarInventario(empleado,new ArrayList<>()),"empleado no modifica inventario");
            rechazar(SecurityException.class,()->datos.registrarPedido(null,1,carrito),"no hay cobros sin sesión");
            rechazar(IOException.class,()->datos.registrarPedido(empleado,1,new ArrayList<>()),"pedido vacío rechazado");
            rechazar(IOException.class,()->datos.registrarPedido(empleado,11,carrito),"mesa inexistente rechazada");
            Pedido pedido=datos.registrarPedido(empleado,3,carrito);
            comprobar(pedido.getNumero()==1&&pedido.calcularTotal()==20000,"primer cobro y total");
            original.setNombre("CAMBIADO");carrito.get(0).setCantidad(99);
            comprobar(datos.getPedidos().get(0).getDetalles().get(0).getCantidad()==2,"historial independiente del carrito");
            Pedido copia=datos.getPedidos().get(0);copia.getDetalles().clear();
            comprobar(datos.getPedidos().get(0).getDetalles().size()==1,"lectura del historial no altera la venta original");
            List<Insumo> stock=new ArrayList<>(datos.getInventario());stock.add(0,new Insumo("# Pan | grande","unidades",7.5));
            datos.guardarInventario(gerente,stock);
            datos.guardarMesa(empleado,3,true);
            List<Plato> menu=datos.getMenu();menu.get(0).setPrecio(99999);datos.guardarMenu(gerente,menu);
            comprobar(datos.getPedidos().get(0).calcularTotal()==20000,"cambiar precio del menú no cambia una venta antigua");
            Path archivo=carpeta.resolve("pedidos.txt"),respaldo=carpeta.resolve("pedidos.respaldo");
            Files.move(archivo,respaldo);Files.createDirectory(archivo);Files.write(archivo.resolve("obstaculo"),Arrays.asList("no reemplazar"));
            rechazar(IOException.class,()->datos.registrarPedido(gerente,null,Arrays.asList(new DetallePedido(original,1,99999))),"error de escritura no confirma una venta");
            comprobar(datos.getPedidos().size()==1,"error de escritura no altera ventas en memoria");
            Files.delete(archivo.resolve("obstaculo"));Files.delete(archivo);Files.move(respaldo,archivo);
        }
        try(ServicioRestaurante datos=new ServicioRestaurante(carpeta)) {
            comprobar(datos.getPedidos().size()==1,"ventas recuperadas tras cerrar y abrir archivos");
            Pedido p=datos.getPedidos().get(0);
            comprobar(p.getMesa()==3&&p.calcularTotal()==20000,"mesa, fecha y total recuperados");
            comprobar(p.getDetalles().get(0).getPlato().getNombre().equals("Hamburguesa ñ | especial"),"acentos y separadores se conservan");
            comprobar(p.getDetalles().get(0).getPlato().getDescripcion().equals("Línea uno\nLínea dos \\ salsa"),"saltos de línea y barras se conservan");
            comprobar(datos.getInventario().get(0).getCantidad()==7.5&&datos.getInventario().get(0).getNombre().equals("# Pan | grande"),"inventario recuperado incluso si el nombre empieza con #");
            comprobar(datos.getMesas()[2],"ocupación de mesa recuperada");
            comprobar(datos.getMenu().get(0).getPrecio()==99999,"menú editado recuperado");
            Pedido segundo=datos.registrarPedido(gerente,null,Arrays.asList(new DetallePedido(original,1,99999)));
            comprobar(segundo.getNumero()==2,"numeración continúa después del reinicio");
        }
        Process hijo=new ProcessBuilder(Paths.get(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),DatosRestauranteTest.class.getName(),"leer",carpeta.toString()).inheritIO().start();
        comprobar(hijo.waitFor()==0,"persistencia comprobada en otro proceso Java");
        try(ServicioRestaurante datos=new ServicioRestaurante(carpeta)) {
            Sesion gerente2=entrar(carpeta,Rol.GERENTE,"gerente123");
            Plato caro=datos.getMenu().get(0);
            rechazar(IOException.class,()->datos.registrarPedido(gerente2,null,Arrays.asList(new DetallePedido(caro,1,caro.getPrecio())),MetodoPago.EFECTIVO,1000.0),"efectivo insuficiente no se cobra");
            rechazar(IOException.class,()->datos.registrarPedido(gerente2,null,Arrays.asList(new DetallePedido(caro,1,caro.getPrecio())),null,null),"el método de pago es obligatorio");
            Pedido efectivo=datos.registrarPedido(gerente2,null,Arrays.asList(new DetallePedido(caro,1,caro.getPrecio())),MetodoPago.EFECTIVO,caro.getPrecio()+1000);
            comprobar(efectivo.getCambio()==1000,"el cambio en efectivo se calcula");
            Pedido tarjeta=datos.registrarPedido(gerente2,null,Arrays.asList(new DetallePedido(caro,1,caro.getPrecio())),MetodoPago.TARJETA,null);
            comprobar(tarjeta.getMetodoPago()==MetodoPago.TARJETA&&tarjeta.getCambio()==0,"pago con tarjeta no genera cambio");
        }
        try(ServicioRestaurante datos=new ServicioRestaurante(carpeta)) {
            List<Pedido> todos=datos.getPedidos();
            Pedido e=todos.get(todos.size()-2),t=todos.get(todos.size()-1);
            comprobar(e.getMetodoPago()==MetodoPago.EFECTIVO&&e.getEfectivoRecibido()==e.calcularTotal()+1000&&t.getMetodoPago()==MetodoPago.TARJETA,"método de pago y efectivo recibido se recuperan");
            comprobar(todos.get(0).getMetodoPago()==MetodoPago.EFECTIVO,"cobro anterior conserva su método");
        }
        int[] numeroCocina=new int[1];
        try(ServicioRestaurante datos=new ServicioRestaurante(carpeta)) {
            Sesion s=entrar(carpeta,Rol.EMPLEADO,"empleado123");
            Plato base=datos.getMenu().get(0);
            Pedido abierto=datos.guardarPedidoAbierto(s,null,5,Arrays.asList(new DetallePedido(base,1,base.getPrecio())));
            numeroCocina[0]=abierto.getNumero();
            comprobar(abierto.getEstado()==EstadoPedido.PENDIENTE,"un pedido guardado nace pendiente");
            comprobar(datos.avanzarEstadoPedidoAbierto(s,abierto.getNumero()).getEstado()==EstadoPedido.EN_PREPARACION,"enviar a cocina lo pone en preparación");
            Pedido editado=datos.guardarPedidoAbierto(s,abierto.getNumero(),5,Arrays.asList(new DetallePedido(base,2,base.getPrecio())));
            comprobar(editado.getEstado()==EstadoPedido.EN_PREPARACION,"editar un pedido en cocina conserva su estado");
            comprobar(datos.avanzarEstadoPedidoAbierto(s,abierto.getNumero()).getEstado()==EstadoPedido.SERVIDO,"cocina marca el pedido como servido");
            rechazar(IOException.class,()->datos.avanzarEstadoPedidoAbierto(s,numeroCocina[0]),"un pedido servido no avanza más");
            rechazar(SecurityException.class,()->datos.avanzarEstadoPedidoAbierto(null,numeroCocina[0]),"cambiar el estado exige sesión");
        }
        try(ServicioRestaurante datos=new ServicioRestaurante(carpeta)) {
            comprobar(datos.getPedidosAbiertos().get(0).getEstado()==EstadoPedido.SERVIDO,"el estado de cocina se recupera al reabrir");
            Sesion s=entrar(carpeta,Rol.EMPLEADO,"empleado123");
            Pedido cobrado=datos.cobrarPedidoAbierto(s,numeroCocina[0],datos.getPedidosAbiertos().get(0).getDetalles(),MetodoPago.TRANSFERENCIA,null);
            comprobar(cobrado.getEstado()==EstadoPedido.COBRADO&&datos.getPedidosAbiertos().isEmpty(),"cobrar un pedido servido lo cierra");
        }
        Path archivo=carpeta.resolve("pedidos.txt");byte[] originalArchivo=Files.readAllBytes(archivo);
        Files.write(archivo,Arrays.asList("P|1|2026-11-04|3|COBRADO|999"),StandardCharsets.UTF_8);
        byte[] corrupto=Files.readAllBytes(archivo);
        rechazar(IOException.class,()->new ServicioRestaurante(carpeta),"archivo corrupto impide abrir en lugar de borrar ventas");
        comprobar(Arrays.equals(corrupto,Files.readAllBytes(archivo)),"archivo corrupto permanece intacto para recuperarlo");
        Files.write(archivo,originalArchivo);
        Path usuarios=carpeta.resolve("usuarios.txt");
        Files.write(usuarios,Arrays.asList("EMPLEADO|nuevaclave","GERENTE|gerente123"),StandardCharsets.UTF_8);
        comprobar(entrar(carpeta,Rol.EMPLEADO,"nuevaclave")!=null,"contraseña editada en TXT se aplica");
        comprobar(entrar(carpeta,Rol.EMPLEADO,"empleado123")==null,"contraseña anterior deja de funcionar");
        Files.write(usuarios,Arrays.asList("GERENTE|gerente123","GERENTE|otra"),StandardCharsets.UTF_8);
        rechazar(IOException.class,()->new Autenticacion(carpeta),"usuarios duplicados rechazados");
        System.out.println("TOTAL: "+pruebas+" comprobaciones. Datos de prueba: "+carpeta);
    }
}
