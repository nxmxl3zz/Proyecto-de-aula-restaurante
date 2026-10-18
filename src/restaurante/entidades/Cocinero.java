package restaurante.entidades;

/** Empleado encargado de la preparación de los platos. */
public class Cocinero extends Personal {
    private String especialidad;

    public Cocinero() {
        super();
        setCargo("Cocinero");
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
}

