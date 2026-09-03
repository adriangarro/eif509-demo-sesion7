package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
