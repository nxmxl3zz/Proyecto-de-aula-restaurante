package restaurante;

import java.awt.EventQueue;
import javax.swing.JOptionPane;
import restaurante.controlador.*;
import restaurante.datos.*;
import restaurante.presentacion.*;
import restaurante.seguridad.Autenticacion;
import restaurante.servicios.ServicioRestaurante;

/** Carga los archivos y exige autenticación antes de abrir el restaurante. */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "--preparar-demo".equals(args[0])) {
            try (ServicioRestaurante servicio =
                            new ServicioRestaurante(DatosRestaurante.rutaPredeterminada())) {
                System.out.println("Menú preparado: " + servicio.getMenu().size()
                        + " productos y adicionales; " + servicio.getInventario().size()
                        + " insumos en " + servicio.getDirectorio());
            } catch (java.io.IOException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.length > 0 && "--consola".equals(args[0])) {
            try (ServicioRestaurante servicio =
                            new ServicioRestaurante(DatosRestaurante.rutaPredeterminada())) {
                ControladorAcceso acceso =
                        new ControladorAcceso(new Autenticacion(servicio.getDirectorio()));
                new Consola(System.in, System.out, acceso,
                        sesion -> new ControladorRestaurante(servicio, sesion))
                        .ejecutar();
            } catch (java.io.IOException ex) {
                System.err.println("No se pudo abrir o guardar el restaurante: " + ex.getMessage());
            }
            return;
        }
        Tema.instalar();
        EventQueue.invokeLater(() -> {
            ServicioRestaurante servicio = null;
            try {
                servicio = new ServicioRestaurante(DatosRestaurante.rutaPredeterminada());
                Autenticacion autenticacion = new Autenticacion(servicio.getDirectorio());
                final ServicioRestaurante abiertos = servicio;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        abiertos.close();
                    } catch (java.io.IOException ex) {
                        System.err.println(ex.getMessage());
                    }
                }));
                mostrarLogin(autenticacion, servicio);
            } catch (java.io.IOException ex) {
                if (servicio != null)
                    try {
                        servicio.close();
                    } catch (java.io.IOException cierre) {
                        ex.addSuppressed(cierre);
                    }
                JOptionPane.showMessageDialog(null,
                        "No se pudo abrir La Doble.\n" + ex.getMessage(),
                        "Archivos del restaurante", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    private static void mostrarLogin(Autenticacion autenticacion, ServicioRestaurante servicio) {
        new VentanaLogin(new ControladorAcceso(autenticacion),
                sesion
                -> new VentanaPrincipal(new ControladorRestaurante(servicio, sesion),
                        () -> mostrarLogin(autenticacion, servicio))
                        .setVisible(true))
                .setVisible(true);
    }
}
