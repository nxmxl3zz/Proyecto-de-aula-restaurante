package restaurante.seguridad;

/** Sólo Autenticacion crea una sesión después de verificar la contraseña. */
public final class Sesion {
    private final Rol rol;
    private boolean activa = true;
    Sesion(Rol rol) {
        this.rol = rol;
    }
    public Rol getRol() {
        return rol;
    }
    public boolean estaActiva() {
        return activa;
    }
    public void cerrar() {
        activa = false;
    }
    public boolean esGerente() {
        return rol == Rol.GERENTE;
    }
}
