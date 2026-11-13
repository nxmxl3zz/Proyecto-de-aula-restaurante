package restaurante.servicios;

import java.io.IOException;

/** Una entrada no cumple las reglas; la vista puede mostrar el mensaje sin perder el formulario. */
public final class ValidacionException extends IOException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
