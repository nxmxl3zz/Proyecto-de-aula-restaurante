package restaurante.entidades;

/** Datos básicos de un plato del menú. */
public class Plato {
    private String codigo;
    private String nombre;
    private String descripcion;
    private double precio;
    private String categoria;

    public Plato() {}

    public Plato(String codigo, String nombre, String descripcion, double precio) {
        this(codigo, nombre, descripcion, precio, "Hamburguesas");
    }

    public Plato(
            String codigo, String nombre, String descripcion, double precio, String categoria) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.categoria = categoria;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria;
    }
    public boolean esAdicional() {
        return "Adicionales".equalsIgnoreCase(categoria) || "Extras".equalsIgnoreCase(categoria);
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    /** Instantánea independiente para el menú y los precios históricos del pedido. */
    public Plato copia() {
        return new Plato(codigo, nombre, descripcion, precio, categoria);
    }

    @Override
    public String toString() {
        return "Código: " + codigo + ", Nombre: " + nombre + ", Categoría: " + categoria
                + ", Descripción: " + descripcion + ", Precio: " + precio;
    }
}
