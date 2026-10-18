package restaurante.entidades;

import java.math.BigDecimal;

/** Datos básicos de un plato del menú. */
public class Plato {
    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;

    public Plato() {
    }

    public Plato(String codigo, String nombre, String descripcion, BigDecimal precio) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
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

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}

