package restaurante;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import java.awt.EventQueue;
import javax.swing.JOptionPane;
import restaurante.presentacion.*;
import restaurante.seguridad.Autenticacion;
import restaurante.datos.*;

/** Carga los archivos y exige autenticación antes de abrir el restaurante. */
public class Main {
    public static void main(String[] args) {
        FlatLaf.registerCustomDefaultsSource("restaurante.themes");FlatDarkLaf.setup();
        EventQueue.invokeLater(() -> {
            DatosRestaurante datos=null;
            try {
                datos=new DatosRestaurante(DatosRestaurante.rutaPredeterminada());
                Autenticacion autenticacion=new Autenticacion(datos.getDirectorio());
                CatalogoMenu.getPlatos().clear();CatalogoMenu.getPlatos().addAll(datos.getMenu());
                final DatosRestaurante abiertos=datos;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {try {abiertos.close();}catch(java.io.IOException ex){System.err.println(ex.getMessage());}}));
                mostrarLogin(autenticacion,datos);
            } catch(java.io.IOException ex) {
                if(datos!=null)try{datos.close();}catch(java.io.IOException cierre){ex.addSuppressed(cierre);}
                JOptionPane.showMessageDialog(null,"No se pudo abrir La Doble.\n"+ex.getMessage(),"Archivos del restaurante",JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    private static void mostrarLogin(Autenticacion autenticacion,DatosRestaurante datos) {
        new VentanaLogin(autenticacion,sesion -> new VentanaPrincipal(sesion,datos,() -> mostrarLogin(autenticacion,datos)).setVisible(true)).setVisible(true);
    }
}
