package restaurante.servicios;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import restaurante.datos.*;
import restaurante.entidades.*;
import restaurante.seguridad.Sesion;

/** Casos de uso: permisos, validación y consumo de recetas, independientes de Swing. */
public final class ServicioRestaurante implements AutoCloseable {
    private final RepositorioRestaurante repositorio;

    public ServicioRestaurante(Path directorio) throws IOException {
        this(new DatosRestaurante(directorio));
    }

    public ServicioRestaurante(RepositorioRestaurante repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "Repositorio obligatorio.");
    }

    public Path getDirectorio() {
        return repositorio.getDirectorio();
    }
    public List<Pedido> getPedidos() {
        return repositorio.getPedidos();
    }
    public List<Plato> getMenu() {
        return repositorio.getMenu();
    }
    public List<Insumo> getInventario() {
        return repositorio.getInventario();
    }
    public boolean[] getMesas() {
        return repositorio.getMesas();
    }
    public List<Mesa> getDetalleMesas() {
        return repositorio.getDetalleMesas();
    }
    public List<Pedido> getPedidosAbiertos() {
        return repositorio.getPedidosAbiertos();
    }
    public Pedido getPedidoAbiertoPorMesa(int numero) {
        for (Pedido pedido : getPedidosAbiertos())
            if (pedido.getMesas().contains(numero))
                return pedido;
        return null;
    }
    public List<IngredienteReceta> getReceta(String codigo) {
        return codigo == null ? Collections.emptyList() : repositorio.getReceta(codigo);
    }
    public static List<String> getCategorias() {
        return CatalogoMenu.getCategorias();
    }

    public static void autenticar(Sesion sesion) {
        if (sesion == null || !sesion.estaActiva()) {
            throw new SecurityException("Inicia sesión para continuar.");
        }
    }

    public static void gerente(Sesion sesion) {
        autenticar(sesion);
        if (!sesion.esGerente())
            throw new SecurityException("Acceso exclusivo del gerente.");
    }

    public List<Pedido> consultarReporte(Sesion sesion) {
        gerente(sesion);
        return repositorio.getPedidos();
    }

    private static void exigir(boolean condicion, String mensaje) throws ValidacionException {
        if (!condicion)
            throw new ValidacionException(mensaje);
    }

    private static String clave(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT);
    }
    public synchronized Pedido registrarPedido(
            Sesion sesion, Integer mesa, List<DetallePedido> carrito) throws IOException {
        return registrarPedido(sesion, mesa, carrito, MetodoPago.EFECTIVO, null);
    }

    /** Cobra con el método indicado; en efectivo, un valor nulo equivale al total exacto. */
    public synchronized Pedido registrarPedido(Sesion sesion, Integer mesa,
            List<DetallePedido> carrito, MetodoPago metodo, Double recibido) throws IOException {
        autenticar(sesion);
        exigir(mesa == null || getPedidoAbiertoPorMesa(mesa) == null,
                "La mesa tiene un pedido abierto. Recupéralo para editarlo o cobrarlo.");
        return cobrar(mesa, carrito, null, metodo, recibido);
    }

    private Pedido crearPedido(int numero, Integer mesa, List<DetallePedido> carrito,
            EstadoPedido estado) throws IOException {
        exigir(carrito != null && !carrito.isEmpty(), "No se puede guardar un pedido vacío.");
        exigir(mesa == null || (mesa >= 1 && mesa <= 10), "Mesa inválida.");
        List<Plato> menuActual = repositorio.getMenu();
        ArrayList<DetallePedido> detalles = new ArrayList<>();
        for (DetallePedido detalle : carrito) {
            exigir(detalle != null && detalle.getPlato() != null && detalle.getCantidad() > 0
                            && Double.isFinite(detalle.getPrecioUnitario())
                            && detalle.getPrecioUnitario() > 0,
                    "Detalle de pedido inválido.");
            String codigo = detalle.getPlato().getCodigo();
            Plato actual = menuActual.stream()
                                   .filter(p -> p.getCodigo().equalsIgnoreCase(codigo))
                                   .findFirst()
                                   .orElse(null);
            exigir(actual != null, "El plato ya no existe en el menú. Actualiza el carrito.");
            exigir(Math.abs(detalle.getPrecioUnitario() - actual.getPrecio()) < 0.005,
                    "El precio de " + actual.getNombre() + " cambió en el menú. Actualiza el carrito.");
            exigir(!actual.esAdicional(),
                    "Los adicionales deben asociarse a un plato; no se venden como líneas "
                            + "independientes.");
            validarPersonalizacion(detalle, actual, menuActual);
            detalles.add(detalle.copia());
        }
        Pedido pedido = new Pedido(numero, LocalDate.now(), null, null, estado, detalles);
        if (mesa != null) {
            List<Mesa> mesasActuales = repositorio.getDetalleMesas();
            int grupo = mesasActuales.get(mesa - 1).getGrupo();
            List<Integer> numeros = new ArrayList<>();
            for (Mesa actual : mesasActuales)
                if (actual.getGrupo() == grupo)
                    numeros.add(actual.getNumero());
            pedido.setMesas(numeros);
        }
        exigir(Double.isFinite(pedido.calcularTotal()), "El total del pedido es demasiado grande.");
        return pedido;
    }

    private void validarPersonalizacion(DetallePedido detalle, Plato actual, List<Plato> menuActual)
            throws IOException {
        exigir(!detalle.estaPersonalizado() || detalle.getCantidad() == 1,
                "Personaliza cada unidad por separado para identificar a quién corresponde.");
        exigir(detalle.getComensal().length() <= 60 && detalle.getObservaciones().length() <= 300,
                "El comensal admite 60 caracteres y las indicaciones 300.");
        exigir(!"Bebidas".equals(actual.getCategoria())
                        || (detalle.getAdicionales().isEmpty()
                                && detalle.getSinIngredientes().isEmpty()),
                "Los adicionales de comida se aplican a platos, no a bebidas.");
        Set<String> omitidos = new HashSet<>();
        List<IngredienteReceta> receta = getReceta(actual.getCodigo());
        for (String nombre : detalle.getSinIngredientes()) {
            exigir(nombre != null && !nombre.trim().isEmpty() && omitidos.add(clave(nombre)),
                    "Ingrediente omitido vacío o repetido.");
            exigir(receta.stream().anyMatch(i -> clave(i.getInsumo()).equals(clave(nombre))),
                    "El ingrediente no pertenece al plato: " + nombre);
        }
        exigir(omitidos.isEmpty() || omitidos.size() < receta.size(),
                "No se pueden retirar todos los ingredientes del plato.");
        Set<String> extras = new HashSet<>();
        for (AdicionalPedido adicional : detalle.getAdicionales()) {
            exigir(adicional != null, "Adicional vacío.");
            String codigo = adicional.getPlato().getCodigo();
            exigir(codigo != null && extras.add(clave(codigo)),
                    "Adicional repetido: ajusta su cantidad.");
            exigir(menuActual.stream().anyMatch(
                           p -> p.esAdicional() && p.getCodigo().equalsIgnoreCase(codigo)),
                    "El adicional ya no está disponible.");
            exigir(menuActual.stream().anyMatch(p -> p.getCodigo().equalsIgnoreCase(codigo)
                           && Math.abs(p.getPrecio() - adicional.getPrecioUnitario()) < 0.005),
                    "El precio del adicional " + adicional.getPlato().getNombre()
                            + " cambió en el menú. Actualiza el carrito.");
            for (IngredienteReceta ingrediente : getReceta(codigo))
                exigir(!omitidos.contains(clave(ingrediente.getInsumo())),
                        "El adicional " + adicional.getPlato().getNombre() + " contiene "
                                + ingrediente.getInsumo() + ", que marcaste para retirar.");
        }
    }
    public DetallePedido validarUnidad(DetallePedido detalle) throws IOException {
        return crearPedido(1, null, Arrays.asList(detalle), EstadoPedido.PENDIENTE)
                .getDetalles()
                .get(0);
    }

    private Pedido cobrar(Integer mesa, List<DetallePedido> carrito, Integer abierto,
            MetodoPago metodo, Double recibido) throws IOException {
        exigir(metodo != null, "Selecciona el método de pago.");
        int ultimo = repositorio.getPedidos().stream().mapToInt(Pedido::getNumero).max().orElse(0);
        exigir(ultimo < Integer.MAX_VALUE, "Se agotó la numeración de pedidos.");
        Pedido pedido = crearPedido(ultimo + 1, mesa, carrito, EstadoPedido.COBRADO);
        double total = pedido.calcularTotal();
        double entregado = metodo == MetodoPago.EFECTIVO && recibido != null ? recibido : total;
        exigir(Double.isFinite(entregado) && entregado >= total - 0.005,
                "El efectivo recibido no cubre el total del pedido.");
        pedido.setPago(metodo, entregado);
        return repositorio.registrarCobro(
                pedido, descontarIngredientes(pedido.getDetalles()), abierto);
    }

    /** Guarda o reemplaza un pedido pendiente sin consumir insumos ni registrar una venta. */
    public synchronized Pedido guardarPedidoAbierto(Sesion sesion, Integer numero, Integer mesa,
            List<DetallePedido> carrito) throws IOException {
        autenticar(sesion);
        List<Pedido> pendientes = new ArrayList<>(getPedidosAbiertos());
        Pedido anterior = numero == null ? null : buscarAbierto(numero);
        int ultimo = pendientes.stream().mapToInt(Pedido::getNumero).max().orElse(0);
        exigir(numero != null || ultimo < Integer.MAX_VALUE,
                "Se agotó la numeración de pedidos abiertos.");
        Pedido pedido = crearPedido(
                numero == null ? ultimo + 1 : numero, mesa, carrito, EstadoPedido.PENDIENTE);
        if (anterior != null) {
            exigir(anterior.getMesas().equals(pedido.getMesas()),
                    "Un pedido guardado no puede cambiar de mesa. Abre un pedido nuevo.");
            pedido.setFecha(anterior.getFecha());
            pedido.setEstado(anterior.getEstado());
        }
        if (mesa != null) {
            Pedido existente = getPedidoAbiertoPorMesa(mesa);
            exigir(existente == null || Objects.equals(existente.getNumero(), numero),
                    "Ese grupo ya tiene un pedido abierto.");
        }
        pendientes.removeIf(p -> p.getNumero() == pedido.getNumero());
        pendientes.add(pedido);
        List<Mesa> nuevas = new ArrayList<>(getDetalleMesas());
        for (int i = 0; i < nuevas.size(); i++)
            if (pedido.getMesas().contains(nuevas.get(i).getNumero())) {
                nuevas.set(i, nuevas.get(i).conEstado(true));
            }
        repositorio.guardarAtencion(pendientes, nuevas);
        return buscarAbierto(pedido.getNumero());
    }

    /** Avanza un pedido abierto por cocina: pendiente, en preparación y servido. */
    public synchronized Pedido avanzarEstadoPedidoAbierto(Sesion sesion, int numero)
            throws IOException {
        autenticar(sesion);
        Pedido pedido = buscarAbierto(numero);
        EstadoPedido siguiente;
        switch (pedido.getEstado()) {
            case PENDIENTE:
                siguiente = EstadoPedido.EN_PREPARACION;
                break;
            case EN_PREPARACION:
                siguiente = EstadoPedido.SERVIDO;
                break;
            default:
                throw new ValidacionException("El pedido ya fue servido; solo falta cobrarlo.");
        }
        pedido.setEstado(siguiente);
        List<Pedido> pendientes = new ArrayList<>(getPedidosAbiertos());
        pendientes.removeIf(p -> p.getNumero() == numero);
        pendientes.add(pedido);
        repositorio.guardarAtencion(pendientes, getDetalleMesas());
        return buscarAbierto(numero);
    }

    public synchronized Pedido cobrarPedidoAbierto(
            Sesion sesion, int numero, List<DetallePedido> carrito) throws IOException {
        return cobrarPedidoAbierto(sesion, numero, carrito, MetodoPago.EFECTIVO, null);
    }

    public synchronized Pedido cobrarPedidoAbierto(Sesion sesion, int numero,
            List<DetallePedido> carrito, MetodoPago metodo, Double recibido) throws IOException {
        autenticar(sesion);
        Pedido anterior = buscarAbierto(numero);
        return cobrar(anterior.getMesa(), carrito, numero, metodo, recibido);
    }

    public synchronized void cancelarPedidoAbierto(Sesion sesion, int numero) throws IOException {
        autenticar(sesion);
        buscarAbierto(numero);
        List<Pedido> pendientes = new ArrayList<>(getPedidosAbiertos());
        pendientes.removeIf(p -> p.getNumero() == numero);
        repositorio.guardarAtencion(pendientes, getDetalleMesas());
    }

    private Pedido buscarAbierto(int numero) throws ValidacionException {
        for (Pedido pedido : getPedidosAbiertos())
            if (pedido.getNumero() == numero)
                return pedido;
        throw new ValidacionException(
                "El pedido abierto ya no existe; puede haberse cobrado o cancelado.");
    }

    private void validarMenu(List<Plato> platos) throws IOException {
        exigir(platos != null, "El menú es obligatorio.");
        Set<String> codigos = new HashSet<>();
        for (Plato p : platos) {
            exigir(p != null && p.getCodigo() != null && !p.getCodigo().trim().isEmpty()
                            && codigos.add(clave(p.getCodigo())),
                    "Código de plato inválido o duplicado.");
            exigir(p.getNombre() != null && !p.getNombre().trim().isEmpty()
                            && p.getDescripcion() != null && !p.getDescripcion().trim().isEmpty(),
                    "Nombre o descripción vacía.");
            exigir(CatalogoMenu.getCategorias().contains(p.getCategoria())
                            && Double.isFinite(p.getPrecio()) && p.getPrecio() > 0,
                    "Categoría o precio inválido.");
        }
    }

    public synchronized void guardarMenu(Sesion sesion, List<Plato> platos) throws IOException {
        gerente(sesion);
        validarMenu(platos);
        repositorio.guardarMenu(platos);
    }

    public synchronized void guardarInventario(Sesion sesion, List<Insumo> insumos)
            throws IOException {
        gerente(sesion);
        exigir(insumos != null, "El inventario es obligatorio.");
        Set<String> nombres = new HashSet<>();
        for (Insumo insumo : insumos) {
            exigir(insumo != null && nombres.add(clave(insumo.getNombre())),
                    "Insumo vacío o duplicado.");
        }
        repositorio.guardarInventario(insumos);
    }

    public synchronized void guardarReceta(
            Sesion sesion, String codigo, List<IngredienteReceta> ingredientes) throws IOException {
        gerente(sesion);
        exigir(codigo != null
                        && repositorio.getMenu().stream().anyMatch(
                                p -> clave(p.getCodigo()).equals(clave(codigo))),
                "El plato no existe en el menú.");
        exigir(ingredientes != null && !ingredientes.isEmpty(),
                "Selecciona al menos un insumo para el plato.");
        Set<String> vistos = new HashSet<>();
        for (IngredienteReceta ingrediente : ingredientes) {
            exigir(ingrediente != null && vistos.add(clave(ingrediente.getInsumo())),
                    "Ingrediente vacío o duplicado.");
            Insumo stock = buscarInsumo(ingrediente.getInsumo());
            exigir(stock != null && clave(stock.getUnidad()).equals(clave(ingrediente.getUnidad())),
                    "El insumo o la unidad no coinciden con el inventario: "
                            + ingrediente.getInsumo());
        }
        repositorio.guardarReceta(codigo, ingredientes);
    }

    public synchronized void guardarMesa(Sesion sesion, int numero, boolean ocupada)
            throws IOException {
        autenticar(sesion);
        exigir(numero >= 1 && numero <= 10, "Mesa inválida.");
        exigir(ocupada || getPedidoAbiertoPorMesa(numero) == null,
                "Cobra o cancela el pedido abierto antes de liberar la mesa.");
        int grupo = getDetalleMesas().get(numero - 1).getGrupo();
        List<Mesa> nuevas = new ArrayList<>(getDetalleMesas());
        for (int i = 0; i < nuevas.size(); i++)
            if (nuevas.get(i).getGrupo() == grupo)
                nuevas.set(i, nuevas.get(i).conEstado(ocupada));
        repositorio.guardarMesas(nuevas);
    }

    public synchronized void configurarSillas(Sesion sesion, int numero, int cantidad)
            throws IOException {
        gerente(sesion);
        exigir(numero >= 1 && numero <= 10 && cantidad >= 1 && cantidad <= 20,
                "Selecciona una mesa y entre 1 y 20 sillas.");
        List<Mesa> nuevas = new ArrayList<>(getDetalleMesas());
        exigir(!nuevas.get(numero - 1).estaOcupada(),
                "Libera la mesa antes de cambiar sus sillas.");
        nuevas.set(numero - 1, nuevas.get(numero - 1).conSillas(cantidad));
        repositorio.guardarMesas(nuevas);
    }

    /** Une mesas libres; extiende la selección a todos los integrantes de un grupo existente. */
    public synchronized void unirMesas(Sesion sesion, List<Integer> numeros) throws IOException {
        autenticar(sesion);
        exigir(numeros != null && numeros.size() >= 2,
                "Selecciona al menos dos mesas para unirlas.");
        Set<Integer> seleccion = new LinkedHashSet<>();
        Set<Integer> grupos = new HashSet<>();
        List<Mesa> actuales = getDetalleMesas();
        for (Integer numero : numeros) {
            exigir(numero != null && numero >= 1 && numero <= 10 && seleccion.add(numero),
                    "Las mesas son inválidas o están repetidas.");
            grupos.add(actuales.get(numero - 1).getGrupo());
        }
        exigir(grupos.size() >= 2, "Las mesas seleccionadas ya están unidas.");
        List<Mesa> nuevas = new ArrayList<>(actuales);
        for (Mesa mesa : actuales)
            if (grupos.contains(mesa.getGrupo())) {
                exigir(!mesa.estaOcupada() && getPedidoAbiertoPorMesa(mesa.getNumero()) == null,
                        "Une las mesas antes de ocuparlas o abrir un pedido.");
                seleccion.add(mesa.getNumero());
            }
        int principal = Collections.min(seleccion);
        for (int i = 0; i < nuevas.size(); i++)
            if (seleccion.contains(nuevas.get(i).getNumero())) {
                nuevas.set(i, nuevas.get(i).conGrupo(principal));
            }
        repositorio.guardarMesas(nuevas);
    }

    public synchronized void separarMesas(Sesion sesion, int numero) throws IOException {
        autenticar(sesion);
        exigir(numero >= 1 && numero <= 10, "Mesa inválida.");
        int grupo = getDetalleMesas().get(numero - 1).getGrupo();
        List<Mesa> nuevas = new ArrayList<>(getDetalleMesas());
        for (int i = 0; i < nuevas.size(); i++)
            if (nuevas.get(i).getGrupo() == grupo) {
                exigir(!nuevas.get(i).estaOcupada() && getPedidoAbiertoPorMesa(i + 1) == null,
                        "Libera el grupo antes de separar las mesas.");
                nuevas.set(i, nuevas.get(i).conGrupo(i + 1));
            }
        repositorio.guardarMesas(nuevas);
    }

    private Insumo buscarInsumo(String nombre) {
        for (Insumo i : repositorio.getInventario())
            if (clave(i.getNombre()).equals(clave(nombre)))
                return i;
        return null;
    }

    /** Comprueba existencias sin reservar ni modificar archivos. */
    public void comprobarExistencias(List<DetallePedido> carrito) throws IOException {
        descontarIngredientes(carrito);
    }

    private List<Insumo> descontarIngredientes(List<DetallePedido> carrito) throws IOException {
        Map<String, java.math.BigDecimal> cantidades = new LinkedHashMap<>();
        for (DetallePedido detalle : carrito) {
            List<IngredienteReceta> receta = getReceta(detalle.getPlato().getCodigo());
            exigir(!receta.isEmpty(),
                    "Falta configurar la receta de " + detalle.getPlato().getNombre()
                            + ". El gerente puede hacerlo desde Menú > Receta / insumos.");
            List<IngredienteReceta> consumos = new ArrayList<>();
            for (IngredienteReceta ingrediente : receta)
                if (detalle.getSinIngredientes().stream().noneMatch(
                            n -> clave(n).equals(clave(ingrediente.getInsumo()))))
                    consumos.add(ingrediente);
            for (AdicionalPedido adicional : detalle.getAdicionales()) {
                List<IngredienteReceta> formula = getReceta(adicional.getPlato().getCodigo());
                exigir(!formula.isEmpty(),
                        "Falta configurar la receta del adicional "
                                + adicional.getPlato().getNombre());
                for (IngredienteReceta i : formula)
                    consumos.add(new IngredienteReceta(i.getInsumo(), i.getUnidad(),
                            i.getCantidad() * adicional.getCantidad()));
            }
            for (IngredienteReceta ingrediente : consumos) {
                Insumo stock = buscarInsumo(ingrediente.getInsumo());
                exigir(stock != null, "Falta el insumo: " + ingrediente.getInsumo());
                exigir(clave(stock.getUnidad()).equals(clave(ingrediente.getUnidad())),
                        "Unidad incorrecta para " + ingrediente.getInsumo()
                                + ". La receta requiere " + ingrediente.getUnidad() + ".");
                java.math.BigDecimal consumo =
                        java.math.BigDecimal.valueOf(ingrediente.getCantidad())
                                .multiply(java.math.BigDecimal.valueOf(detalle.getCantidad()));
                String nombre = clave(ingrediente.getInsumo());
                cantidades.put(nombre,
                        cantidades.getOrDefault(nombre, java.math.BigDecimal.ZERO).add(consumo));
            }
        }
        List<Insumo> resultado = new ArrayList<>();
        for (Insumo stock : repositorio.getInventario()) {
            java.math.BigDecimal requerido =
                    cantidades.getOrDefault(clave(stock.getNombre()), java.math.BigDecimal.ZERO);
            java.math.BigDecimal actual = java.math.BigDecimal.valueOf(stock.getCantidad());
            exigir(actual.compareTo(requerido) >= 0,
                    "Existencias insuficientes de " + stock.getNombre() + ": disponibles "
                            + actual.stripTrailingZeros().toPlainString() + " " + stock.getUnidad()
                            + ", necesarios " + requerido.stripTrailingZeros().toPlainString()
                            + ".");
            resultado.add(new Insumo(stock.getNombre(), stock.getUnidad(),
                    actual.subtract(requerido).doubleValue()));
        }
        return resultado;
    }

    @Override
    public void close() throws IOException {
        repositorio.close();
    }
}
