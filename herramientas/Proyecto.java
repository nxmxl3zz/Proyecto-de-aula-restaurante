import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Compila, empaqueta y prueba desde Java, conservando FlatLaf como biblioteca de interfaz. */
public final class Proyecto {
    private static final Path CLASES = Paths.get("build/classes");
    private static final Path PRUEBAS = Paths.get("build/test/classes");
    private static final Path JAR = Paths.get("dist/Proyecto-Gestion-Restaurante.jar");
    private static final Path FLATLAF = Paths.get("lib/flatlaf-3.7.2.jar");

    public static void main(String[] args) {
        try {
            if (!Files.isDirectory(Paths.get("src/restaurante"))) {
                throw new IOException("Ejecuta esta herramienta desde la carpeta raíz del proyecto.");
            }
            String opcion = args.length == 0 ? "compilar" : args[0];
            if (!Arrays.asList("compilar", "probar", "probar-ui", "ejecutar", "consola", "limpiar")
                    .contains(opcion)) {
                throw new IllegalArgumentException("Uso: java herramientas/Proyecto.java "
                        + "{compilar|probar|probar-ui|ejecutar|consola|limpiar}");
            }
            if ("limpiar".equals(opcion)) {
                limpiar(Paths.get("build"));
                System.out.println("Archivos temporales de compilación eliminados.");
                return;
            }
            compilar();
            if ("probar".equals(opcion)) {
                for (String suite : Arrays.asList("DatosRestauranteTest", "RecetasInventarioTest",
                        "ControladorTest", "ConsolaTest", "AtencionTest", "PanelesTest",
                        "PersonalizacionesTest", "LimpiezaTest", "ExperienciaUsuarioTest")) {
                    ejecutar("-Djava.awt.headless=true", "-cp", rutaClases(), "restaurante." + suite);
                }
                System.out.println("Todas las suites finalizaron correctamente.");
            } else if ("probar-ui".equals(opcion)) {
                ejecutar("-cp", rutaClases(), "restaurante.ButtonIntegrationTest");
            } else if ("ejecutar".equals(opcion)) {
                ejecutar("-jar", JAR.toString());
            } else if ("consola".equals(opcion)) {
                ejecutar("-jar", JAR.toString(), "--consola");
            }
        } catch (Exception ex) {
            System.err.println("No se completó la operación: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static String rutaClases() {
        return PRUEBAS + File.pathSeparator + CLASES + File.pathSeparator + FLATLAF;
    }

    private static void compilar() throws IOException {
        JavaCompiler compilador = ToolProvider.getSystemJavaCompiler();
        if (compilador == null) throw new IOException("Se necesita un JDK completo para compilar.");
        limpiar(CLASES);
        limpiar(PRUEBAS);
        compilarFuentes(compilador, Paths.get("src"), CLASES, FLATLAF.toString());
        try (Stream<Path> archivos = Files.walk(Paths.get("src"))) {
            for (Path origen : archivos.filter(Files::isRegularFile).collect(Collectors.toList())) {
                String nombre = origen.getFileName().toString();
                if (nombre.endsWith(".java") || nombre.endsWith(".form") || nombre.endsWith(".md")) continue;
                Path destino = CLASES.resolve(Paths.get("src").relativize(origen));
                Files.createDirectories(destino.getParent());
                Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        compilarFuentes(compilador, Paths.get("test"), PRUEBAS,
                CLASES + File.pathSeparator + FLATLAF);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "restaurante.Main");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, "lib/flatlaf-3.7.2.jar");
        Files.createDirectories(JAR.getParent());
        Files.createDirectories(JAR.getParent().resolve("lib"));
        Files.copy(FLATLAF, JAR.getParent().resolve("lib").resolve(FLATLAF.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);
        try (JarOutputStream salida = new JarOutputStream(Files.newOutputStream(JAR), manifest);
                Stream<Path> archivos = Files.walk(CLASES)) {
            for (Path origen : archivos.filter(Files::isRegularFile).sorted().collect(Collectors.toList())) {
                String nombre = CLASES.relativize(origen).toString().replace(File.separatorChar, '/');
                salida.putNextEntry(new JarEntry(nombre));
                Files.copy(origen, salida);
                salida.closeEntry();
            }
        }
        System.out.println("Compilado desde Java; interfaz FlatLaf conservada: " + JAR);
    }

    private static void compilarFuentes(JavaCompiler compilador, Path carpeta, Path destino,
            String classpath) throws IOException {
        Files.createDirectories(destino);
        List<String> opciones = new ArrayList<>(Arrays.asList("-encoding", "UTF-8", "--release", "8",
                "-Xlint:-options", "-d", destino.toString()));
        if (classpath != null) opciones.addAll(Arrays.asList("-cp", classpath));
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            opciones.addAll(archivos.filter(p -> p.toString().endsWith(".java")).sorted()
                    .map(Path::toString).collect(Collectors.toList()));
        }
        if (compilador.run(null, System.out, System.err, opciones.toArray(new String[0])) != 0) {
            throw new IOException("Falló la compilación de " + carpeta + ".");
        }
    }

    private static void limpiar(Path carpeta) throws IOException {
        if (!Files.exists(carpeta)) return;
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            for (Path archivo : archivos.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.delete(archivo);
            }
        }
    }

    private static void ejecutar(String... argumentos) throws IOException, InterruptedException {
        String binario = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        List<String> comando = new ArrayList<>();
        comando.add(Paths.get(System.getProperty("java.home"), "bin", binario).toString());
        comando.addAll(Arrays.asList(argumentos));
        int resultado = new ProcessBuilder(comando).inheritIO().start().waitFor();
        if (resultado != 0) throw new IOException("La ejecución Java terminó con código " + resultado + ".");
    }
}
