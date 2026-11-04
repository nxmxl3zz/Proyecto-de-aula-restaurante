package restaurante;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import restaurante.datos.*;
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
            try(DatosRestaurante datos=new DatosRestaurante(Paths.get(args[1]))) {
                if(datos.getPedidos().size()!=2||datos.getPedidos().get(1).getNumero()!=2)throw new AssertionError("No se recuperó el historial en el nuevo proceso.");
            }return;
        }
        Path carpeta=Files.createTempDirectory("la-doble-datos-test-");
        Sesion empleado,gerente;
        Plato original=new Plato("H001","Hamburguesa ñ | especial","Línea uno\nLínea dos \\ salsa",10000,"Hamburguesas");
        List<DetallePedido> carrito=new ArrayList<>();carrito.add(new DetallePedido(original,2,10000));
        try(DatosRestaurante datos=new DatosRestaurante(carpeta)) {
            empleado=entrar(carpeta,Rol.EMPLEADO,"empleado123");gerente=entrar(carpeta,Rol.GERENTE,"gerente123");
            comprobar(empleado!=null&&!empleado.esGerente(),"autenticación de empleado");
            comprobar(gerente!=null&&gerente.esGerente(),"autenticación de gerente");
            comprobar(entrar(carpeta,Rol.GERENTE,"empleado123")==null,"contraseña no sirve para otro rol");
            comprobar(entrar(carpeta,Rol.EMPLEADO,"")==null,"contraseña vacía rechazada");
            comprobar(datos.getPedidos().isEmpty(),"primer inicio sin ventas falsas");
            rechazar(IOException.class,()->new DatosRestaurante(carpeta),"bloqueo contra dos aplicaciones simultáneas");
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
            rechazar(IOException.class,()->datos.registrarPedido(gerente,null,Arrays.asList(new DetallePedido(original,1,10000))),"error de escritura no confirma una venta");
            comprobar(datos.getPedidos().size()==1,"error de escritura no altera ventas en memoria");
            Files.delete(archivo.resolve("obstaculo"));Files.delete(archivo);Files.move(respaldo,archivo);
        }
        try(DatosRestaurante datos=new DatosRestaurante(carpeta)) {
            comprobar(datos.getPedidos().size()==1,"ventas recuperadas tras cerrar y abrir archivos");
            Pedido p=datos.getPedidos().get(0);
            comprobar(p.getMesa()==3&&p.calcularTotal()==20000,"mesa, fecha y total recuperados");
            comprobar(p.getDetalles().get(0).getPlato().getNombre().equals("Hamburguesa ñ | especial"),"acentos y separadores se conservan");
            comprobar(p.getDetalles().get(0).getPlato().getDescripcion().equals("Línea uno\nLínea dos \\ salsa"),"saltos de línea y barras se conservan");
            comprobar(datos.getInventario().get(0).getCantidad()==7.5&&datos.getInventario().get(0).getNombre().equals("# Pan | grande"),"inventario recuperado incluso si el nombre empieza con #");
            comprobar(datos.getMesas()[2],"ocupación de mesa recuperada");
            comprobar(datos.getMenu().get(0).getPrecio()==99999,"menú editado recuperado");
            Pedido segundo=datos.registrarPedido(gerente,null,Arrays.asList(new DetallePedido(original,1,15000)));
            comprobar(segundo.getNumero()==2,"numeración continúa después del reinicio");
        }
        Process hijo=new ProcessBuilder(Paths.get(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),DatosRestauranteTest.class.getName(),"leer",carpeta.toString()).inheritIO().start();
        comprobar(hijo.waitFor()==0,"persistencia comprobada en otro proceso Java");
        Path archivo=carpeta.resolve("pedidos.txt");byte[] originalArchivo=Files.readAllBytes(archivo);
        Files.write(archivo,Arrays.asList("P|1|2026-11-04|3|COBRADO|999"),StandardCharsets.UTF_8);
        byte[] corrupto=Files.readAllBytes(archivo);
        rechazar(IOException.class,()->new DatosRestaurante(carpeta),"archivo corrupto impide abrir en lugar de borrar ventas");
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
