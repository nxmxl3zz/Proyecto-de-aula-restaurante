package restaurante;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import restaurante.controlador.*;
import restaurante.presentacion.Consola;
import restaurante.seguridad.Autenticacion;
import restaurante.servicios.ServicioRestaurante;

/** Flujo real de consola con entradas erróneas, permisos, cobro y reinicio. */
public final class ConsolaTest {
    private static int pruebas;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor)
            throw new AssertionError(mensaje);
        pruebas++;
        System.out.println("OK " + mensaje);
    }
    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("la-doble-consola-");
        String guion = "3\n1\nincorrecta\n1\nempleado123\n5\n6\n2\nabc\n1\n3\n1\nH001\n1\n0\nS\n0\n"
                + "2\ngerente123\n5\n0\n0\n";
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            DatosPrueba.stockClasico(servicio);
            ControladorAcceso acceso = new ControladorAcceso(new Autenticacion(carpeta));
            new Consola(new ByteArrayInputStream(guion.getBytes(StandardCharsets.UTF_8)),
                    new PrintStream(salida, true, "UTF-8"), acceso,
                    sesion -> new ControladorRestaurante(servicio, sesion))
                    .ejecutar();
        }
        String texto = salida.toString("UTF-8");
        comprobar(texto.contains("Selecciona un rol válido"), "consola rechaza rol inválido");
        comprobar(texto.contains("Contraseña incorrecta"), "consola rechaza contraseña inválida");
        comprobar(texto.contains("Acceso exclusivo del gerente"),
                "consola aplica permisos aunque se escriba una opción oculta");
        comprobar(texto.contains("No se aplicó la operación"),
                "consola continúa ante entrada no numérica");
        comprobar(texto.contains("Cobro registrado: #1"), "consola confirma cobro");
        comprobar(texto.contains("Estado: COBRADO"), "gerente consulta la venta por consola");
        try (ServicioRestaurante servicio = new ServicioRestaurante(carpeta)) {
            comprobar(servicio.getPedidos().size() == 1
                            && servicio.getPedidos().get(0).calcularTotal() == 28900,
                    "consola guarda total y pedido en TXT");
            comprobar(
                    servicio.getInventario().stream().anyMatch(
                            i -> i.getNombre().equals("Pan hamburguesa") && i.getCantidad() == 19),
                    "consola descuenta receta y conserva existencias al reiniciar");
        }
        Path reunion = Files.createTempDirectory("la-doble-consola-reunion-");
        String instrucciones = "1\nempleado123\n16\n1,3\n13\n3\nH001\n1\n0\nS\n12\n"
                + "14\n1\nH001\n2\n0\nS\n15\n1\nS\n2\n3\n0\n17\n1\n0\n"
                + "2\ngerente123\n18\n1\n5\n0\n1\nempleado123\n"
                + "13\n0\nB003\n1\n0\nS\n14\n1\nB003\n2\n0\nS\n19\n1\nS\n0\n0\n";
        ByteArrayOutputStream nuevaSalida = new ByteArrayOutputStream();
        try (ServicioRestaurante servicio = new ServicioRestaurante(reunion)) {
            new Consola(new ByteArrayInputStream(instrucciones.getBytes(StandardCharsets.UTF_8)),
                    new PrintStream(nuevaSalida, true, "UTF-8"),
                    new ControladorAcceso(new Autenticacion(reunion)),
                    sesion -> new ControladorRestaurante(servicio, sesion))
                    .ejecutar();
        }
        String reunionTexto = nuevaSalida.toString("UTF-8");
        comprobar(reunionTexto.contains("Mesas unidas") && reunionTexto.contains("Mesas 1 + 3"),
                "consola une mesas y guarda su pedido compartido");
        comprobar(!reunionTexto.contains("No se aplicó la operación"),
                "consola completa edición, cobro, separación y cancelación sin errores");
        try (ServicioRestaurante servicio = new ServicioRestaurante(reunion)) {
            comprobar(servicio.getPedidos().size() == 1
                            && servicio.getPedidos().get(0).calcularTotal() == 57800,
                    "consola cobra la versión editada de la reunión");
            comprobar(servicio.getPedidos().get(0).getMesas().equals(java.util.Arrays.asList(1, 3)),
                    "consola conserva las mesas en el historial");
            comprobar(servicio.getDetalleMesas().get(0).getSillas() == 5
                            && servicio.getDetalleMesas().get(2).getGrupo() == 3,
                    "consola persiste capacidades y separación");
            comprobar(servicio.getPedidosAbiertos().isEmpty(),
                    "consola edita y cancela un abierto sin mesa");
        }
        Path personalizar = Files.createTempDirectory("la-doble-consola-personalizar-");
        String entradas = "1\nempleado123\n13\n0\nH002\n2\n0\nS\n20\n1\n1\nAna\nBien "
                          + "caliente\nTomate\nE003\n1\n0\nS\n15\n1\nS\n0\n0\n";
        ByteArrayOutputStream resultado = new ByteArrayOutputStream();
        try (ServicioRestaurante servicio = new ServicioRestaurante(personalizar)) {
            new Consola(new ByteArrayInputStream(entradas.getBytes(StandardCharsets.UTF_8)),
                    new PrintStream(resultado, true, "UTF-8"),
                    new ControladorAcceso(new Autenticacion(personalizar)),
                    sesion -> new ControladorRestaurante(servicio, sesion))
                    .ejecutar();
        }
        comprobar(!resultado.toString("UTF-8").contains("No se aplicó la operación"),
                "la consola personaliza una unidad sin interferir con la otra");
        try (ServicioRestaurante servicio = new ServicioRestaurante(personalizar)) {
            restaurante.entidades.Pedido p = servicio.getPedidos().get(0);
            comprobar(p.calcularTotal() == 40000
                            && p.getDetalles().get(0).getAdicionales().size() == 1,
                    "la consola cobra dos hamburguesas con un solo queso adicional");
            comprobar(p.getDetalles().get(0).getSinIngredientes().contains("Tomate")
                            && p.getDetalles().get(1).getSinIngredientes().isEmpty(),
                    "la consola guarda sin tomate exclusivamente para la unidad de Ana");
        }
        System.out.println("TOTAL CONSOLA: " + pruebas + " comprobaciones.");
    }
}
