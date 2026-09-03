package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.model.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    List<Bitacora> findByPedidoId(Long pedidoId);
}
