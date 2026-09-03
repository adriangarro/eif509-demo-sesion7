# EIF509 · Demo Sesión 7 — Servicios y transacciones: el rollback observable

Repositorio de demostración del curso **EIF509 "Desarrollo de Aplicaciones
Basadas en Web"** (Universidad Nacional, Costa Rica).

Esta demo continúa el proyecto de la
[Sesión 6 (Testcontainers)](https://github.com/adriangarro/eif509-demo-sesion6):
mismas entidades, mismas pruebas — ahora con una **capa de servicios** que
orquesta un proceso de negocio de tres pasos y lo protege con
`@Transactional`. La serie completa:
[Sesión 3 (PostgreSQL + Flyway)](https://github.com/adriangarro/eif509-demo-sesion3)
· [Sesión 4 (MongoDB)](https://github.com/adriangarro/eif509-demo-sesion4)
· [Sesión 5 (JPA y N+1)](https://github.com/adriangarro/eif509-demo-sesion5)
· [Sesión 6 (Testcontainers)](https://github.com/adriangarro/eif509-demo-sesion6).

## ¿Qué van a construir?

El proceso **confirmar pedido** = reservar inventario + crear factura +
registrar bitácora: **juntos o ninguno**. Al final de esta guía habrán
visto, con sus propios ojos y en la base de datos:

1. **La corrida feliz**: las tres tablas cambian en una sola transacción.
2. **El fallo inyectado**: la factura revienta en el paso 2 por una regla
   de negocio, y el inventario reservado en el paso 1 **vuelve solo**
   (rollback total, cero basura).
3. **La auditoría que sobrevive**: un registro con `REQUIRES_NEW` queda
   guardado *aunque* el proceso principal haga rollback.
4. **La prueba que lo garantiza para siempre**: `assertThatThrownBy` +
   verificación de que el estado quedó intacto, sobre un PostgreSQL real
   (Testcontainers, Sesión 6).

Y de paso, los criterios de la sesión en código:

- Las **reglas viven en el servicio**; las **invariantes simples en la
  entidad** (`Inventario.reservar`, `Pedido.confirmar`); el `CHECK` del
  esquema es la defensa en profundidad.
- Cada regla violada produce una **excepción de negocio nombrada**
  (`InventarioInsuficienteException`, `MontoFacturaExcedidoException`,
  `PedidoYaConfirmadoException`, `PedidoNoExisteException`) que extiende
  `RuntimeException` — por eso dispara el rollback.
- **Bloqueo optimista** con `@Version` en `inventario`: se ve en el
  `UPDATE ... WHERE id=? AND version=?` del log.

## Requisitos previos

| Herramienta | Versión mínima | Descarga oficial | Cómo verificar |
|---|---|---|---|
| Docker Desktop | 4.x | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) | `docker --version` |
| JDK (Java) | 21 | [adoptium.net](https://adoptium.net/) | `java -version` |
| Git | 2.30 | [git-scm.com/downloads](https://git-scm.com/downloads) | `git --version` |

No necesitan instalar Gradle (el proyecto trae `gradlew`) ni PostgreSQL
(corre en Docker). Las notas por sistema operativo (WSL2 en Windows, el
`JAVA_HOME` de Homebrew en macOS) son idénticas a las sesiones
anteriores: ver el
[README de la Sesión 5](https://github.com/adriangarro/eif509-demo-sesion5#requisitos-previos).
En macOS con Homebrew, en la terminal de la demo:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

## Estructura del proyecto

Lo nuevo respecto a la Sesión 6 está marcado con `←`:

```text
└── src/
    ├── main/resources/db/migration/
    │   └── V4__inventario_factura_bitacora.sql   ← inventario, factura, bitacora, auditoría
    ├── main/java/cr/una/eif509/demo/
    │   ├── model/
    │   │   ├── Pedido.java                       ← + producto, cantidad, estado, confirmar()
    │   │   ├── Inventario.java                   ← reservar() + @Version
    │   │   ├── Factura.java, Bitacora.java       ← nuevas
    │   │   └── IntentoConfirmacion.java          ← nueva (auditoría)
    │   ├── excepcion/                            ← ExcepcionDeNegocio y las 4 reglas
    │   ├── repository/                           ← uno por entidad nueva
    │   ├── service/
    │   │   ├── PedidoService.java                ← confirmarPedido() @Transactional
    │   │   ├── FacturaService.java               ← regla del límite (REQUIRED)
    │   │   ├── BitacoraService.java              ← paso 3 (REQUIRED)
    │   │   ├── AuditoriaService.java             ← registrarIntento() REQUIRES_NEW
    │   │   └── EstadoService.java                ← imprime el estado (solo demo)
    │   └── DemoApplication.java                  ← elige la demo por argumento
    └── test/java/cr/una/eif509/demo/
        ├── PostgresContainerBase.java            ← contenedor compartido (singleton)
        ├── repository/PedidoRepositoryIT.java    (las 3 pruebas de la Sesión 6)
        └── service/PedidoServiceIT.java          ← las 5 pruebas del proceso
```

## El esquema (migración V4)

El esquema evoluciona **sin romper los datos** de las sesiones anteriores:

| Tabla | Qué es | Reglas del esquema |
|---|---|---|
| `inventario` | 4 productos con existencias | `CHECK (disponible >= 0)`, columna `version` para `@Version` |
| `pedido` (+ columnas) | ahora sabe qué producto lleva, cuántos y en qué estado va | `estado IN ('CREADO','CONFIRMADO')`, `cantidad > 0` |
| `factura` | una por pedido confirmado | `pedido_id UNIQUE`, `CHECK (monto > 0)` |
| `bitacora` | evento del proceso (paso 3) | vive **dentro** de la transacción |
| `intento_confirmacion` | auditoría de cada intento | **sin FK** a propósito: registra hasta pedidos inexistentes |

Los pedidos semilla que usa la demo:

| Pedido | Producto | Qué pasa al confirmarlo |
|---|---|---|
| **1** | Teclado x1, ₡15 000 | **Corrida feliz** |
| **9** | Laptop x1, ₡66 000 | Falla en el paso 2: supera el límite de facturación automática (₡50 000) |
| **7** | Silla x3 (hay 2) | Falla en el paso 1: inventario insuficiente |

## Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/adriangarro/eif509-demo-sesion7.git
cd eif509-demo-sesion7
```

### 2. Verificar que Docker Desktop está corriendo

```bash
docker info
```

Si ven `Cannot connect to the Docker daemon`, abran Docker Desktop y
esperen el ícono estable.

### 3. Levantar PostgreSQL

```bash
docker compose up -d
docker ps
```

Deben ver `eif509-demo-sesion7-db-1` en estado `Up`. Las migraciones
V1..V4 se aplican solas al correr cualquier demo.

### 4. Verificar Java

```bash
java -version
```

Debe reportar 21 o superior (en macOS con el `export` sin sudo,
verifiquen con `"$JAVA_HOME/bin/java" -version`).

## Ejecución de la demo

La demo se elige por argumento. Cada corrida imprime el **log de
transacciones** (líneas `JpaTransactionManager`), el **SQL** que Hibernate
envía, y al final el **estado de la base** en bloque.

### 5. Corrida feliz: las tres tablas cambian

```bash
./gradlew bootRun --args='feliz'
```

Qué observar en el log, en este orden:

```text
Creating new transaction with name [...PedidoService.confirmarPedido]: PROPAGATION_REQUIRED
select ... from pedido ...              <- paso 0: cargar el pedido
select ... from inventario ...
update inventario set disponible=?, producto=?, version=? where id=? and version=?   <- paso 1
Participating in existing transaction   <- FacturaService.crear se UNE (REQUIRED)
insert into factura (monto, pedido_id) ...                                           <- paso 2
insert into bitacora (evento, pedido_id) ...                                         <- paso 3
Suspending current transaction, creating new transaction with name [...AuditoriaService.registrarIntento]
insert into intento_confirmacion ...    <- auditoría en SU transacción
Initiating transaction commit           <- commit de la auditoría
Resuming suspended transaction after completion of inner transaction
Initiating transaction commit           <- commit del proceso completo
>>> RESULTADO: pedido 1 CONFIRMADO
```

Y el estado:

```text
inventario:
  Teclado mecánico   disponible=9   version=1     <- era 10, version 0
  ...
pedidos:
  #1  Ana Rojas     Teclado mecánico   x1  total=15000.00  CONFIRMADO
  ...
facturas:
  #1 pedido=1 monto=15000.00
bitacora:
  #1 pedido=1 evento=confirmado
intento_confirmacion (auditoría, REQUIRES_NEW):
  #1 pedido=1 OK
```

Tres escrituras, una transacción, un commit. Fíjense en el `UPDATE` de
inventario: `WHERE id=? AND version=?` — eso es el bloqueo optimista de
`@Version`. Si otra transacción hubiera cambiado la fila entre la lectura
y la escritura, afectaría 0 filas y Hibernate lanzaría
`OptimisticLockException` en vez de pisar el cambio ajeno.

### 6. Verlo en la base con SQL directo

```bash
docker compose exec db psql -U dev -d eif509 -c 'SELECT id, producto, disponible, version FROM inventario ORDER BY id;'
docker compose exec db psql -U dev -d eif509 -c 'SELECT * FROM factura;'
docker compose exec db psql -U dev -d eif509 -c 'SELECT id, pedido_id, evento FROM bitacora;'
```

Salida esperada: el teclado con `disponible = 9` y `version = 1`, una
factura y una fila de bitácora, ambas del pedido 1.

### 7. Inyectar el fallo: la factura revienta en el paso 2

```bash
./gradlew bootRun --args='fallo'
```

El pedido 9 vale ₡66 000, por encima del límite de facturación
automática. Qué observar:

```text
Creating new transaction with name [...PedidoService.confirmarPedido]
select ... from pedido ... / select ... from inventario ...
update inventario set disponible=?, ... where id=? and version=?      <- el paso 1 SÍ escribió
Participating in existing transaction                                 <- FacturaService.crear entra...
Participating transaction failed - marking existing transaction as rollback-only   <- ...y lanza la regla
Suspending current transaction, creating new transaction with name [...AuditoriaService.registrarIntento]
insert into intento_confirmacion ...
Initiating transaction commit                                         <- la auditoría se confirma sola
Resuming suspended transaction after completion of inner transaction
Initiating transaction rollback                                       <- el proceso completo se deshace
Rolling back JPA transaction on EntityManager [...]
>>> EXCEPCIÓN DE NEGOCIO: MontoFacturaExcedidoException
>>> El pedido 9 por 66000.00 supera el límite de facturación automática (50000.00): requiere aprobación manual
```

Y el estado:

```text
inventario:
  ...
  Laptop 14"         disponible=3   version=0     <- INTACTO: el UPDATE se deshizo
pedidos:
  #9  Ana Rojas     Laptop 14"         x1  total=66000.00  CREADO
facturas:
  #1 pedido=1 monto=15000.00                     <- solo la del pedido 1
bitacora:
  #1 pedido=1 evento=confirmado                  <- solo la del pedido 1
intento_confirmacion (auditoría, REQUIRES_NEW):
  #1 pedido=1 OK
  #2 pedido=9 FALLO: El pedido 9 por 66000.00 supera el límite ...   <- SOBREVIVIÓ al rollback
```

El punto de la demo: el `UPDATE` de inventario **se envió a la base** y
aun así el inventario quedó intacto. Eso es atomicidad: el proceso
completo o nada. No existen los "medios pedidos".

### 8. Confirmarlo con SQL directo

```bash
docker compose exec db psql -U dev -d eif509 -c 'SELECT id, producto, disponible, version FROM inventario ORDER BY id;'
docker compose exec db psql -U dev -d eif509 -c "SELECT id, cantidad, total, estado FROM pedido WHERE id IN (1, 9);"
docker compose exec db psql -U dev -d eif509 -c 'SELECT id, pedido_id, resultado FROM intento_confirmacion ORDER BY id;'
```

Laptop sigue en 3 con `version = 0`; el pedido 9 sigue `CREADO`; la
auditoría tiene el intento fallido.

### 9. (Extra) Fallar en el paso 1: sin inventario

```bash
./gradlew bootRun --args='sinstock'
```

El pedido 7 pide 3 sillas y hay 2. La invariante de la **entidad**
(`Inventario.reservar`) lanza `InventarioInsuficienteException` antes de
que salga ningún `UPDATE`: en el log no hay escrituras del proceso, solo
la auditoría y el rollback.

### 10. La prueba que lo garantiza para siempre

```bash
./gradlew test
```

No hace falta la base del compose: Testcontainers levanta un PostgreSQL
propio (Sesión 6). Salida esperada: `BUILD SUCCESSFUL`, 8 pruebas en
verde (3 de repositorio + 5 del servicio). La que importa hoy, en
[`PedidoServiceIT`](src/test/java/cr/una/eif509/demo/service/PedidoServiceIT.java):

```java
@Test
void falloEnLaFacturaDeshaceLaReservaDeInventario() {
    int antes = disponible(LAPTOP);

    assertThatThrownBy(() -> servicio.confirmarPedido(9L))
            .isInstanceOf(MontoFacturaExcedidoException.class);

    assertThat(disponible(LAPTOP)).isEqualTo(antes);      // rollback del paso 1
    assertThat(facturas.findByPedidoId(9L)).isEmpty();    // el paso 2 nunca escribió
    assertThat(bitacora.findByPedidoId(9L)).isEmpty();    // el paso 3 nunca corrió
    assertThat(intentos.findByPedidoId(9L)).hasSize(1);   // la auditoría sobrevivió
}
```

Este es el guion literal del Lab 4: *proceso multi-paso donde un fallo
intermedio provoca rollback verificable mediante una prueba*.

### 11. El mismo verde en el CI

Cada push corre la suite completa en GitHub Actions
([.github/workflows/ci.yml](.github/workflows/ci.yml)) con Testcontainers.

## Cómo leer el log de transacciones

| Línea del log | Qué significa |
|---|---|
| `Creating new transaction with name [...]` | Empieza la transacción del método de servicio |
| `Participating in existing transaction` | Un servicio anidado con `REQUIRED` se **une** a la que ya existe |
| `Participating transaction failed - marking existing transaction as rollback-only` | Un servicio anidado lanzó: la transacción compartida ya no puede confirmarse |
| `Suspending current transaction, creating new transaction with name [...]` | `REQUIRES_NEW`: la actual se pausa y nace una independiente |
| `Resuming suspended transaction after completion of inner transaction` | La independiente terminó (con su propio commit); vuelve la principal |
| `Initiating transaction commit` | Todo lo enviado se confirma |
| `Initiating transaction rollback` | Todo lo enviado se deshace |

Dos detalles de diseño que se ven en el código:

- `PedidoService` **relanza** la excepción después de auditar. Si la
  tragara, Spring intentaría confirmar una transacción ya marcada
  `rollback-only` y fallaría con `UnexpectedRollbackException`.
- `AuditoriaService` es **otro bean**. `@Transactional` funciona por
  proxy: una llamada a un método de la misma clase no pasa por el proxy
  y `REQUIRES_NEW` no tendría efecto.

## Comandos útiles

| Acción | Comando |
|---|---|
| Levantar la base | `docker compose up -d` |
| Correr una demo | `./gradlew bootRun --args='feliz'` (o `fallo`, `sinstock`, `estado`) |
| Ver el estado sin confirmar nada | `./gradlew bootRun --args='estado'` |
| Correr las pruebas | `./gradlew test` |
| Reiniciar la demo desde cero | `docker compose down -v && docker compose up -d` |
| Detener (conserva los datos) | `docker compose down` |

Ojo: `feliz` confirma el pedido 1; correrlo **dos veces** produce
`PedidoYaConfirmadoException` — la invariante funciona. Para repetir la
demo desde cero, reinicien la base (`down -v`).

## Solución de problemas

| Problema | Causa | Solución |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop apagado | Abrirlo, esperar el ícono estable, reintentar |
| `Connection to localhost:5432 refused` en `bootRun` | La base del compose no está arriba | `docker compose up -d` |
| `PedidoYaConfirmadoException` en `feliz` | Ya se corrió antes | Es correcto; `docker compose down -v` para repetir desde cero |
| `Could not find a valid Docker environment` en las pruebas con Docker corriendo | Testcontainers viejo vs Docker Engine 29 | Ya resuelto: `ext['testcontainers.version'] = '1.21.4'` en `build.gradle` (ver Sesión 6) |
| `Could not open JPA EntityManager for transaction` en la segunda clase de prueba | Un contenedor por clase + contexto de Spring cacheado | Contenedor **compartido** (singleton) en `PostgresContainerBase`; ver el comentario ahí |
| `Schema-validation: ...` al arrancar | Una entidad no coincide con V4 | Leer el error: dice qué columna o tipo falló |
| `Unable to locate a Java Runtime` | Falta el JDK o `JAVA_HOME` | Ver Requisitos previos |

## Relación con el Laboratorio 4

El Lab 4 (se asigna la próxima semana) pide exactamente esto sobre **su
propio dominio**: el servicio del proceso de negocio #1 con su frontera
transaccional, **3 o más reglas de negocio con excepción nombrada y una
prueba que la exige**, y un proceso multi-paso donde un fallo intermedio
provoque **rollback verificable mediante una prueba**. El taller de hoy
(listar los pasos del proceso, nombrar las excepciones, esqueleto del
servicio `@Transactional`) es el 40 % del laboratorio resuelto por
adelantado. Este repo es el mapa; su proceso es el territorio.

---

> **Material de referencia del curso.** El servicio de su laboratorio
> debe nacer de su propio proceso de negocio; no copien este ejemplo.
