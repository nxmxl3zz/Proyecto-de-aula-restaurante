package restaurante.entidades;

/** Existencia de un insumo con su unidad de medida. */
public final class Insumo {
    private final String nombre;
    private final String unidad;
    private final double cantidad;
    public Insumo(String nombre,String unidad,double cantidad) {
        if(nombre==null||nombre.trim().isEmpty()||unidad==null||unidad.trim().isEmpty()||!Double.isFinite(cantidad)||cantidad<0)
            throw new IllegalArgumentException("Insumo o cantidad inválida.");
        this.nombre=nombre.trim();this.unidad=unidad.trim();this.cantidad=cantidad;
    }
    public String getNombre() {return nombre;}
    public String getUnidad() {return unidad;}
    public double getCantidad() {return cantidad;}
}
