package cr.una.eif509.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

// Auditoría: registra CADA intento de confirmar, haya salido bien o mal.
// Guarda el id como número (sin relación) para poder registrar incluso
// intentos sobre pedidos que no existen.
@Entity
@Table(name = "intento_confirmacion")
public class IntentoConfirmacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private String resultado;

    @Column(name = "registrado_en", insertable = false, updatable = false)
    private OffsetDateTime registradoEn;

    protected IntentoConfirmacion() {
        // Constructor sin argumentos requerido por JPA.
    }

    public IntentoConfirmacion(Long pedidoId, String resultado) {
        this.pedidoId = pedidoId;
        this.resultado = resultado;
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public String getResultado() {
        return resultado;
    }

    public OffsetDateTime getRegistradoEn() {
        return registradoEn;
    }
}
