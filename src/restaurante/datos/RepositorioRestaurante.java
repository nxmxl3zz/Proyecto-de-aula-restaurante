package restaurante.datos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import restaurante.entidades.*;

/** Contrato de almacenamiento; las reglas y los permisos pertenecen al servicio. */
public interface RepositorioRestaurante extends AutoCloseable {
    Path getDirectorio();
    List<Pedido> getPedidos();
    List<Pedido> getPedidosAbiertos();
    List<Plato> getMenu();
    List<Insumo> getInventario();
    boolean[] getMesas();
    List<Mesa> getDetalleMesas();
    List<IngredienteReceta> getReceta(String codigo);
    Pedido registrarCobro(Pedido pedido, List<Insumo> existencias) throws IOException;
    Pedido registrarCobro(Pedido pedido, List<Insumo> existencias, Integer abierto)
            throws IOException;
    void guardarAtencion(List<Pedido> abiertos, List<Mesa> mesas) throws IOException;
    void guardarMesas(List<Mesa> mesas) throws IOException;
    void guardarMenu(List<Plato> platos) throws IOException;
    void guardarInventario(List<Insumo> insumos) throws IOException;
    void guardarReceta(String codigo, List<IngredienteReceta> ingredientes) throws IOException;
    void guardarMesa(int numero, boolean ocupada) throws IOException;
    @Override void close() throws IOException;
}
