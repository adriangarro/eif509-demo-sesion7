package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Lección de la Sesión 5 aplicada: para listar pedidos con su cliente
    // y su producto se traen en UN solo viaje (sin N+1).
    @Query("SELECT p FROM Pedido p JOIN FETCH p.cliente JOIN FETCH p.producto ORDER BY p.id")
    List<Pedido> findAllConClienteYProducto();
}
