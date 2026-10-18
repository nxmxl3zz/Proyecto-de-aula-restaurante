package restaurante.entidades;

import java.util.ArrayList;

/** Datos generales del establecimiento y los registros que maneja. */
public class Restaurante {
    private String nombre;
    private String direccion;
    private String telefono;
    private String sitioWeb;
    private ArrayList<Plato> platos;
    private ArrayList<Empleado> empleados;
    private ArrayList<Cliente> clientes;
    private ArrayList<ProveedorAlimentos> proveedores;
    private ArrayList<Pedido> pedidos;

    public Restaurante() {
        platos = new ArrayList<>();
        empleados = new ArrayList<>();
        clientes = new ArrayList<>();
        proveedores = new ArrayList<>();
        pedidos = new ArrayList<>();
    }

    public Restaurante(String nombre, String direccion, String telefono, String sitioWeb) {
        this();
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.sitioWeb = sitioWeb;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getSitioWeb() {
        return sitioWeb;
    }

    public void setSitioWeb(String sitioWeb) {
        this.sitioWeb = sitioWeb;
    }

    public ArrayList<Plato> getPlatos() {
        return platos;
    }

    public ArrayList<Empleado> getEmpleados() {
        return empleados;
    }

    public ArrayList<Cliente> getClientes() {
        return clientes;
    }

    public ArrayList<ProveedorAlimentos> getProveedores() {
        return proveedores;
    }

    public ArrayList<Pedido> getPedidos() {
        return pedidos;
    }

    /**
     * Agrega un plato al menú.
     *
     * @param plato el plato que se agrega
     */
    public void agregarPlato(Plato plato) {
        platos.add(plato);
    }

    /**
     * Registra un empleado del restaurante.
     *
     * @param empleado el mesero o cocinero que se registra
     */
    public void agregarEmpleado(Empleado empleado) {
        empleados.add(empleado);
    }

    /**
     * Registra un cliente del restaurante.
     *
     * @param cliente el cliente que se registra
     */
    public void agregarCliente(Cliente cliente) {
        clientes.add(cliente);
    }

    /**
     * Registra un proveedor de alimentos.
     *
     * @param proveedor el proveedor que se registra
     */
    public void agregarProveedor(ProveedorAlimentos proveedor) {
        proveedores.add(proveedor);
    }

    /**
     * Registra un pedido.
     *
     * @param pedido el pedido que se registra
     */
    public void agregarPedido(Pedido pedido) {
        pedidos.add(pedido);
    }

    @Override
    public String toString() {
        return "Nombre: " + nombre + ", Dirección: " + direccion + ", Teléfono: " + telefono
                + ", Sitio web: " + sitioWeb;
    }
}
