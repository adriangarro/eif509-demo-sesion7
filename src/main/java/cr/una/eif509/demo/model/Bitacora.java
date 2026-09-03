package cr.una.eif509.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "bitacora")
public class Bitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false)
    private String evento;

    @Column(name = "registrado_en", insertable = false, updatable = false)
    private OffsetDateTime registradoEn;

    protected Bitacora() {
        // Constructor sin argumentos requerido por JPA.
    }

    public Bitacora(Pedido pedido, String evento) {
        this.pedido = pedido;
        this.evento = evento;
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public String getEvento() {
        return evento;
    }

    public OffsetDateTime getRegistradoEn() {
        return registradoEn;
    }
}
