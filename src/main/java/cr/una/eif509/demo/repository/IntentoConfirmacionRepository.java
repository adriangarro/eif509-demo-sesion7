package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.model.IntentoConfirmacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntentoConfirmacionRepository extends JpaRepository<IntentoConfirmacion, Long> {

    List<IntentoConfirmacion> findByPedidoId(Long pedidoId);
}
