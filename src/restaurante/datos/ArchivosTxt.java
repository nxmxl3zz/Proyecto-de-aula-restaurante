package restaurante.datos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** UTF-8, campos separados por | y sustitución atómica del archivo completo. */
public final class ArchivosTxt {
    private ArchivosTxt() {}
    public static List<String> leer(Path ruta) throws IOException {
        return Files.readAllLines(ruta,StandardCharsets.UTF_8);
    }
    public static String fila(Object... valores) {
        List<String> campos=new ArrayList<>();
        for(Object valor:valores) campos.add(String.valueOf(valor).replace("\\","\\\\").replace("|","\\|").replace("#","\\#").replace("\n","\\n").replace("\r","\\r"));
        return String.join("|",campos);
    }
    public static List<String> campos(String linea) throws IOException {
        List<String> campos=new ArrayList<>();StringBuilder campo=new StringBuilder();boolean escape=false;
        for(char c:linea.toCharArray()) {
            if(escape) {
                if(c=='n') campo.append('\n');else if(c=='r') campo.append('\r');
                else if(c=='|'||c=='\\'||c=='#') campo.append(c);else throw new IOException("Secuencia de escape inválida.");
                escape=false;
            } else if(c=='\\') escape=true;
            else if(c=='|') {campos.add(campo.toString());campo.setLength(0);}else campo.append(c);
        }
        if(escape) throw new IOException("Campo incompleto.");
        campos.add(campo.toString());return campos;
    }
    public static void guardar(Path ruta,List<String> lineas) throws IOException {
        Files.createDirectories(ruta.toAbsolutePath().getParent());
        Path temporal=Files.createTempFile(ruta.toAbsolutePath().getParent(),".la-doble-",".tmp");
        try {
            Files.write(temporal,lineas,StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING);
            try {Files.move(temporal,ruta,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex) {Files.move(temporal,ruta,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporal);}
    }
}
