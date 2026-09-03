package cr.una.eif509.demo.excepcion;

public class InventarioInsuficienteException extends ExcepcionDeNegocio {

    public InventarioInsuficienteException(String producto, int disponible, int solicitado) {
        super("Inventario insuficiente para " + producto
                + ": hay " + disponible + ", se piden " + solicitado);
    }
}
