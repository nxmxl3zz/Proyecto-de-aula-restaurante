package restaurante.seguridad;

/** Sólo Autenticacion crea una sesión después de verificar la contraseña. */
public final class Sesion {
    private final Rol rol;
    Sesion(Rol rol) {this.rol=rol;}
    public Rol getRol() {return rol;}
    public boolean esGerente() {return rol==Rol.GERENTE;}
}
