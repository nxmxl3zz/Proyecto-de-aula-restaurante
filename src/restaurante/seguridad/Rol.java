package restaurante.seguridad;

public enum Rol {
    EMPLEADO("Empleado"), GERENTE("Gerente");
    private final String nombre;
    Rol(String nombre) {this.nombre=nombre;}
    @Override public String toString() {return nombre;}
}
