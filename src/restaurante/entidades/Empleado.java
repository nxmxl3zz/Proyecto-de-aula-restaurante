package restaurante.entidades;

/** Empleado del restaurante. El cargo lo define cada subclase. */
public abstract class Empleado extends Persona {
    private String cargo;

    public Empleado(String cargo) {
        super();
        this.cargo = cargo;
    }

    public Empleado(String identificacion, String nombre, String direccion, String telefono,
            String cargo) {
        super(identificacion, nombre, direccion, telefono);
        this.cargo = cargo;
    }

    public String getCargo() {
        return cargo;
    }

    @Override
    public String toString() {
        return super.toString() + ", Cargo: " + cargo;
    }
}
