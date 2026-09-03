package cr.una.eif509.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

// Lo común de las pruebas de integración, escrito UNA vez (Sesión 6).
//
// Patrón "singleton container": UN solo PostgreSQL 16 real para toda la
// suite, arrancado en el bloque static. ¿Por qué no @Container por clase?
// Spring cachea el ApplicationContext entre clases de prueba con la misma
// configuración, y ese contexto guarda la URL del contenedor. Con un
// contenedor por clase, la segunda clase reusaría el contexto apuntando
// a un contenedor que ya murió: "Could not open JPA EntityManager".
// Testcontainers (ryuk) apaga el contenedor al terminar la JVM.
@SpringBootTest
public abstract class PostgresContainerBase {

    static final PostgreSQLContainer<?> db =
            new PostgreSQLContainer<>("postgres:16");

    static {
        db.start();
    }

    // Le pasa a Spring la URL del contenedor, con su puerto aleatorio.
    // Al arrancar, Flyway corre las migraciones REALES (V1..V4) adentro.
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
    }
}
