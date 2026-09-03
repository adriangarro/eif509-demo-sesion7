package cr.una.eif509.demo.service;

import cr.una.eif509.demo.model.IntentoConfirmacion;
import cr.una.eif509.demo.repository.IntentoConfirmacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final IntentoConfirmacionRepository intentos;

    public AuditoriaService(IntentoConfirmacionRepository intentos) {
        this.intentos = intentos;
    }

    // REQUIRES_NEW: suspende la transacción actual y abre una nueva e
    // independiente. Se confirma (commit) por su cuenta, así que el
    // registro sobrevive aunque confirmarPedido haga rollback:
    // la auditoría sobrevive al fallo.
    //
    // Debe estar en OTRO bean: @Transactional funciona por proxy, y una
    // llamada a un método de la misma clase no pasa por el proxy.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntento(Long pedidoId, String resultado) {
        intentos.save(new IntentoConfirmacion(pedidoId, resultado));
    }
}
