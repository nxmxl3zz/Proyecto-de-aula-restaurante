package restaurante.datos;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import restaurante.entidades.*;
import restaurante.seguridad.Sesion;

/** Archivos de la aplicación. No actualiza la memoria hasta completar la escritura. */
public final class DatosRestaurante implements AutoCloseable {
    private final Path directorio;
    private final FileChannel canal;
    private final FileLock bloqueo;
    private final List<Pedido> pedidos=new ArrayList<>();
    private final List<Insumo> inventario=new ArrayList<>();
    private final List<Plato> menu=new ArrayList<>();
    private final Map<String,List<IngredienteReceta>> recetas=new LinkedHashMap<>();
    private boolean recuperacionPendiente;
    private final boolean[] mesas=new boolean[10];

    public static Path rutaPredeterminada() {return Paths.get(System.getProperty("restaurante.datos","datos")).toAbsolutePath().normalize();}
    public DatosRestaurante(Path directorio) throws IOException {
        this.directorio=directorio.toAbsolutePath().normalize();Files.createDirectories(this.directorio);
        canal=FileChannel.open(this.directorio.resolve(".bloqueo"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        FileLock adquirido;
        try {adquirido=canal.tryLock();}catch(OverlappingFileLockException ex) {canal.close();throw new IOException("La carpeta de datos ya está abierta en otra sesión.",ex);}
        if(adquirido==null) {canal.close();throw new IOException("Otra aplicación está usando la carpeta de datos.");}
        bloqueo=adquirido;
        try {
            recuperarCobro();
            boolean versionAnterior=!Files.exists(archivo("recetas.txt"));
            if(!Files.exists(archivo("inventario.txt")) || (versionAnterior && filas("inventario.txt").isEmpty()))
                ArchivosTxt.guardar(archivo("inventario.txt"),ejemplo("inventario.txt"));
            inicializar("recetas.txt",ejemplo("recetas.txt"));
            inicializar("pedidos.txt",Arrays.asList("# La Doble v1 · P: pedido cobrado; D: detalle"));
            inicializar("mesas.txt",Arrays.asList("# mesa|ocupada (true/false)","1|false","2|false","3|false","4|false","5|false","6|false","7|false","8|false","9|false","10|false"));
            if(!Files.exists(archivo("menu.txt"))) guardarMenuInterno(CatalogoMenu.getPlatos());
            cargarMenu();cargarInventario();cargarRecetas();cargarMesas();cargarPedidos();
        } catch(IOException|RuntimeException ex) {
            close();throw new IOException("No se pudieron leer los datos de "+this.directorio+". No se sobrescribieron los archivos existentes. "+ex.getMessage(),ex);
        }
    }
    private List<String> ejemplo(String nombre) throws IOException {
        try(java.io.InputStream entrada=DatosRestaurante.class.getResourceAsStream("/restaurante/config/"+nombre)) {
            if(entrada==null)throw new IOException("No se encontró la configuración inicial: "+nombre);
            List<String> lineas=new ArrayList<>();
            try(java.io.BufferedReader lector=new java.io.BufferedReader(new java.io.InputStreamReader(entrada,java.nio.charset.StandardCharsets.UTF_8))) {
                String linea;while((linea=lector.readLine())!=null)lineas.add(linea);
            }return lineas;
        }
    }
    private void exigirEscritura() throws IOException {
        exigir(!recuperacionPendiente,"Hay una recuperación de cobro pendiente. Corrige el archivo bloqueado y reinicia la aplicación.");
    }
    private Path archivo(String nombre) {return directorio.resolve(nombre);}
    public Path getDirectorio() {return directorio;}
    private void inicializar(String nombre,List<String> lineas) throws IOException {if(!Files.exists(archivo(nombre))) ArchivosTxt.guardar(archivo(nombre),lineas);}
    private List<List<String>> filas(String nombre) throws IOException {
        List<List<String>> resultado=new ArrayList<>();int numero=0;
        for(String linea:ArchivosTxt.leer(archivo(nombre))) {
            numero++;if(linea.trim().isEmpty()||linea.startsWith("#")) continue;
            try {resultado.add(ArchivosTxt.campos(linea));}catch(IOException ex) {throw new IOException(nombre+", línea "+numero+": "+ex.getMessage(),ex);}
        }return resultado;
    }
    private static void exigir(boolean condicion,String mensaje) throws IOException {if(!condicion) throw new IOException(mensaje);}
    private static int entero(String texto) throws IOException {try{return Integer.parseInt(texto);}catch(NumberFormatException ex){throw new IOException("Número entero inválido: "+texto,ex);}}
    private static double decimal(String texto) throws IOException {
        try {double valor=Double.parseDouble(texto);exigir(Double.isFinite(valor)&&valor>=0,"Cantidad inválida: "+texto);return valor;}
        catch(NumberFormatException ex){throw new IOException("Número inválido: "+texto,ex);}
    }
    private static Plato copiar(Plato p) {return new Plato(p.getCodigo(),p.getNombre(),p.getDescripcion(),p.getPrecio(),p.getCategoria());}
    private static Pedido copiar(Pedido p) {
        ArrayList<DetallePedido> detalles=new ArrayList<>();
        for(DetallePedido d:p.getDetalles()) detalles.add(new DetallePedido(copiar(d.getPlato()),d.getCantidad(),d.getPrecioUnitario()));
        Pedido copia=new Pedido(p.getNumero(),p.getFecha(),null,null,p.getEstado(),detalles);copia.setMesa(p.getMesa());return copia;
    }
    public synchronized List<Pedido> getPedidos() {List<Pedido> copia=new ArrayList<>();for(Pedido p:pedidos)copia.add(copiar(p));return Collections.unmodifiableList(copia);}
    public synchronized List<Plato> getMenu() {List<Plato> copia=new ArrayList<>();for(Plato p:menu)copia.add(copiar(p));return copia;}
    public synchronized List<Insumo> getInventario() {return new ArrayList<>(inventario);}
    public synchronized boolean[] getMesas() {return mesas.clone();}
    private static void autenticar(Sesion sesion) {if(sesion==null)throw new SecurityException("Inicia sesión para continuar.");}
    private static void gerente(Sesion sesion) {autenticar(sesion);if(!sesion.esGerente())throw new SecurityException("Acceso exclusivo del gerente.");}

    public synchronized Pedido registrarPedido(Sesion sesion,Integer mesa,List<DetallePedido> carrito) throws IOException {
        autenticar(sesion);exigirEscritura();exigir(!carrito.isEmpty(),"No se puede guardar un pedido vacío.");
        exigir(mesa==null||(mesa>=1&&mesa<=10),"Mesa inválida.");
        int ultimo=pedidos.stream().mapToInt(Pedido::getNumero).max().orElse(0);exigir(ultimo<Integer.MAX_VALUE,"Se agotó la numeración de pedidos.");
        ArrayList<DetallePedido> detalles=new ArrayList<>();
        for(DetallePedido d:carrito) {
            exigir(d.getPlato()!=null&&d.getCantidad()>0&&Double.isFinite(d.getPrecioUnitario())&&d.getPrecioUnitario()>0,"Detalle de pedido inválido.");
            detalles.add(new DetallePedido(copiar(d.getPlato()),d.getCantidad(),d.getPrecioUnitario()));
        }
        Pedido pedido=new Pedido(ultimo+1,LocalDate.now(),null,null,EstadoPedido.PENDIENTE,detalles);pedido.setMesa(mesa);
        exigir(Double.isFinite(pedido.calcularTotal()),"El total del pedido es demasiado grande.");
        List<Insumo> existencias=descontarIngredientes(carrito);
        List<Pedido> nuevos=new ArrayList<>(pedidos);nuevos.add(pedido);
        guardarCobro(nuevos,existencias);
        pedidos.add(pedido);inventario.clear();inventario.addAll(existencias);return copiar(pedido);
    }
    private List<String> lineasPedidos(List<Pedido> lista) {
        List<String> lineas=new ArrayList<>();lineas.add("# La Doble v1 · P|numero|fecha|mesa|pago|total; D|pedido|codigo|nombre|descripcion|categoria|cantidad|precio");
        for(Pedido p:lista) {
            lineas.add(ArchivosTxt.fila("P",p.getNumero(),p.getFecha(),p.getMesa()==null?"":p.getMesa(),"COBRADO",p.calcularTotal()));
            for(DetallePedido d:p.getDetalles())lineas.add(ArchivosTxt.fila("D",p.getNumero(),d.getPlato().getCodigo(),d.getPlato().getNombre(),d.getPlato().getDescripcion(),d.getPlato().getCategoria(),d.getCantidad(),d.getPrecioUnitario()));
        }return lineas;
    }
    private void cargarPedidos() throws IOException {
        Map<Integer,Pedido> leidos=new LinkedHashMap<>();Map<Integer,Double> totales=new HashMap<>();
        for(List<String> c:filas("pedidos.txt")) {
            if("P".equals(c.get(0))) {
                exigir(c.size()==6,"Cabecera de pedido inválida.");int id=entero(c.get(1));
                exigir(id>0&&!leidos.containsKey(id)&&"COBRADO".equals(c.get(4)),"Número de pedido duplicado o pago inválido.");
                Pedido p=new Pedido(id,LocalDate.parse(c.get(2)),null,null,EstadoPedido.PENDIENTE,new ArrayList<>());
                if(!c.get(3).isEmpty()){int mesa=entero(c.get(3));exigir(mesa>=1&&mesa<=10,"Mesa inválida.");p.setMesa(mesa);}
                leidos.put(id,p);totales.put(id,decimal(c.get(5)));
            } else if("D".equals(c.get(0))) {
                exigir(c.size()==8,"Detalle de pedido inválido.");Pedido p=leidos.get(entero(c.get(1)));exigir(p!=null,"Detalle sin cabecera.");
                int cantidad=entero(c.get(6));double precio=decimal(c.get(7));exigir(cantidad>0&&precio>0,"Cantidad o precio de venta inválido.");
                p.agregarDetalle(new DetallePedido(new Plato(c.get(2),c.get(3),c.get(4),precio,c.get(5)),cantidad,precio));
            } else throw new IOException("Registro de pedido desconocido.");
        }
        for(Pedido p:leidos.values())exigir(!p.getDetalles().isEmpty()&&Double.isFinite(p.calcularTotal())&&Math.abs(p.calcularTotal()-totales.get(p.getNumero()))<0.005,"Total o detalles inconsistentes en el pedido "+p.getNumero());
        pedidos.addAll(leidos.values());
    }
    public synchronized void guardarMenu(Sesion sesion,List<Plato> platos) throws IOException {
        gerente(sesion);exigirEscritura();validarMenu(platos);guardarMenuInterno(platos);menu.clear();for(Plato p:platos)menu.add(copiar(p));
    }
    private void validarMenu(List<Plato> platos) throws IOException {
        Set<String> codigos=new HashSet<>();for(Plato p:platos) {
            exigir(p.getCodigo()!=null&&!p.getCodigo().trim().isEmpty()&&codigos.add(p.getCodigo().toLowerCase(Locale.ROOT)),"Código de plato inválido o duplicado.");
            exigir(p.getNombre()!=null&&!p.getNombre().trim().isEmpty()&&p.getDescripcion()!=null&&!p.getDescripcion().trim().isEmpty(),"Nombre o descripción vacía.");
            exigir(CatalogoMenu.getCategorias().contains(p.getCategoria())&&Double.isFinite(p.getPrecio())&&p.getPrecio()>0,"Categoría o precio inválido.");
        }
    }
    private void guardarMenuInterno(List<Plato> platos) throws IOException {
        List<String> lineas=new ArrayList<>();lineas.add("# codigo|nombre|descripcion|precio|categoria");
        for(Plato p:platos)lineas.add(ArchivosTxt.fila(p.getCodigo(),p.getNombre(),p.getDescripcion(),p.getPrecio(),p.getCategoria()));ArchivosTxt.guardar(archivo("menu.txt"),lineas);
    }
    private void cargarMenu() throws IOException {
        for(List<String> c:filas("menu.txt")){exigir(c.size()==5,"Registro de menú inválido.");menu.add(new Plato(c.get(0),c.get(1),c.get(2),decimal(c.get(3)),c.get(4)));}validarMenu(menu);
    }
    public synchronized void guardarInventario(Sesion sesion,List<Insumo> insumos) throws IOException {
        gerente(sesion);exigirEscritura();List<String> lineas=new ArrayList<>();lineas.add("# nombre|unidad|cantidad");Set<String> nombres=new HashSet<>();
        for(Insumo i:insumos){exigir(nombres.add(i.getNombre().toLowerCase(Locale.ROOT)),"Insumo duplicado.");lineas.add(ArchivosTxt.fila(i.getNombre(),i.getUnidad(),i.getCantidad()));}
        ArchivosTxt.guardar(archivo("inventario.txt"),lineas);inventario.clear();inventario.addAll(insumos);
    }
    private void cargarInventario() throws IOException {
        Set<String> nombres=new HashSet<>();for(List<String> c:filas("inventario.txt")) {
            exigir(c.size()==3,"Registro de inventario inválido.");Insumo i=new Insumo(c.get(0),c.get(1),decimal(c.get(2)));
            exigir(nombres.add(i.getNombre().toLowerCase(Locale.ROOT)),"Insumo duplicado.");inventario.add(i);
        }
    }
    private static String clave(String texto) {return texto.trim().toLowerCase(Locale.ROOT);}
    public synchronized List<IngredienteReceta> getReceta(String codigo) {
        List<IngredienteReceta> receta=recetas.get(clave(codigo));
        return receta==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(receta));
    }
    private void cargarRecetas() throws IOException {
        for(List<String> c:filas("recetas.txt")) {
            exigir(c.size()==4&&!c.get(0).trim().isEmpty(),"Registro de receta inválido.");
            List<IngredienteReceta> lista=recetas.computeIfAbsent(clave(c.get(0)),k->new ArrayList<>());
            for(IngredienteReceta i:lista)exigir(!clave(i.getInsumo()).equals(clave(c.get(1))),"Ingrediente duplicado en una receta.");
            lista.add(new IngredienteReceta(c.get(1),c.get(2),decimal(c.get(3))));
        }
    }
    public synchronized void guardarReceta(Sesion sesion,String codigo,List<IngredienteReceta> ingredientes) throws IOException {
        gerente(sesion);exigirEscritura();
        exigir(menu.stream().anyMatch(p->clave(p.getCodigo()).equals(clave(codigo))),"El plato no existe en el menú.");
        exigir(!ingredientes.isEmpty(),"Selecciona al menos un insumo para el plato.");
        Set<String> vistos=new HashSet<>();
        for(IngredienteReceta i:ingredientes) {
            exigir(vistos.add(clave(i.getInsumo())),"Ingrediente duplicado.");
            Insumo stock=buscarInsumo(i.getInsumo());
            exigir(stock!=null&&clave(stock.getUnidad()).equals(clave(i.getUnidad())),"El insumo o la unidad no coinciden con el inventario: "+i.getInsumo());
        }
        Map<String,List<IngredienteReceta>> nuevas=new LinkedHashMap<>(recetas);nuevas.put(clave(codigo),new ArrayList<>(ingredientes));
        List<String> lineas=new ArrayList<>();lineas.add("# codigo_plato|insumo|unidad|cantidad_por_plato");
        for(Map.Entry<String,List<IngredienteReceta>> entrada:nuevas.entrySet())for(IngredienteReceta i:entrada.getValue())
            lineas.add(ArchivosTxt.fila(entrada.getKey(),i.getInsumo(),i.getUnidad(),i.getCantidad()));
        ArchivosTxt.guardar(archivo("recetas.txt"),lineas);recetas.clear();recetas.putAll(nuevas);
    }
    private Insumo buscarInsumo(String nombre) {
        for(Insumo i:inventario)if(clave(i.getNombre()).equals(clave(nombre)))return i;return null;
    }
    private List<Insumo> descontarIngredientes(List<DetallePedido> carrito) throws IOException {
        Map<String,java.math.BigDecimal> cantidades=new LinkedHashMap<>();
        for(DetallePedido detalle:carrito) {
            List<IngredienteReceta> receta=getReceta(detalle.getPlato().getCodigo());
            exigir(!receta.isEmpty(),"Falta configurar la receta de "+detalle.getPlato().getNombre()+". El gerente puede hacerlo desde Menú > Receta / insumos.");
            for(IngredienteReceta ingrediente:receta) {
                Insumo stock=buscarInsumo(ingrediente.getInsumo());
                exigir(stock!=null,"Falta el insumo: "+ingrediente.getInsumo());
                exigir(clave(stock.getUnidad()).equals(clave(ingrediente.getUnidad())),"Unidad incorrecta para "+ingrediente.getInsumo()+". La receta requiere "+ingrediente.getUnidad()+".");
                java.math.BigDecimal consumo=java.math.BigDecimal.valueOf(ingrediente.getCantidad()).multiply(java.math.BigDecimal.valueOf(detalle.getCantidad()));
                String nombre=clave(ingrediente.getInsumo());
                cantidades.put(nombre,cantidades.getOrDefault(nombre,java.math.BigDecimal.ZERO).add(consumo));
            }
        }
        List<Insumo> resultado=new ArrayList<>();
        for(Insumo stock:inventario) {
            java.math.BigDecimal requerido=cantidades.getOrDefault(clave(stock.getNombre()),java.math.BigDecimal.ZERO);
            java.math.BigDecimal actual=java.math.BigDecimal.valueOf(stock.getCantidad());
            exigir(actual.compareTo(requerido)>=0,"Existencias insuficientes de "+stock.getNombre()+": disponibles "+actual.stripTrailingZeros().toPlainString()+" "+stock.getUnidad()+", necesarios "+requerido.stripTrailingZeros().toPlainString()+".");
            resultado.add(new Insumo(stock.getNombre(),stock.getUnidad(),actual.subtract(requerido).doubleValue()));
        }return resultado;
    }
    private List<String> lineasInventario(List<Insumo> lista) {
        List<String> lineas=new ArrayList<>();lineas.add("# nombre|unidad|cantidad");
        for(Insumo i:lista)lineas.add(ArchivosTxt.fila(i.getNombre(),i.getUnidad(),i.getCantidad()));return lineas;
    }
    /** El diario conserva el estado anterior: si el proceso se interrumpe se revierte el cobro incompleto. */
    private void guardarCobro(List<Pedido> ventas,List<Insumo> stock) throws IOException {
        List<String> anterioresPedidos=ArchivosTxt.leer(archivo("pedidos.txt"));
        List<String> anterioresStock=ArchivosTxt.leer(archivo("inventario.txt"));
        List<String> diario=new ArrayList<>();diario.add("# Cobro pendiente: copias anteriores para recuperación");
        for(String linea:anterioresPedidos)diario.add(ArchivosTxt.fila("P",linea));
        for(String linea:anterioresStock)diario.add(ArchivosTxt.fila("I",linea));
        ArchivosTxt.guardar(archivo("cobro_pendiente.txt"),diario);
        try {
            ArchivosTxt.guardar(archivo("inventario.txt"),lineasInventario(stock));
            ArchivosTxt.guardar(archivo("pedidos.txt"),lineasPedidos(ventas));
            Files.delete(archivo("cobro_pendiente.txt"));
        }catch(IOException error) {
            recuperacionPendiente=true;
            try {
                ArchivosTxt.guardar(archivo("inventario.txt"),anterioresStock);
                ArchivosTxt.guardar(archivo("pedidos.txt"),anterioresPedidos);
                Files.delete(archivo("cobro_pendiente.txt"));recuperacionPendiente=false;
            }catch(IOException recuperacion){error.addSuppressed(recuperacion);}
            throw error;
        }
    }
    private void recuperarCobro() throws IOException {
        if(!Files.exists(archivo("cobro_pendiente.txt")))return;
        List<String> anterioresPedidos=new ArrayList<>(),anterioresStock=new ArrayList<>();
        for(List<String> c:filas("cobro_pendiente.txt")) {
            exigir(c.size()==2,"Diario de cobro inválido.");
            if("P".equals(c.get(0)))anterioresPedidos.add(c.get(1));
            else if("I".equals(c.get(0)))anterioresStock.add(c.get(1));
            else throw new IOException("Tipo de registro de recuperación inválido.");
        }
        exigir(!anterioresPedidos.isEmpty()&&!anterioresStock.isEmpty(),"Diario de cobro incompleto.");
        ArchivosTxt.guardar(archivo("inventario.txt"),anterioresStock);
        ArchivosTxt.guardar(archivo("pedidos.txt"),anterioresPedidos);
        Files.delete(archivo("cobro_pendiente.txt"));
    }
    public synchronized void guardarMesa(Sesion sesion,int numero,boolean ocupada) throws IOException {
        autenticar(sesion);exigirEscritura();exigir(numero>=1&&numero<=10,"Mesa inválida.");boolean[] nuevos=mesas.clone();nuevos[numero-1]=ocupada;
        List<String> lineas=new ArrayList<>();lineas.add("# mesa|ocupada (true/false)");for(int i=0;i<10;i++)lineas.add(ArchivosTxt.fila(i+1,nuevos[i]));
        ArchivosTxt.guardar(archivo("mesas.txt"),lineas);mesas[numero-1]=ocupada;
    }
    private void cargarMesas() throws IOException {
        Set<Integer> vistas=new HashSet<>();for(List<String> c:filas("mesas.txt")) {
            exigir(c.size()==2,"Registro de mesa inválido.");int numero=entero(c.get(0));exigir(numero>=1&&numero<=10&&vistas.add(numero),"Mesa inválida o duplicada.");
            exigir("true".equals(c.get(1))||"false".equals(c.get(1)),"Estado de mesa inválido.");mesas[numero-1]=Boolean.parseBoolean(c.get(1));
        }exigir(vistas.size()==10,"El archivo debe contener las diez mesas.");
    }
    @Override public void close() throws IOException {try {if(bloqueo.isValid())bloqueo.release();}finally{canal.close();}}
}
