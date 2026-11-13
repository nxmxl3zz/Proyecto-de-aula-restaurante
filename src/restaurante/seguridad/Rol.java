package restaurante.seguridad;

/** Perfiles de atención y administración del restaurante. */
public enum Rol {
    EMPLEADO("Empleado"),
    GERENTE("Gerente");
    private final String nombre;
    Rol(String nombre) {
        this.nombre = nombre;
    }
    @Override
    public String toString() {
        return nombre;
    }
}
