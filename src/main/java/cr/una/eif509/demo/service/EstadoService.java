package cr.una.eif509.demo.service;

import cr.una.eif509.demo.repository.BitacoraRepository;
import cr.una.eif509.demo.repository.FacturaRepository;
import cr.una.eif509.demo.repository.IntentoConfirmacionRepository;
import cr.una.eif509.demo.repository.InventarioRepository;
import cr.una.eif509.demo.repository.PedidoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Solo para la demo: imprime el estado de las tablas que toca el proceso,
// para ver el antes/después sin salir de la consola. (En clase, el mismo
// estado se consulta con SQL directo — ver README.)
@Service
public class EstadoService {

    private final InventarioRepository inventarios;
    private final PedidoRepository pedidos;
    private final FacturaRepository facturas;
    private final BitacoraRepository bitacora;
    private final IntentoConfirmacionRepository intentos;

    public EstadoService(InventarioRepository inventarios,
                         PedidoRepository pedidos,
                         FacturaRepository facturas,
                         BitacoraRepository bitacora,
                         IntentoConfirmacionRepository intentos) {
        this.inventarios = inventarios;
        this.pedidos = pedidos;
        this.facturas = facturas;
        this.bitacora = bitacora;
        this.intentos = intentos;
    }

    // Se arma todo el texto primero y se imprime al final, en bloque,
    // para que el SQL del log no se mezcle con el estado.
    @Transactional(readOnly = true)
    public void imprimir() {
        var sb = new StringBuilder();
        sb.append("\n---------------- ESTADO DE LA BASE ----------------\n");

        sb.append("inventario:\n");
        inventarios.findAll(Sort.by("id")).forEach(i ->
                sb.append(String.format("  %-18s disponible=%-3d version=%d%n",
                        i.getProducto(), i.getDisponible(), i.getVersion())));

        sb.append("pedidos:\n");
        pedidos.findAllConClienteYProducto().forEach(p ->
                sb.append(String.format("  #%-2d %-13s %-18s x%d  total=%-9s %s%n",
                        p.getId(), p.getCliente().getNombre(), p.getProducto().getProducto(),
                        p.getCantidad(), p.getTotal(), p.getEstado())));

        sb.append("facturas:\n");
        facturas.findAll(Sort.by("id")).forEach(f ->
                sb.append(String.format("  #%d pedido=%d monto=%s%n",
                        f.getId(), f.getPedido().getId(), f.getMonto())));

        sb.append("bitacora:\n");
        bitacora.findAll(Sort.by("id")).forEach(b ->
                sb.append(String.format("  #%d pedido=%d evento=%s%n",
                        b.getId(), b.getPedido().getId(), b.getEvento())));

        sb.append("intento_confirmacion (auditoría, REQUIRES_NEW):\n");
        intentos.findAll(Sort.by("id")).forEach(i ->
                sb.append(String.format("  #%d pedido=%d %s%n",
                        i.getId(), i.getPedidoId(), i.getResultado())));

        sb.append("---------------------------------------------------");
        System.out.println(sb);
    }
}
