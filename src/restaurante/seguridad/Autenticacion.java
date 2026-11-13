package restaurante.seguridad;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import restaurante.datos.ArchivosTxt;

/** Dos roles con contraseña en usuarios.txt para la demostración local. */
public final class Autenticacion {
    private final Path archivo;
    public Autenticacion(Path directorio) throws IOException {
        archivo = directorio.resolve("usuarios.txt");
        if (!Files.exists(archivo)) {
            try (java.io.InputStream entrada = Autenticacion.class.getResourceAsStream(
                         "/restaurante/config/usuarios.txt")) {
                if (entrada == null)
                    throw new IOException("No se encontró la configuración inicial de usuarios.");
                List<String> lineas = new ArrayList<>();
                try (java.io.BufferedReader lector =
                                new java.io.BufferedReader(new java.io.InputStreamReader(
                                        entrada, java.nio.charset.StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = lector.readLine()) != null) lineas.add(linea);
                }
                ArchivosTxt.guardar(archivo, lineas);
            }
        }
        leerClaves();
    }
    private Map<Rol, String> leerClaves() throws IOException {
        Map<Rol, String> claves = new EnumMap<>(Rol.class);
        try {
            for (String linea : ArchivosTxt.leer(archivo)) {
                if (linea.startsWith("#") || linea.trim().isEmpty())
                    continue;
                List<String> c = ArchivosTxt.campos(linea);
                if (c.size() != 2 || c.get(1).isEmpty())
                    throw new IOException("Formato de usuario inválido.");
                Rol rol = Rol.valueOf(c.get(0));
                if (claves.put(rol, c.get(1)) != null)
                    throw new IOException("Rol duplicado en usuarios.txt.");
            }
        } catch (IllegalArgumentException ex) {
            throw new IOException("Rol desconocido en usuarios.txt.", ex);
        }
        if (claves.size() != 2)
            throw new IOException("usuarios.txt debe contener EMPLEADO y GERENTE.");
        return claves;
    }
    public Sesion autenticar(Rol rol, char[] clave) throws IOException {
        return rol != null && clave != null && leerClaves().get(rol).equals(new String(clave))
                ? new Sesion(rol)
                : null;
    }
}
