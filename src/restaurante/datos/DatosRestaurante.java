package restaurante.datos;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import restaurante.entidades.*;

/**
 * Repositorio TXT: serializa, recupera y bloquea archivos. Publica cambios en memoria después de
 * escribir.
 */
public final class DatosRestaurante implements RepositorioRestaurante {
    private final Path directorio;
    private final FileChannel canal;
    private final FileLock bloqueo;
    private final List<Pedido> pedidos = new ArrayList<>();
    private final List<Pedido> abiertos = new ArrayList<>();
    private final List<Insumo> inventario = new ArrayList<>();
    private final List<Plato> menu = new ArrayList<>();
    private final Map<String, List<IngredienteReceta>> recetas = new LinkedHashMap<>();
    private boolean recuperacionPendiente;
    private final boolean[] mesas = new boolean[10];
    private final int[] sillas = new int[10];
    private final int[] grupos = new int[10];

    public static Path rutaPredeterminada() {
        return Paths.get(System.getProperty("restaurante.datos", "datos"))
                .toAbsolutePath()
                .normalize();
    }
    public DatosRestaurante(Path directorio) throws IOException {
        this.directorio = directorio.toAbsolutePath().normalize();
        Files.createDirectories(this.directorio);
        canal = FileChannel.open(this.directorio.resolve(".bloqueo"), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        FileLock adquirido;
        try {
            adquirido = canal.tryLock();
        } catch (OverlappingFileLockException ex) {
            canal.close();
            throw new IOException("La carpeta de datos ya está abierta en otra sesión.", ex);
        }
        if (adquirido == null) {
            canal.close();
            throw new IOException("Otra aplicación está usando la carpeta de datos.");
        }
        bloqueo = adquirido;
        try {
            recuperarCobro();
            boolean versionAnterior = !Files.exists(archivo("recetas.txt"));
            if (!Files.exists(archivo("inventario.txt"))
                    || (versionAnterior && filas("inventario.txt").isEmpty()))
                ArchivosTxt.guardar(archivo("inventario.txt"), ejemplo("inventario.txt"));
            inicializar("recetas.txt", ejemplo("recetas.txt"));
            inicializar(
                    "pedidos.txt", Arrays.asList("# La Doble v1 · P: pedido cobrado; D: detalle"));
            inicializar("pedidos_abiertos.txt",
                    Arrays.asList("# Pedidos abiertos; no consumen inventario"));
            inicializar("mesas.txt",
                    Arrays.asList("# mesa|ocupada (true/false)", "1|false", "2|false", "3|false",
                            "4|false", "5|false", "6|false", "7|false", "8|false", "9|false",
                            "10|false"));
            if (!Files.exists(archivo("menu.txt")))
                guardarMenuInterno(CatalogoMenu.getPlatos());
            cargarMenu();
            cargarInventario();
            cargarRecetas();
            cargarMesas();
            cargarPedidos();
            cargarPedidos("pedidos_abiertos.txt", abiertos, EstadoPedido.PENDIENTE);
            Set<Integer> atenciones = new HashSet<>();
            for (Pedido abierto : abiertos) {
                if (abierto.getMesa() == null)
                    continue;
                int grupo = grupos[abierto.getMesa() - 1];
                exigir(atenciones.add(grupo),
                        "Hay dos pedidos abiertos para el mismo grupo de mesas.");
                List<Integer> miembros = new ArrayList<>();
                for (Mesa m : getDetalleMesas())
                    if (m.getGrupo() == grupo) {
                        exigir(m.estaOcupada(),
                                "Un pedido abierto está asociado a una mesa libre.");
                        miembros.add(m.getNumero());
                    }
                exigir(miembros.equals(abierto.getMesas()),
                        "Las mesas del pedido abierto no coinciden con su grupo.");
            }
            if (AmpliacionMenu.aplicar(this.directorio, menu, inventario, recetas)) {
                menu.clear();
                inventario.clear();
                recetas.clear();
                cargarMenu();
                cargarInventario();
                cargarRecetas();
            }
        } catch (IOException | RuntimeException ex) {
            try {
                close();
            } catch (IOException cierre) {
                ex.addSuppressed(cierre);
            }
            throw new IOException("No se pudieron leer los datos de " + this.directorio
                            + ". No se sobrescribieron los archivos existentes. "
                            + ex.getMessage(),
                    ex);
        }
    }
    private List<String> ejemplo(String nombre) throws IOException {
        try (java.io.InputStream entrada = DatosRestaurante.class.getResourceAsStream(
                     "/restaurante/config/" + nombre)) {
            if (entrada == null)
                throw new IOException("No se encontró la configuración inicial: " + nombre);
            List<String> lineas = new ArrayList<>();
            try (java.io.BufferedReader lector =
                            new java.io.BufferedReader(new java.io.InputStreamReader(
                                    entrada, java.nio.charset.StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) lineas.add(linea);
            }
            return lineas;
        }
    }
    private void exigirEscritura() throws IOException {
        exigir(!recuperacionPendiente,
                "Hay una recuperación de cobro pendiente. Corrige el archivo bloqueado y reinicia "
                        + "la aplicación.");
    }
    private Path archivo(String nombre) {
        return directorio.resolve(nombre);
    }
    public Path getDirectorio() {
        return directorio;
    }
    private void inicializar(String nombre, List<String> lineas) throws IOException {
        if (!Files.exists(archivo(nombre)))
            ArchivosTxt.guardar(archivo(nombre), lineas);
    }
    private List<List<String>> filas(String nombre) throws IOException {
        List<List<String>> resultado = new ArrayList<>();
        int numero = 0;
        for (String linea : ArchivosTxt.leer(archivo(nombre))) {
            numero++;
            if (linea.trim().isEmpty() || linea.startsWith("#"))
                continue;
            try {
                resultado.add(ArchivosTxt.campos(linea));
            } catch (IOException ex) {
                throw new IOException(nombre + ", línea " + numero + ": " + ex.getMessage(), ex);
            }
        }
        return resultado;
    }
    private static void exigir(boolean condicion, String mensaje) throws IOException {
        if (!condicion)
            throw new IOException(mensaje);
    }
    private static int entero(String texto) throws IOException {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            throw new IOException("Número entero inválido: " + texto, ex);
        }
    }
    private static double decimal(String texto) throws IOException {
        try {
            double valor = Double.parseDouble(texto);
            exigir(Double.isFinite(valor) && valor >= 0, "Cantidad inválida: " + texto);
            return valor;
        } catch (NumberFormatException ex) {
            throw new IOException("Número inválido: " + texto, ex);
        }
    }
    private static Pedido copiar(Pedido p) {
        ArrayList<DetallePedido> detalles = new ArrayList<>();
        for (DetallePedido d : p.getDetalles()) detalles.add(d.copia());
        Pedido copia = new Pedido(p.getNumero(), p.getFecha(), null, null, p.getEstado(), detalles);
        copia.setMesas(p.getMesas());
        if (p.getMetodoPago() != null)
            copia.setPago(p.getMetodoPago(), p.getEfectivoRecibido());
        return copia;
    }
    public synchronized List<Pedido> getPedidos() {
        List<Pedido> copia = new ArrayList<>();
        for (Pedido p : pedidos) copia.add(copiar(p));
        return Collections.unmodifiableList(copia);
    }
    public synchronized List<Pedido> getPedidosAbiertos() {
        List<Pedido> copia = new ArrayList<>();
        for (Pedido p : abiertos) copia.add(copiar(p));
        return Collections.unmodifiableList(copia);
    }
    public synchronized List<Plato> getMenu() {
        List<Plato> copia = new ArrayList<>();
        for (Plato p : menu) copia.add(p.copia());
        return copia;
    }
    public synchronized List<Insumo> getInventario() {
        return new ArrayList<>(inventario);
    }
    public synchronized boolean[] getMesas() {
        return mesas.clone();
    }
    public synchronized List<Mesa> getDetalleMesas() {
        List<Mesa> resultado = new ArrayList<>();
        for (int i = 0; i < 10; i++) resultado.add(new Mesa(i + 1, sillas[i], mesas[i], grupos[i]));
        return Collections.unmodifiableList(resultado);
    }

    @Override
    public synchronized Pedido registrarCobro(Pedido pedido, List<Insumo> existencias)
            throws IOException {
        return registrarCobro(pedido, existencias, null);
    }
    @Override
    public synchronized Pedido registrarCobro(
            Pedido pedido, List<Insumo> existencias, Integer abierto) throws IOException {
        exigirEscritura();
        List<Pedido> nuevos = new ArrayList<>(pedidos);
        nuevos.add(copiar(pedido));
        List<Pedido> pendientes = new ArrayList<>(abiertos);
        if (abierto != null)
            exigir(pendientes.removeIf(p -> p.getNumero() == abierto),
                    "El pedido abierto ya no existe.");
        guardarOperacion(nuevos, existencias, pendientes, getDetalleMesas());
        pedidos.add(copiar(pedido));
        inventario.clear();
        inventario.addAll(existencias);
        abiertos.clear();
        abiertos.addAll(pendientes);
        return copiar(pedido);
    }
    @Override
    public synchronized void guardarAtencion(List<Pedido> pendientes, List<Mesa> nuevasMesas)
            throws IOException {
        exigirEscritura();
        guardarOperacion(pedidos, inventario, pendientes, nuevasMesas);
        abiertos.clear();
        for (Pedido p : pendientes) abiertos.add(copiar(p));
        aplicarMesas(nuevasMesas);
    }
    private List<String> lineasPedidos(List<Pedido> lista) {
        List<String> lineas = new ArrayList<>();
        lineas.add("# La Doble v1 · P|numero|fecha|mesa|estado|total|mesas|metodo|recibido; "
                + "D|pedido|codigo|nombre|descripcion|categoria|cantidad|precio");
        for (Pedido p : lista) {
            lineas.add(ArchivosTxt.fila("P", p.getNumero(), p.getFecha(),
                    p.getMesa() == null ? "" : p.getMesa(), p.getEstado(), p.calcularTotal(),
                    p.getMesas()
                            .stream()
                            .map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(",")),
                    p.getMetodoPago() == null ? "" : p.getMetodoPago().name(),
                    p.getMetodoPago() == null ? "" : p.getEfectivoRecibido()));
            int indice = 0;
            for (DetallePedido d : p.getDetalles()) {
                lineas.add(ArchivosTxt.fila("D", p.getNumero(), d.getPlato().getCodigo(),
                        d.getPlato().getNombre(), d.getPlato().getDescripcion(),
                        d.getPlato().getCategoria(), d.getCantidad(), d.getPrecioUnitario(),
                        d.getComensal(), d.getObservaciones()));
                for (AdicionalPedido adicional : d.getAdicionales()) {
                    Plato extra = adicional.getPlato();
                    lineas.add(ArchivosTxt.fila("X", p.getNumero(), indice, extra.getCodigo(),
                            extra.getNombre(), extra.getDescripcion(), extra.getCategoria(),
                            adicional.getCantidad(), adicional.getPrecioUnitario()));
                }
                for (String insumo : d.getSinIngredientes())
                    lineas.add(ArchivosTxt.fila("O", p.getNumero(), indice, insumo));
                indice++;
            }
        }
        return lineas;
    }
    /** Los cobrados solo admiten COBRADO; los abiertos, los estados de cocina. */
    private static EstadoPedido estadoValido(EstadoPedido esperado, String texto) {
        for (EstadoPedido e : EstadoPedido.values()) {
            if (!e.name().equals(texto))
                continue;
            boolean cocina = e == EstadoPedido.PENDIENTE || e == EstadoPedido.EN_PREPARACION
                    || e == EstadoPedido.SERVIDO;
            return esperado == EstadoPedido.COBRADO ? (e == EstadoPedido.COBRADO ? e : null)
                                                    : (cocina ? e : null);
        }
        return null;
    }
    private void cargarPedidos() throws IOException {
        cargarPedidos("pedidos.txt", pedidos, EstadoPedido.COBRADO);
    }
    private void cargarPedidos(String nombre, List<Pedido> destino, EstadoPedido estado)
            throws IOException {
        Map<Integer, Pedido> leidos = new LinkedHashMap<>();
        Map<Integer, Double> totales = new HashMap<>();
        for (List<String> c : filas(nombre)) {
            if ("P".equals(c.get(0))) {
                exigir(c.size() == 6 || c.size() == 7 || c.size() == 9, "Cabecera de pedido inválida.");
                int id = entero(c.get(1));
                EstadoPedido leido = estadoValido(estado, c.get(4));
                exigir(id > 0 && !leidos.containsKey(id) && leido != null,
                        "Número de pedido duplicado o estado inválido.");
                Pedido p = new Pedido(
                        id, LocalDate.parse(c.get(2)), null, null, leido, new ArrayList<>());
                if (!c.get(3).isEmpty()) {
                    int mesa = entero(c.get(3));
                    exigir(mesa >= 1 && mesa <= 10, "Mesa inválida.");
                    p.setMesa(mesa);
                }
                if (c.size() == 9 && !c.get(7).isEmpty()) {
                    MetodoPago metodo;
                    try {
                        metodo = MetodoPago.valueOf(c.get(7));
                    } catch (IllegalArgumentException ex) {
                        throw new IOException("Método de pago inválido: " + c.get(7), ex);
                    }
                    p.setPago(metodo, c.get(8).isEmpty() ? 0 : decimal(c.get(8)));
                }
                if (c.size() >= 7) {
                    List<Integer> numeros = new ArrayList<>();
                    if (!c.get(6).isEmpty())
                        for (String numero : c.get(6).split(",", -1)) numeros.add(entero(numero));
                    exigir(Objects.equals(p.getMesa(), numeros.isEmpty() ? null : numeros.get(0)),
                            "Mesa principal inconsistente.");
                    p.setMesas(numeros);
                }
                leidos.put(id, p);
                totales.put(id, decimal(c.get(5)));
            } else if ("D".equals(c.get(0))) {
                exigir(c.size() == 8 || c.size() == 10, "Detalle de pedido inválido.");
                Pedido p = leidos.get(entero(c.get(1)));
                exigir(p != null, "Detalle sin cabecera.");
                int cantidad = entero(c.get(6));
                double precio = decimal(c.get(7));
                exigir(cantidad > 0 && precio > 0, "Cantidad o precio de venta inválido.");
                DetallePedido detalle =
                        new DetallePedido(new Plato(c.get(2), c.get(3), c.get(4), precio, c.get(5)),
                                cantidad, precio);
                if (c.size() == 10) {
                    detalle.setComensal(c.get(8));
                    detalle.setObservaciones(c.get(9));
                }
                p.agregarDetalle(detalle);
            } else if ("X".equals(c.get(0)) || "O".equals(c.get(0))) {
                boolean extra = "X".equals(c.get(0));
                exigir(c.size() == (extra ? 9 : 4), "Personalización de pedido inválida.");
                Pedido p = leidos.get(entero(c.get(1)));
                int indice = entero(c.get(2));
                exigir(p != null && indice >= 0 && indice < p.getDetalles().size(),
                        "Personalización sin producto asociado.");
                DetallePedido detalle = p.getDetalles().get(indice);
                if (extra) {
                    double precio = decimal(c.get(8));
                    List<AdicionalPedido> lista = new ArrayList<>(detalle.getAdicionales());
                    lista.add(new AdicionalPedido(
                            new Plato(c.get(3), c.get(4), c.get(5), precio, c.get(6)),
                            entero(c.get(7)), precio));
                    detalle.setAdicionales(lista);
                } else {
                    exigir(!c.get(3).trim().isEmpty()
                                    && !detalle.getSinIngredientes().contains(c.get(3)),
                            "Ingrediente omitido inválido o repetido.");
                    List<String> omitidos = new ArrayList<>(detalle.getSinIngredientes());
                    omitidos.add(c.get(3));
                    detalle.setSinIngredientes(omitidos);
                }
            } else
                throw new IOException("Registro de pedido desconocido.");
        }
        for (Pedido p : leidos.values())
            exigir(!p.getDetalles().isEmpty() && Double.isFinite(p.calcularTotal())
                            && Math.abs(p.calcularTotal() - totales.get(p.getNumero())) < 0.005,
                    "Total o detalles inconsistentes en el pedido " + p.getNumero());
        destino.addAll(leidos.values());
    }
    public synchronized void guardarMenu(List<Plato> platos) throws IOException {
        exigirEscritura();
        validarMenu(platos);
        guardarMenuInterno(platos);
        menu.clear();
        for (Plato p : platos) menu.add(p.copia());
    }
    private void validarMenu(List<Plato> platos) throws IOException {
        Set<String> codigos = new HashSet<>();
        for (Plato p : platos) {
            exigir(p.getCodigo() != null && !p.getCodigo().trim().isEmpty()
                            && codigos.add(p.getCodigo().toLowerCase(Locale.ROOT)),
                    "Código de plato inválido o duplicado.");
            exigir(p.getNombre() != null && !p.getNombre().trim().isEmpty()
                            && p.getDescripcion() != null && !p.getDescripcion().trim().isEmpty(),
                    "Nombre o descripción vacía.");
            exigir(CatalogoMenu.getCategorias().contains(p.getCategoria())
                            && Double.isFinite(p.getPrecio()) && p.getPrecio() > 0,
                    "Categoría o precio inválido.");
        }
    }
    private void guardarMenuInterno(List<Plato> platos) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add("# codigo|nombre|descripcion|precio|categoria");
        for (Plato p : platos)
            lineas.add(ArchivosTxt.fila(p.getCodigo(), p.getNombre(), p.getDescripcion(),
                    p.getPrecio(), p.getCategoria()));
        ArchivosTxt.guardar(archivo("menu.txt"), lineas);
    }
    private void cargarMenu() throws IOException {
        for (List<String> c : filas("menu.txt")) {
            exigir(c.size() == 5, "Registro de menú inválido.");
            menu.add(new Plato(c.get(0), c.get(1), c.get(2), decimal(c.get(3)), c.get(4)));
        }
        validarMenu(menu);
    }
    public synchronized void guardarInventario(List<Insumo> insumos) throws IOException {
        exigirEscritura();
        List<String> lineas = new ArrayList<>();
        lineas.add("# nombre|unidad|cantidad");
        Set<String> nombres = new HashSet<>();
        for (Insumo i : insumos) {
            exigir(nombres.add(i.getNombre().toLowerCase(Locale.ROOT)), "Insumo duplicado.");
            lineas.add(ArchivosTxt.fila(i.getNombre(), i.getUnidad(), i.getCantidad()));
        }
        ArchivosTxt.guardar(archivo("inventario.txt"), lineas);
        inventario.clear();
        inventario.addAll(insumos);
    }
    private void cargarInventario() throws IOException {
        Set<String> nombres = new HashSet<>();
        for (List<String> c : filas("inventario.txt")) {
            exigir(c.size() == 3, "Registro de inventario inválido.");
            Insumo i = new Insumo(c.get(0), c.get(1), decimal(c.get(2)));
            exigir(nombres.add(i.getNombre().toLowerCase(Locale.ROOT)), "Insumo duplicado.");
            inventario.add(i);
        }
    }
    private static String clave(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT);
    }
    public synchronized List<IngredienteReceta> getReceta(String codigo) {
        List<IngredienteReceta> receta = recetas.get(clave(codigo));
        return receta == null ? Collections.emptyList()
                              : Collections.unmodifiableList(new ArrayList<>(receta));
    }
    private void cargarRecetas() throws IOException {
        for (List<String> c : filas("recetas.txt")) {
            exigir(c.size() == 4 && !c.get(0).trim().isEmpty(), "Registro de receta inválido.");
            List<IngredienteReceta> lista =
                    recetas.computeIfAbsent(clave(c.get(0)), k -> new ArrayList<>());
            for (IngredienteReceta i : lista)
                exigir(!clave(i.getInsumo()).equals(clave(c.get(1))),
                        "Ingrediente duplicado en una receta.");
            lista.add(new IngredienteReceta(c.get(1), c.get(2), decimal(c.get(3))));
        }
    }
    public synchronized void guardarReceta(String codigo, List<IngredienteReceta> ingredientes)
            throws IOException {
        exigirEscritura();
        Map<String, List<IngredienteReceta>> nuevas = new LinkedHashMap<>(recetas);
        nuevas.put(clave(codigo), new ArrayList<>(ingredientes));
        List<String> lineas = new ArrayList<>();
        lineas.add("# codigo_plato|insumo|unidad|cantidad_por_plato");
        for (Map.Entry<String, List<IngredienteReceta>> entrada : nuevas.entrySet())
            for (IngredienteReceta i : entrada.getValue())
                lineas.add(ArchivosTxt.fila(
                        entrada.getKey(), i.getInsumo(), i.getUnidad(), i.getCantidad()));
        ArchivosTxt.guardar(archivo("recetas.txt"), lineas);
        recetas.clear();
        recetas.putAll(nuevas);
    }

    private List<String> lineasInventario(List<Insumo> lista) {
        List<String> lineas = new ArrayList<>();
        lineas.add("# nombre|unidad|cantidad");
        for (Insumo i : lista)
            lineas.add(ArchivosTxt.fila(i.getNombre(), i.getUnidad(), i.getCantidad()));
        return lineas;
    }
    /** Guarda juntos el cobro, los pedidos abiertos y la atención de las mesas. */
    private void guardarOperacion(List<Pedido> ventas, List<Insumo> stock, List<Pedido> pendientes,
            List<Mesa> nuevasMesas) throws IOException {
        Map<String, List<String>> anteriores = new LinkedHashMap<>();
        for (String nombre : Arrays.asList(
                     "pedidos.txt", "inventario.txt", "pedidos_abiertos.txt", "mesas.txt")) {
            anteriores.put(nombre, ArchivosTxt.leer(archivo(nombre)));
        }
        Map<String, List<String>> nuevos = new LinkedHashMap<>();
        nuevos.put("inventario.txt", lineasInventario(stock));
        nuevos.put("pedidos.txt", lineasPedidos(ventas));
        nuevos.put("pedidos_abiertos.txt", lineasPedidos(pendientes));
        nuevos.put("mesas.txt", lineasMesas(nuevasMesas));
        List<String> diario = new ArrayList<>();
        diario.add("# Operación pendiente: copias anteriores para recuperación");
        for (Map.Entry<String, List<String>> entrada : anteriores.entrySet()) {
            String tipo = tipoArchivo(entrada.getKey());
            diario.add(ArchivosTxt.fila("V", tipo));
            for (String linea : entrada.getValue()) diario.add(ArchivosTxt.fila(tipo, linea));
        }
        ArchivosTxt.guardar(archivo("cobro_pendiente.txt"), diario);
        try {
            escribirArchivos(nuevos);
            Files.delete(archivo("cobro_pendiente.txt"));
        } catch (IOException error) {
            recuperacionPendiente = true;
            try {
                escribirArchivos(anteriores);
                Files.delete(archivo("cobro_pendiente.txt"));
                recuperacionPendiente = false;
            } catch (IOException recuperacion) {
                error.addSuppressed(recuperacion);
            }
            throw error;
        }
    }

    private static String tipoArchivo(String nombre) {
        if ("pedidos.txt".equals(nombre))
            return "P";
        if ("inventario.txt".equals(nombre))
            return "I";
        if ("pedidos_abiertos.txt".equals(nombre))
            return "A";
        return "M";
    }

    private void escribirArchivos(Map<String, List<String>> contenidos) throws IOException {
        for (Map.Entry<String, List<String>> entrada : contenidos.entrySet()) {
            ArchivosTxt.guardar(archivo(entrada.getKey()), entrada.getValue());
        }
    }
    private void recuperarCobro() throws IOException {
        if (!Files.exists(archivo("cobro_pendiente.txt")))
            return;
        Map<String, String> nombres = new LinkedHashMap<>();
        nombres.put("P", "pedidos.txt");
        nombres.put("I", "inventario.txt");
        nombres.put("A", "pedidos_abiertos.txt");
        nombres.put("M", "mesas.txt");
        Map<String, List<String>> anteriores = new LinkedHashMap<>();
        for (List<String> c : filas("cobro_pendiente.txt")) {
            exigir(c.size() == 2, "Diario de cobro inválido.");
            if ("V".equals(c.get(0))) {
                String declarado = nombres.get(c.get(1));
                exigir(declarado != null, "Archivo de recuperación desconocido.");
                anteriores.computeIfAbsent(declarado, k -> new ArrayList<>());
                continue;
            }
            String nombre = nombres.get(c.get(0));
            exigir(nombre != null, "Tipo de registro de recuperación inválido.");
            anteriores.computeIfAbsent(nombre, k -> new ArrayList<>()).add(c.get(1));
        }
        exigir(anteriores.containsKey("pedidos.txt") && anteriores.containsKey("inventario.txt")
                        && (anteriores.containsKey("pedidos_abiertos.txt")
                                == anteriores.containsKey("mesas.txt")),
                "Diario de cobro incompleto.");
        escribirArchivos(anteriores);
        Files.delete(archivo("cobro_pendiente.txt"));
    }
    public synchronized void guardarMesa(int numero, boolean ocupada) throws IOException {
        exigir(numero >= 1 && numero <= 10, "Mesa inválida.");
        List<Mesa> nuevas = new ArrayList<>(getDetalleMesas());
        int grupo = grupos[numero - 1];
        for (int i = 0; i < nuevas.size(); i++)
            if (nuevas.get(i).getGrupo() == grupo) {
                nuevas.set(i, nuevas.get(i).conEstado(ocupada));
            }
        guardarMesas(nuevas);
    }
    public synchronized void guardarMesas(List<Mesa> nuevas) throws IOException {
        exigirEscritura();
        validarMesas(nuevas);
        ArchivosTxt.guardar(archivo("mesas.txt"), lineasMesas(nuevas));
        aplicarMesas(nuevas);
    }
    private List<String> lineasMesas(List<Mesa> lista) {
        List<String> lineas = new ArrayList<>();
        lineas.add("# mesa|ocupada|sillas|grupo");
        for (Mesa mesa : lista)
            lineas.add(ArchivosTxt.fila(
                    mesa.getNumero(), mesa.estaOcupada(), mesa.getSillas(), mesa.getGrupo()));
        return lineas;
    }
    private void aplicarMesas(List<Mesa> lista) {
        for (Mesa mesa : lista) {
            int indice = mesa.getNumero() - 1;
            mesas[indice] = mesa.estaOcupada();
            sillas[indice] = mesa.getSillas();
            grupos[indice] = mesa.getGrupo();
        }
    }
    private void validarMesas(List<Mesa> lista) throws IOException {
        exigir(lista.size() == 10, "Deben existir diez mesas.");
        Map<Integer, Mesa> porNumero = new HashMap<>();
        for (Mesa mesa : lista)
            exigir(porNumero.put(mesa.getNumero(), mesa) == null, "Mesa duplicada.");
        for (Mesa mesa : lista) {
            Mesa principal = porNumero.get(mesa.getGrupo());
            exigir(principal != null && principal.getGrupo() == principal.getNumero()
                            && principal.estaOcupada() == mesa.estaOcupada(),
                    "Grupo de mesas inconsistente.");
        }
    }
    private void cargarMesas() throws IOException {
        Set<Integer> vistas = new HashSet<>();
        for (List<String> c : filas("mesas.txt")) {
            exigir(c.size() == 2 || c.size() == 4, "Registro de mesa inválido.");
            int numero = entero(c.get(0));
            exigir(numero >= 1 && numero <= 10 && vistas.add(numero), "Mesa inválida o duplicada.");
            exigir("true".equals(c.get(1)) || "false".equals(c.get(1)), "Estado de mesa inválido.");
            mesas[numero - 1] = Boolean.parseBoolean(c.get(1));
            sillas[numero - 1] = c.size() == 4 ? entero(c.get(2)) : Mesa.sillasIniciales(numero);
            grupos[numero - 1] = c.size() == 4 ? entero(c.get(3)) : numero;
        }
        exigir(vistas.size() == 10, "El archivo debe contener las diez mesas.");
        validarMesas(getDetalleMesas());
    }
    @Override
    public void close() throws IOException {
        try {
            if (bloqueo.isValid())
                bloqueo.release();
        } finally {
            canal.close();
        }
    }
}
