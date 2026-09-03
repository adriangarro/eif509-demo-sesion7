package cr.una.eif509.demo;

import cr.una.eif509.demo.excepcion.ExcepcionDeNegocio;
import cr.una.eif509.demo.service.EstadoService;
import cr.una.eif509.demo.service.PedidoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // La demo se elige por argumento:
    //   ./gradlew bootRun --args='feliz'     -> confirma el pedido 1: las 3 tablas cambian
    //   ./gradlew bootRun --args='fallo'     -> pedido 9: la factura revienta en el paso 2, rollback total
    //   ./gradlew bootRun --args='sinstock'  -> pedido 7: no hay inventario, falla en el paso 1
    //   ./gradlew bootRun --args='estado'    -> solo imprime el estado de la base
    @Bean
    CommandLineRunner demo(PedidoService pedidoService, EstadoService estado) {
        return args -> {
            String cual = args.length > 0 ? args[0] : "";
            switch (cual) {
                case "feliz" -> confirmar(pedidoService, estado, 1L);
                case "fallo" -> confirmar(pedidoService, estado, 9L);
                case "sinstock" -> confirmar(pedidoService, estado, 7L);
                case "estado" -> estado.imprimir();
                default -> System.out.println(
                        "Uso: ./gradlew bootRun --args='feliz | fallo | sinstock | estado'");
            }
        };
    }

    private static void confirmar(PedidoService pedidoService, EstadoService estado, Long pedidoId) {
        System.out.println();
        System.out.println("==========================================================");
        System.out.println(">>> confirmarPedido(" + pedidoId + ")");
        System.out.println("==========================================================");
        try {
            var pedido = pedidoService.confirmarPedido(pedidoId);
            System.out.println(">>> RESULTADO: pedido " + pedido.getId() + " " + pedido.getEstado());
        } catch (ExcepcionDeNegocio e) {
            // El servicio decide QUÉ falló; quien lo llama decide CÓMO contarlo.
            System.out.println(">>> EXCEPCIÓN DE NEGOCIO: " + e.getClass().getSimpleName());
            System.out.println(">>> " + e.getMessage());
        }
        estado.imprimir();
    }
}
