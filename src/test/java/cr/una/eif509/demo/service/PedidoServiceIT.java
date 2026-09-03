package cr.una.eif509.demo.service;

import cr.una.eif509.demo.PostgresContainerBase;
import cr.una.eif509.demo.excepcion.InventarioInsuficienteException;
import cr.una.eif509.demo.excepcion.MontoFacturaExcedidoException;
import cr.una.eif509.demo.excepcion.PedidoNoExisteException;
import cr.una.eif509.demo.excepcion.PedidoYaConfirmadoException;
import cr.una.eif509.demo.model.EstadoPedido;
import cr.una.eif509.demo.repository.BitacoraRepository;
import cr.una.eif509.demo.repository.FacturaRepository;
import cr.una.eif509.demo.repository.IntentoConfirmacionRepository;
import cr.una.eif509.demo.repository.InventarioRepository;
import cr.una.eif509.demo.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// La prueba que lo garantiza para siempre: cada regla de negocio violada
// produce su excepción nombrada, Y el estado de la base queda intacto.
// Datos semilla (V4): pedido 1 = Teclado x1, 15000 (feliz);
// pedido 9 = Laptop x1, 66000 (supera el límite de facturación);
// pedido 7 = Silla x3 con solo 2 disponibles (sin inventario).
//
// Las pruebas comparten el contenedor y JUnit no garantiza el orden:
// cada una usa su propio pedido y compara el inventario ANTES/DESPUÉS.
class PedidoServiceIT extends PostgresContainerBase {

    static final long TECLADO = 1L;
    static final long MONITOR = 2L;
    static final long SILLA = 3L;
    static final long LAPTOP = 4L;

    @Autowired PedidoService servicio;
    @Autowired PedidoRepository pedidos;
    @Autowired InventarioRepository inventarios;
    @Autowired FacturaRepository facturas;
    @Autowired BitacoraRepository bitacora;
    @Autowired IntentoConfirmacionRepository intentos;

    @Test
    void corridaFelizEscribeLasTresTablas() {
        int antes = disponible(TECLADO);

        var pedido = servicio.confirmarPedido(1L);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(disponible(TECLADO)).isEqualTo(antes - 1);
        assertThat(facturas.findByPedidoId(1L)).isPresent();
        assertThat(bitacora.findByPedidoId(1L)).hasSize(1);
        assertThat(intentos.findByPedidoId(1L))
                .singleElement()
                .satisfies(i -> assertThat(i.getResultado()).isEqualTo("OK"));
    }

    // El guion literal del Lab 4: un fallo intermedio (paso 2) provoca
    // rollback verificable — el inventario reservado en el paso 1 vuelve.
    @Test
    void falloEnLaFacturaDeshaceLaReservaDeInventario() {
        int antes = disponible(LAPTOP);

        assertThatThrownBy(() -> servicio.confirmarPedido(9L))
                .isInstanceOf(MontoFacturaExcedidoException.class);

        assertThat(disponible(LAPTOP)).isEqualTo(antes);            // rollback del paso 1
        assertThat(facturas.findByPedidoId(9L)).isEmpty();          // el paso 2 nunca escribió
        assertThat(bitacora.findByPedidoId(9L)).isEmpty();          // el paso 3 nunca corrió
        assertThat(pedidos.findById(9L).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CREADO);

        // ...pero la auditoría (REQUIRES_NEW) sobrevivió al rollback.
        assertThat(intentos.findByPedidoId(9L))
                .singleElement()
                .satisfies(i -> assertThat(i.getResultado()).startsWith("FALLO"));
    }

    @Test
    void sinInventarioFallaEnElPaso1YNoTocaNada() {
        int antes = disponible(SILLA);

        assertThatThrownBy(() -> servicio.confirmarPedido(7L))
                .isInstanceOf(InventarioInsuficienteException.class);

        assertThat(disponible(SILLA)).isEqualTo(antes);
        assertThat(facturas.findByPedidoId(7L)).isEmpty();
        assertThat(intentos.findByPedidoId(7L)).hasSize(1);
    }

    @Test
    void noConfirmaElMismoPedidoDosVeces() {
        int antes = disponible(MONITOR);

        servicio.confirmarPedido(2L);
        assertThatThrownBy(() -> servicio.confirmarPedido(2L))
                .isInstanceOf(PedidoYaConfirmadoException.class);

        // El inventario se descontó exactamente una vez.
        assertThat(disponible(MONITOR)).isEqualTo(antes - 1);
        assertThat(facturas.findByPedidoId(2L)).isPresent();
    }

    @Test
    void pedidoInexistenteEsUnErrorDeNegocioNombrado() {
        assertThatThrownBy(() -> servicio.confirmarPedido(999L))
                .isInstanceOf(PedidoNoExisteException.class);
    }

    private int disponible(long inventarioId) {
        return inventarios.findById(inventarioId).orElseThrow().getDisponible();
    }
}
