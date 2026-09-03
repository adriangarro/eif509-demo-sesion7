package cr.una.eif509.demo.excepcion;

public class PedidoYaConfirmadoException extends ExcepcionDeNegocio {

    public PedidoYaConfirmadoException(Long pedidoId) {
        super("El pedido " + pedidoId + " ya fue confirmado");
    }
}
