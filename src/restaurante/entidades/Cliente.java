package restaurante.entidades;

/** Cliente del restaurante. */
public class Cliente extends Persona {
    private String preferenciasEspeciales;

    public Cliente() {
        super();
    }

    public Cliente(String identificacion, String nombre, String direccion, String telefono,
            String preferenciasEspeciales) {
        super(identificacion, nombre, direccion, telefono);
        this.preferenciasEspeciales = preferenciasEspeciales;
    }

    public String getPreferenciasEspeciales() {
        return preferenciasEspeciales;
    }

    public void setPreferenciasEspeciales(String preferenciasEspeciales) {
        this.preferenciasEspeciales = preferenciasEspeciales;
    }

    @Override
    public String toString() {
        return super.toString() + ", Preferencias especiales: " + preferenciasEspeciales;
    }
}

