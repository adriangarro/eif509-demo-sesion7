package cr.una.eif509.demo.service;

import cr.una.eif509.demo.excepcion.PedidoNoExisteException;
import cr.una.eif509.demo.excepcion.PedidoYaConfirmadoException;
import cr.una.eif509.demo.model.Pedido;
import cr.una.eif509.demo.repository.InventarioRepository;
import cr.una.eif509.demo.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// La capa de servicios: aquí viven las reglas y la ORQUESTACIÓN del
// proceso. El método habla el idioma del negocio (confirmarPedido),
// no el de la base de datos.
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final InventarioRepository inventarios;
    private final FacturaService facturas;
    private final BitacoraService bitacora;
    private final AuditoriaService auditoria;

    // Inyección por constructor: dependencias explícitas, finales
    // y fáciles de reemplazar en una prueba.
    public PedidoService(PedidoRepository pedidos,
                         InventarioRepository inventarios,
                         FacturaService facturas,
                         BitacoraService bitacora,
                         AuditoriaService auditoria) {
        this.pedidos = pedidos;
        this.inventarios = inventarios;
        this.facturas = facturas;
        this.bitacora = bitacora;
        this.auditoria = auditoria;
    }

    // @Transactional en el método de SERVICIO: la transacción abarca el
    // caso de uso completo. Si CUALQUIER paso lanza una RuntimeException,
    // los anteriores se deshacen (rollback): no existen "medios pedidos".
    @Transactional
    public Pedido confirmarPedido(Long pedidoId) {
        var pedido = pedidos.findById(pedidoId)
                .orElseThrow(() -> new PedidoNoExisteException(pedidoId));

        // Validar en la frontera, ANTES de tocar el estado del sistema.
        if (pedido.estaConfirmado()) {
            throw new PedidoYaConfirmadoException(pedidoId);
        }

        try {
            // Paso 1 · Reservar inventario (regla: no vender lo que no hay).
            // saveAndFlush envía el UPDATE a la base YA — así, si el paso 2
            // falla, se ve en el log que hubo un UPDATE y luego un rollback.
            var inventario = pedido.getProducto();
            inventario.reservar(pedido.getCantidad());
            inventarios.saveAndFlush(inventario);

            // Paso 2 · Crear la factura (regla: límite de facturación).
            facturas.crear(pedido);

            // Paso 3 · Registrar en la bitácora del proceso.
            bitacora.registrar(pedido, "confirmado");

            pedido.confirmar();
            auditoria.registrarIntento(pedidoId, "OK");
            return pedidos.save(pedido);

        } catch (RuntimeException e) {
            // La auditoría corre con REQUIRES_NEW: se guarda AUNQUE esta
            // transacción haga rollback. Y se relanza la excepción: si se
            // tragara aquí, Spring intentaría confirmar una transacción ya
            // marcada para rollback (UnexpectedRollbackException).
            auditoria.registrarIntento(pedidoId, "FALLO: " + e.getMessage());
            throw e;
        }
    }
}
