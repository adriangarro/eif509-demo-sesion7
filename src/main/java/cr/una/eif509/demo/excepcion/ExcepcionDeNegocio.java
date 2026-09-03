package cr.una.eif509.demo.excepcion;

// Un error de negocio NO es una excepción técnica: es una respuesta
// esperada del dominio, con nombre. Extiende RuntimeException porque
// @Transactional hace rollback automático ante cualquier RuntimeException
// — así, violar una regla deshace el proceso completo.
public abstract class ExcepcionDeNegocio extends RuntimeException {

    protected ExcepcionDeNegocio(String mensaje) {
        super(mensaje);
    }
}
