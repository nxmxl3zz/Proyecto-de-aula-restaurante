package restaurante;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import restaurante.datos.*;
import restaurante.servicios.ServicioRestaurante;
import restaurante.entidades.*;
import restaurante.seguridad.*;

/** Reglas de recetas, consumos agregados y recuperación del cobro con inventario. */
public final class RecetasInventarioTest {
    private static int pruebas;
    private interface Accion {void ejecutar() throws Exception;}
    private static void comprobar(boolean ok,String mensaje){if(!ok)throw new AssertionError(mensaje);pruebas++;System.out.println("OK "+mensaje);}
    private static void rechazar(Accion accion,String mensaje)throws Exception {
        try{accion.ejecutar();}catch(IOException|IllegalArgumentException|SecurityException ex){comprobar(true,mensaje);return;}throw new AssertionError(mensaje);
    }
    private static Sesion sesion(ServicioRestaurante datos,Rol rol)throws Exception{return new Autenticacion(datos.getDirectorio()).autenticar(rol,(rol==Rol.GERENTE?"gerente123":"empleado123").toCharArray());}
    private static double cantidad(ServicioRestaurante datos,String nombre){return datos.getInventario().stream().filter(i->i.getNombre().equals(nombre)).findFirst().get().getCantidad();}
    private static DetallePedido detalle(ServicioRestaurante datos,String codigo,int cantidad){Plato p=datos.getMenu().stream().filter(x->x.getCodigo().equals(codigo)).findFirst().get();return new DetallePedido(p,cantidad,p.getPrecio());}
    public static void main(String[] args)throws Exception {
        Path carpeta=Files.createTempDirectory("la-doble-recetas-test-");
        try(ServicioRestaurante datos=new ServicioRestaurante(carpeta)) {
            DatosPrueba.stockClasico(datos);
            Sesion gerente=sesion(datos,Rol.GERENTE),empleado=sesion(datos,Rol.EMPLEADO);
            comprobar(cantidad(datos,"Pan hamburguesa")==20,"stock inicial: 20 panes");
            comprobar(cantidad(datos,"Medallones de carne")==40,"stock inicial: 40 medallones");
            comprobar(cantidad(datos,"Queso cheddar")==40,"stock inicial: 40 lonchas");
            comprobar(datos.getMenu().stream().allMatch(p->!datos.getReceta(p.getCodigo()).isEmpty()),"todos los platos de ejemplo tienen receta");
            datos.registrarPedido(empleado,1,Arrays.asList(detalle(datos,"H001",1)));
            comprobar(cantidad(datos,"Pan hamburguesa")==19,"La Doble descuenta un pan");
            comprobar(cantidad(datos,"Medallones de carne")==38,"La Doble descuenta exactamente dos medallones");
            comprobar(cantidad(datos,"Queso cheddar")==38,"La Doble descuenta exactamente dos lonchas");
            comprobar(cantidad(datos,"Cebolla caramelizada")==385&&cantidad(datos,"Salsa de la casa")==580,"La Doble descuenta cebolla y salsa");
            DetallePedido doble=detalle(datos,"H001",1);
            doble.setAdicionales(Arrays.asList(new AdicionalPedido(detalle(datos,"E003",1).getPlato(),3,4000),new AdicionalPedido(detalle(datos,"E002",1).getPlato(),2,5000)));
            datos.registrarPedido(empleado,null,Arrays.asList(detalle(datos,"C002",2),doble));
            comprobar(cantidad(datos,"Pan hamburguesa")==16,"combos y hamburguesas suman sus panes");
            comprobar(cantidad(datos,"Medallones de carne")==34,"dos clásicas y una doble consumen cuatro medallones");
            comprobar(cantidad(datos,"Queso cheddar")==31,"queso se suma entre combos, hamburguesas y extras");
            comprobar(cantidad(datos,"Papas")==3700&&cantidad(datos,"Aceite")==1470,"dos combos consumen dos porciones de papas y aceite");
            comprobar(cantidad(datos,"Gaseosa")==22,"dos combos consumen dos bebidas");
            datos.registrarPedido(empleado,null,Arrays.asList(detalle(datos,"B001",1),detalle(datos,"B002",1),detalle(datos,"B003",1)));
            comprobar(cantidad(datos,"Gaseosa")==21&&cantidad(datos,"Limonada")==3300&&cantidad(datos,"Agua")==11,"bebidas descuentan sus insumos");
            comprobar(cantidad(datos,"Tocineta")==540,"dos extras de tocineta descuentan 60 g");
            byte[] ventas=Files.readAllBytes(carpeta.resolve("pedidos.txt")),stock=Files.readAllBytes(carpeta.resolve("inventario.txt"));
            rechazar(()->datos.registrarPedido(empleado,null,Arrays.asList(detalle(datos,"H001",17))),"sin suficientes panes se rechaza el pedido");
            comprobar(Arrays.equals(ventas,Files.readAllBytes(carpeta.resolve("pedidos.txt")))&&Arrays.equals(stock,Files.readAllBytes(carpeta.resolve("inventario.txt"))),"stock insuficiente no altera ninguno de los dos archivos");
            comprobar(datos.getPedidos().size()==3&&cantidad(datos,"Pan hamburguesa")==16,"stock insuficiente no altera memoria ni consume número");
            List<Plato> menu=datos.getMenu();Plato personalizado=new Plato("N001","Triple","Tres carnes",30000,"Hamburguesas");menu.add(personalizado);datos.guardarMenu(gerente,menu);
            rechazar(()->datos.registrarPedido(empleado,null,Arrays.asList(new DetallePedido(personalizado,1,30000))),"plato sin receta no se vende sin descontar insumos");
            List<IngredienteReceta> receta=Arrays.asList(new IngredienteReceta("Pan hamburguesa","unidades",1),new IngredienteReceta("Medallones de carne","unidades",3));
            rechazar(()->datos.guardarReceta(empleado,"N001",receta),"empleado no puede cambiar recetas");
            datos.guardarReceta(gerente,"N001",receta);
            Pedido pedido=datos.registrarPedido(empleado,null,Arrays.asList(new DetallePedido(personalizado,2,30000)));
            comprobar(pedido.getNumero()==4&&cantidad(datos,"Medallones de carne")==28,"receta personalizada multiplica tres carnes por dos platos");
            rechazar(()->datos.guardarReceta(gerente,"N001",Collections.emptyList()),"receta vacía rechazada");
            rechazar(()->datos.guardarReceta(gerente,"N001",Arrays.asList(new IngredienteReceta("Medallones de carne","kg",1))),"unidad de receta incompatible rechazada");
            rechazar(()->datos.guardarReceta(gerente,"N001",Arrays.asList(receta.get(0),receta.get(0))),"ingrediente duplicado rechazado");
            datos.guardarReceta(gerente,"B002",Arrays.asList(new IngredienteReceta("Limonada","ml",0.1)));
            datos.registrarPedido(empleado,null,Arrays.asList(detalle(datos,"B002",3)));
            comprobar(cantidad(datos,"Limonada")==3299.7,"cantidades decimales sin errores de resta acumulados");
            rechazar(()->new IngredienteReceta("Carne","g",-1),"cantidades negativas no son recetas válidas");
            rechazar(()->new IngredienteReceta("Carne","g",Double.NaN),"NaN no es una cantidad de receta válida");
        }
        try(ServicioRestaurante datos=new ServicioRestaurante(carpeta)) {
            comprobar(cantidad(datos,"Medallones de carne")==28&&cantidad(datos,"Limonada")==3299.7,"consumos permanecen tras reiniciar");
            comprobar(datos.getReceta("N001").get(1).getCantidad()==3,"receta personalizada permanece tras reiniciar");
        }
        Path jornada=Files.createTempDirectory("la-doble-20-burgers-");
        try(ServicioRestaurante datos=new ServicioRestaurante(jornada)) {
            DatosPrueba.stockClasico(datos);
            Sesion gerente=sesion(datos,Rol.GERENTE);
            datos.registrarPedido(gerente,1,Arrays.asList(detalle(datos,"H001",20)));
            comprobar(cantidad(datos,"Pan hamburguesa")==0&&cantidad(datos,"Medallones de carne")==0&&cantidad(datos,"Queso cheddar")==0,"stock permite veinte dobles y se agota exactamente");
            rechazar(()->datos.registrarPedido(gerente,null,Arrays.asList(detalle(datos,"H001",1))),"no se permite una hamburguesa número 21 sin reponer");
        }
        try(ServicioRestaurante datos=new ServicioRestaurante(jornada)){comprobar(cantidad(datos,"Pan hamburguesa")==0,"reiniciar no repone automáticamente los insumos agotados");}
        // Simulate interruption between the two file replacements, before the commit marker is removed.
        Path recuperacion=Files.createTempDirectory("la-doble-cobro-recuperacion-");
        List<String> stockAnterior,ventasAnteriores;
        try(ServicioRestaurante datos=new ServicioRestaurante(recuperacion)) {
            DatosPrueba.stockClasico(datos);
            stockAnterior=ArchivosTxt.leer(recuperacion.resolve("inventario.txt"));ventasAnteriores=ArchivosTxt.leer(recuperacion.resolve("pedidos.txt"));
            datos.registrarPedido(sesion(datos,Rol.GERENTE),1,Arrays.asList(detalle(datos,"H001",1)));
        }
        List<String> diario=new ArrayList<>();for(String l:ventasAnteriores)diario.add(ArchivosTxt.fila("P",l));for(String l:stockAnterior)diario.add(ArchivosTxt.fila("I",l));
        ArchivosTxt.guardar(recuperacion.resolve("cobro_pendiente.txt"),diario);
        try(ServicioRestaurante datos=new ServicioRestaurante(recuperacion)) {
            comprobar(datos.getPedidos().isEmpty()&&cantidad(datos,"Medallones de carne")==40,"cobro interrumpido recupera pedidos y existencias juntos");
            comprobar(!Files.exists(recuperacion.resolve("cobro_pendiente.txt")),"diario se elimina después de recuperar");
            datos.registrarPedido(sesion(datos,Rol.GERENTE),1,Arrays.asList(detalle(datos,"H001",1)));
            comprobar(cantidad(datos,"Medallones de carne")==38,"tras recuperar un cobro válido descuenta una sola vez");
        }
        Path existente=Files.createTempDirectory("la-doble-stock-existente-");ArchivosTxt.guardar(existente.resolve("inventario.txt"),Arrays.asList("Insumo personal|kg|5"));
        try(ServicioRestaurante datos=new ServicioRestaurante(existente)){comprobar(cantidad(datos,"Insumo personal")==5,"la ampliación conserva los insumos personales y sus cantidades");}
        System.out.println("TOTAL RECETAS: "+pruebas+" comprobaciones.");
    }
}
