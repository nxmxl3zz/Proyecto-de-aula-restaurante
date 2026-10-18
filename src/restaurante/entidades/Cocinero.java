package restaurante.entidades;

/** Empleado encargado de la preparación de los platos. */
public class Cocinero extends Empleado {
    private String especialidad;

    public Cocinero() {
        super("Cocinero");
    }

    public Cocinero(String identificacion, String nombre, String direccion, String telefono,
            String especialidad) {
        super(identificacion, nombre, direccion, telefono, "Cocinero");
        this.especialidad = especialidad;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    @Override
    public String toString() {
        return super.toString() + ", Especialidad: " + especialidad;
    }
}
