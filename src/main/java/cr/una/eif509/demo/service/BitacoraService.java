package cr.una.eif509.demo.service;

import cr.una.eif509.demo.model.Bitacora;
import cr.una.eif509.demo.model.Pedido;
import cr.una.eif509.demo.repository.BitacoraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BitacoraService {

    private final BitacoraRepository bitacora;

    public BitacoraService(BitacoraRepository bitacora) {
        this.bitacora = bitacora;
    }

    // REQUIRED: forma parte del proceso. Si el proceso se deshace,
    // esta fila también desaparece.
    @Transactional
    public Bitacora registrar(Pedido pedido, String evento) {
        return bitacora.saveAndFlush(new Bitacora(pedido, evento));
    }
}
