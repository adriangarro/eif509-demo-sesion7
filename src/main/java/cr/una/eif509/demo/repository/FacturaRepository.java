package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    // Método derivado: Spring Data navega pedido.id y genera el WHERE.
    Optional<Factura> findByPedidoId(Long pedidoId);
}
