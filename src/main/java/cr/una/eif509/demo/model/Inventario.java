package cr.una.eif509.demo.model;

import cr.una.eif509.demo.excepcion.InventarioInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventario")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String producto;

    @Column(nullable = false)
    private int disponible;

    // Bloqueo optimista: Hibernate genera
    //   UPDATE inventario SET disponible=?, version=N+1 WHERE id=? AND version=N
    // Si otra transacción ganó (0 filas afectadas), lanza
    // OptimisticLockException en vez de pisar su cambio.
    @Version
    private Long version;

    protected Inventario() {
        // Constructor sin argumentos requerido por JPA.
    }

    // Invariante simple: vive en la ENTIDAD (equilibrio del curso).
    // El CHECK (disponible >= 0) del esquema es la defensa en profundidad.
    public void reservar(int cantidad) {
        if (disponible < cantidad) {
            throw new InventarioInsuficienteException(producto, disponible, cantidad);
        }
        disponible -= cantidad;
    }

    public Long getId() {
        return id;
    }

    public String getProducto() {
        return producto;
    }

    public int getDisponible() {
        return disponible;
    }

    public Long getVersion() {
        return version;
    }
}
