package restaurante.datos;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import restaurante.entidades.*;

/**
 * Amplía una vez la demostración, con respaldo y sin reponer automáticamente las ventas
 * posteriores.
 */
final class AmpliacionMenu {
    private static final String MARCA = "menu_restaurante_v3.txt";
    private AmpliacionMenu() {}

    static boolean aplicar(Path directorio, List<Plato> menu, List<Insumo> inventario,
            Map<String, List<IngredienteReceta>> recetas) throws IOException {
        if (Files.exists(directorio.resolve(MARCA)))
            return false;
        Map<String, Plato> nuevos = new LinkedHashMap<>();
        for (Plato p : menu)
            nuevos.put(clave(p.getCodigo()),
                    new Plato(p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                            p.esAdicional() ? "Adicionales" : p.getCategoria()));
        Set<String> agregados = new HashSet<>();
        for (Plato p : CatalogoMenu.getPlatos()) {
            if (!p.getCodigo().matches("[SPZD]00[123]"))
                continue;
            if (!nuevos.containsKey(clave(p.getCodigo()))) {
                nuevos.put(clave(p.getCodigo()), p);
                agregados.add(clave(p.getCodigo()));
            }
        }
        Map<String, List<IngredienteReceta>> ampliadas = new LinkedHashMap<>(recetas);
        Map<String, List<IngredienteReceta>> iniciales = leerRecetas();
        for (Plato p : nuevos.values()) {
            String codigo = clave(p.getCodigo());
            if ((agregados.contains(codigo) || !ampliadas.containsKey(codigo))
                    && iniciales.containsKey(codigo))
                ampliadas.put(codigo, iniciales.get(codigo));
        }
        Map<String, BigDecimal> necesarias = new LinkedHashMap<>();
        Map<String, IngredienteReceta> ingredientes = new LinkedHashMap<>();
        for (Plato p : nuevos.values())
            for (IngredienteReceta i :
                    ampliadas.getOrDefault(clave(p.getCodigo()), Collections.emptyList())) {
                String nombre = clave(i.getInsumo());
                IngredienteReceta anterior = ingredientes.put(nombre, i);
                if (anterior != null && !clave(anterior.getUnidad()).equals(clave(i.getUnidad())))
                    throw new IOException(
                            "Unidades incompatibles al preparar existencias: " + i.getInsumo());
                necesarias.put(nombre,
                        necesarias.getOrDefault(nombre, BigDecimal.ZERO)
                                .add(BigDecimal.valueOf(i.getCantidad())
                                                .multiply(BigDecimal.valueOf(30))));
            }
        Map<String, Insumo> stock = new LinkedHashMap<>();
        for (Insumo i : inventario) stock.put(clave(i.getNombre()), i);
        for (Map.Entry<String, BigDecimal> entrada : necesarias.entrySet()) {
            IngredienteReceta i = ingredientes.get(entrada.getKey());
            Insumo anterior = stock.get(entrada.getKey());
            if (anterior != null && !clave(anterior.getUnidad()).equals(clave(i.getUnidad())))
                throw new IOException(
                        "La unidad existente no coincide con la receta: " + i.getInsumo());
            double cantidad = Math.max(anterior == null ? 0 : anterior.getCantidad(),
                    entrada.getValue().doubleValue());
            stock.put(entrada.getKey(),
                    new Insumo(anterior == null ? i.getInsumo() : anterior.getNombre(),
                            i.getUnidad(), cantidad));
        }
        Map<String, List<String>> contenidos = new LinkedHashMap<>();
        List<String> platos =
                new ArrayList<>(Arrays.asList("# codigo|nombre|descripcion|precio|categoria"));
        for (Plato p : nuevos.values())
            platos.add(ArchivosTxt.fila(p.getCodigo(), p.getNombre(), p.getDescripcion(),
                    p.getPrecio(), p.getCategoria()));
        contenidos.put("menu.txt", platos);
        List<String> formulas =
                new ArrayList<>(Arrays.asList("# codigo_plato|insumo|unidad|cantidad_por_plato"));
        for (Map.Entry<String, List<IngredienteReceta>> receta : ampliadas.entrySet())
            for (IngredienteReceta i : receta.getValue())
                formulas.add(ArchivosTxt.fila(
                        receta.getKey(), i.getInsumo(), i.getUnidad(), i.getCantidad()));
        contenidos.put("recetas.txt", formulas);
        List<String> existencias = new ArrayList<>(Arrays.asList(
                "# Existencias para 30 unidades de cada producto; reposición inicial única",
                "# nombre|unidad|cantidad"));
        for (Insumo i : stock.values())
            existencias.add(ArchivosTxt.fila(i.getNombre(), i.getUnidad(), i.getCantidad()));
        contenidos.put("inventario.txt", existencias);
        Path respaldo = directorio.resolve(".respaldo-menu-restaurante-v3");
        Files.createDirectories(respaldo);
        Map<String, List<String>> anteriores = new LinkedHashMap<>();
        for (String archivo : contenidos.keySet()) {
            anteriores.put(archivo, ArchivosTxt.leer(directorio.resolve(archivo)));
            if (!Files.exists(respaldo.resolve(archivo)))
                Files.copy(directorio.resolve(archivo), respaldo.resolve(archivo));
        }
        try {
            for (Map.Entry<String, List<String>> archivo : contenidos.entrySet())
                ArchivosTxt.guardar(directorio.resolve(archivo.getKey()), archivo.getValue());
            ArchivosTxt.guardar(directorio.resolve(MARCA),
                    Arrays.asList("# Ampliación aplicada una sola vez: 10 platos y existencias "
                                  + "para 30 unidades de cada producto.",
                            "# Respaldo previo en .respaldo-menu-restaurante-v3. No borrar esta "
                            + "marca para reponer inventario."));
        } catch (IOException error) {
            for (Map.Entry<String, List<String>> archivo : anteriores.entrySet()) try {
                    ArchivosTxt.guardar(directorio.resolve(archivo.getKey()), archivo.getValue());
                } catch (IOException recuperacion) {
                    error.addSuppressed(recuperacion);
                }
            throw error;
        }
        return true;
    }

    private static String clave(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT);
    }
    private static Map<String, List<IngredienteReceta>> leerRecetas() throws IOException {
        Map<String, List<IngredienteReceta>> recetas = new LinkedHashMap<>();
        try (InputStream entrada = AmpliacionMenu.class.getResourceAsStream(
                     "/restaurante/config/recetas.txt")) {
            if (entrada == null)
                throw new IOException("No se encontraron las recetas iniciales.");
            try (BufferedReader lector = new BufferedReader(
                         new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    if (linea.trim().isEmpty() || linea.startsWith("#"))
                        continue;
                    List<String> c = ArchivosTxt.campos(linea);
                    recetas.computeIfAbsent(clave(c.get(0)), k -> new ArrayList<>())
                            .add(new IngredienteReceta(
                                    c.get(1), c.get(2), Double.parseDouble(c.get(3))));
                }
            }
        }
        return recetas;
    }
}
