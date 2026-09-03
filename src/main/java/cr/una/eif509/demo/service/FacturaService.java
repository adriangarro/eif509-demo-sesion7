package cr.una.eif509.demo.service;

import cr.una.eif509.demo.excepcion.MontoFacturaExcedidoException;
import cr.una.eif509.demo.model.Factura;
import cr.una.eif509.demo.model.Pedido;
import cr.una.eif509.demo.repository.FacturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class FacturaService {

    // Regla de negocio: por encima de este monto no se factura
    // automáticamente, requiere aprobación manual.
    public static final BigDecimal LIMITE_FACTURACION_AUTOMATICA = new BigDecimal("50000.00");

    private final FacturaRepository facturas;

    public FacturaService(FacturaRepository facturas) {
        this.facturas = facturas;
    }

    // Propagación REQUIRED (la de por defecto): se UNE a la transacción
    // de confirmarPedido. Si esta regla falla, no es "la factura falló":
    // es "el proceso completo falló" — y todo se deshace junto.
    @Transactional
    public Factura crear(Pedido pedido) {
        if (pedido.getTotal().compareTo(LIMITE_FACTURACION_AUTOMATICA) > 0) {
            throw new MontoFacturaExcedidoException(
                    pedido.getId(), pedido.getTotal(), LIMITE_FACTURACION_AUTOMATICA);
        }
        return facturas.saveAndFlush(new Factura(pedido, pedido.getTotal()));
    }
}
