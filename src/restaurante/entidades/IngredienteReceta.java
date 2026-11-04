package restaurante.entidades;

/** Cantidad de un insumo necesaria para preparar una unidad de un plato. */
public final class IngredienteReceta {
    private final String insumo;
    private final String unidad;
    private final double cantidad;
    public IngredienteReceta(String insumo,String unidad,double cantidad) {
        if(insumo==null||insumo.trim().isEmpty()||unidad==null||unidad.trim().isEmpty()||!Double.isFinite(cantidad)||cantidad<=0)
            throw new IllegalArgumentException("Ingrediente o cantidad de receta inválida.");
        this.insumo=insumo.trim();this.unidad=unidad.trim();this.cantidad=cantidad;
    }
    public String getInsumo() {return insumo;}
    public String getUnidad() {return unidad;}
    public double getCantidad() {return cantidad;}
}
