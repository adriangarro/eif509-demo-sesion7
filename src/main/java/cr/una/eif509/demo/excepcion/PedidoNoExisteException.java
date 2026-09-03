package cr.una.eif509.demo.excepcion;

public class PedidoNoExisteException extends ExcepcionDeNegocio {

    public PedidoNoExisteException(Long pedidoId) {
        super("No existe el pedido " + pedidoId);
    }
}
