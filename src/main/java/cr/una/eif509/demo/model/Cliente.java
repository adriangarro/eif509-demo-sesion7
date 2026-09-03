package cr.una.eif509.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

// @Entity convierte la clase en una tabla gestionada por el ORM.
// Importante: el esquema ya existe (Flyway). La entidad debe
// COINCIDIR con la tabla cliente, no crearla (ddl-auto=validate).
@Entity
@Table(name = "cliente")
public class Cliente {

    // IDENTITY delega la numeración a PostgreSQL
    // (la columna es GENERATED ALWAYS AS IDENTITY).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    // Lado inverso de la relación: el cliente ve sus pedidos,
    // sin columna adicional en esta tabla.
    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos;

    protected Cliente() {
        // Constructor sin argumentos requerido por JPA.
    }

    public Cliente(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }
}
