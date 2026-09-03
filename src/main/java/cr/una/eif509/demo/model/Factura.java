package cr.una.eif509.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "factura")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Una factura por pedido: el UNIQUE del esquema lo garantiza.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "emitida_en", insertable = false, updatable = false)
    private OffsetDateTime emitidaEn;

    protected Factura() {
        // Constructor sin argumentos requerido por JPA.
    }

    public Factura(Pedido pedido, BigDecimal monto) {
        this.pedido = pedido;
        this.monto = monto;
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public OffsetDateTime getEmitidaEn() {
        return emitidaEn;
    }
}
