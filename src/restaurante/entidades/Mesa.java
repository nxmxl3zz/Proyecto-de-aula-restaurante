package restaurante.entidades;

/** Estado inmutable de una mesa física y del grupo con el que comparte la atención. */
public final class Mesa {
    private final int numero;
    private final int sillas;
    private final boolean ocupada;
    private final int grupo;

    public Mesa(int numero, int sillas, boolean ocupada, int grupo) {
        if (numero < 1 || numero > 10 || sillas < 1 || sillas > 20 || grupo < 1 || grupo > 10) {
            throw new IllegalArgumentException(
                    "Mesa inválida: usa números de 1 a 10 y entre 1 y 20 sillas.");
        }
        this.numero = numero;
        this.sillas = sillas;
        this.ocupada = ocupada;
        this.grupo = grupo;
    }

    public static int sillasIniciales(int numero) {
        return new int[] {2, 2, 4, 4, 4, 4, 6, 6, 8, 8}[numero - 1];
    }

    public int getNumero() {
        return numero;
    }
    public int getSillas() {
        return sillas;
    }
    public boolean estaOcupada() {
        return ocupada;
    }
    public int getGrupo() {
        return grupo;
    }

    public Mesa conEstado(boolean estado) {
        return new Mesa(numero, sillas, estado, grupo);
    }
    public Mesa conSillas(int cantidad) {
        return new Mesa(numero, cantidad, ocupada, grupo);
    }
    public Mesa conGrupo(int principal) {
        return new Mesa(numero, sillas, ocupada, principal);
    }
}
