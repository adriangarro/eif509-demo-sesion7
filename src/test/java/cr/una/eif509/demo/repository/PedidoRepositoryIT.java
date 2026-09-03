package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.PostgresContainerBase;
import cr.una.eif509.demo.model.Cliente;
import cr.una.eif509.demo.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Las pruebas de la Sesión 6, ahora sobre el esquema V4
// (un pedido lleva producto y cantidad).
class PedidoRepositoryIT extends PostgresContainerBase {

    @Autowired ClienteRepository clienteRepository;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired InventarioRepository inventarioRepository;

    @Test
    void guardaYRecuperaUnPedido() {
        var cliente = clienteRepository.save(new Cliente("Ana", "ana@mail.com"));
        var teclado = inventarioRepository.findById(1L).orElseThrow();
        var pedido = pedidoRepository.save(
                new Pedido(cliente, teclado, 1, BigDecimal.TEN));

        assertThat(pedidoRepository.findById(pedido.getId())).isPresent();
    }

    @Test
    void rechazaCorreoDuplicado() {
        clienteRepository.save(new Cliente("María", "maria@mail.com"));

        assertThatThrownBy(() ->
                clienteRepository.saveAndFlush(new Cliente("Otra", "maria@mail.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaTotalNegativo() {
        var cliente = clienteRepository.save(new Cliente("Luis", "luis@mail.com"));
        var teclado = inventarioRepository.findById(1L).orElseThrow();

        assertThatThrownBy(() ->
                pedidoRepository.saveAndFlush(
                        new Pedido(cliente, teclado, 1, new BigDecimal("-5"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
