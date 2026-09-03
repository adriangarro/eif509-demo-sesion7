package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {
}
