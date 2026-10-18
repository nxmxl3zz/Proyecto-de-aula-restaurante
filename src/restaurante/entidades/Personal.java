package restaurante.entidades;

/** Empleado del restaurante y su cargo. */
public class Personal extends Persona {
    private String cargo;

    public Personal() {
        super();
    }

    public Personal(String identificacion, String nombre, String direccion, String telefono,
            String cargo) {
        super(identificacion, nombre, direccion, telefono);
        this.cargo = cargo;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
}

