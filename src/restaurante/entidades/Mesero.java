package restaurante.entidades;

/** Empleado que atiende los pedidos del restaurante. */
public class Mesero extends Personal {
    private String turno;

    public Mesero() {
        super();
        setCargo("Mesero");
    }

    public Mesero(String identificacion, String nombre, String direccion, String telefono,
            String turno) {
        super(identificacion, nombre, direccion, telefono, "Mesero");
        this.turno = turno;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }
}

