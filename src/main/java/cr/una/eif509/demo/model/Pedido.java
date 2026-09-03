package cr.una.eif509.demo.model;

import cr.una.eif509.demo.excepcion.PedidoYaConfirmadoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Criterio del curso: TODO perezoso por defecto (Sesión 5).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Inventario producto;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    // La base pone el valor con DEFAULT now(); aquí solo se lee.
    @Column(name = "creado_en", insertable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Pedido() {
        // Constructor sin argumentos requerido por JPA.
    }

    public Pedido(Cliente cliente, Inventario producto, int cantidad, BigDecimal total) {
        this.cliente = cliente;
        this.producto = producto;
        this.cantidad = cantidad;
        this.total = total;
        this.estado = EstadoPedido.CREADO;
    }

    // Invariante de la entidad: un pedido se confirma UNA vez.
    public void confirmar() {
        if (estaConfirmado()) {
            throw new PedidoYaConfirmadoException(id);
        }
        estado = EstadoPedido.CONFIRMADO;
    }

    public boolean estaConfirmado() {
        return estado == EstadoPedido.CONFIRMADO;
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Inventario getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
