package restaurante.entidades;

/** Datos comunes de las personas vinculadas al restaurante. */
public abstract class Persona {
    private String identificacion;
    private String nombre;
    private String direccion;
    private String telefono;

    public Persona() {
    }

    public Persona(String identificacion, String nombre, String direccion, String telefono) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
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

    @Override
    public String toString() {
        return "Identificación: " + identificacion + ", Nombre: " + nombre
                + ", Dirección: " + direccion + ", Teléfono: " + telefono;
    }
}

