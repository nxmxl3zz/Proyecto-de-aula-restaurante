package restaurante.controlador;

import java.io.IOException;
import restaurante.seguridad.*;

/** Punto de entrada de la vista de acceso al servicio de autenticación. */
public final class ControladorAcceso {
    private final Autenticacion autenticacion;

    public ControladorAcceso(Autenticacion autenticacion) {
        this.autenticacion = java.util.Objects.requireNonNull(autenticacion);
    }

    public Sesion autenticar(Rol rol, char[] clave) throws IOException {
        return autenticacion.autenticar(rol, clave);
    }
}
