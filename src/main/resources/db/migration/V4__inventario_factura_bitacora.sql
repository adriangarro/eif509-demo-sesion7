-- Sesión 7: lo que necesita el proceso "confirmar pedido"
-- (reservar inventario -> crear factura -> registrar bitácora).
-- V1..V3 son las mismas migraciones de las sesiones anteriores:
-- el esquema evoluciona SIN romper los datos existentes.

-- Inventario por producto. La columna version es para el bloqueo
-- optimista (@Version): cada UPDATE la incrementa y verifica que nadie
-- más la cambió entre la lectura y la escritura.
CREATE TABLE inventario (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  producto    TEXT NOT NULL UNIQUE,
  disponible  INT NOT NULL CHECK (disponible >= 0),
  version     BIGINT NOT NULL DEFAULT 0
);

INSERT INTO inventario (producto, disponible) VALUES
  ('Teclado mecánico', 10),
  ('Monitor 27"',       5),
  ('Silla ergonómica',  2),
  ('Laptop 14"',        3);

-- El pedido ahora sabe QUÉ producto lleva, CUÁNTOS y en qué ESTADO va.
-- Se agregan las columnas con valores por defecto para que las filas
-- existentes sigan siendo válidas, y luego se endurece la regla.
ALTER TABLE pedido ADD COLUMN estado TEXT NOT NULL DEFAULT 'CREADO'
  CHECK (estado IN ('CREADO', 'CONFIRMADO'));
ALTER TABLE pedido ADD COLUMN cantidad INT NOT NULL DEFAULT 1
  CHECK (cantidad > 0);
ALTER TABLE pedido ADD COLUMN producto_id BIGINT REFERENCES inventario(id);

-- Asignar producto y cantidad a los 10 pedidos semilla.
-- El 7 pide 3 sillas y solo hay 2: es el pedido "sin inventario".
UPDATE pedido SET producto_id = CASE id
    WHEN 2 THEN 2  WHEN 8 THEN 2            -- Monitor
    WHEN 7 THEN 3                           -- Silla
    WHEN 5 THEN 4  WHEN 9 THEN 4            -- Laptop
    ELSE 1                                  -- Teclado
  END,
  cantidad = CASE id WHEN 3 THEN 2 WHEN 7 THEN 3 ELSE 1 END;

ALTER TABLE pedido ALTER COLUMN producto_id SET NOT NULL;

-- Una factura por pedido (UNIQUE) y nunca por monto cero o negativo.
CREATE TABLE factura (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id   BIGINT NOT NULL UNIQUE REFERENCES pedido(id),
  monto       NUMERIC(12,2) NOT NULL CHECK (monto > 0),
  emitida_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Bitácora del proceso: paso 3 de confirmar pedido. Vive DENTRO de la
-- transacción: si el proceso falla, esta fila también se deshace.
CREATE TABLE bitacora (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id      BIGINT NOT NULL REFERENCES pedido(id),
  evento         TEXT NOT NULL,
  registrado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Auditoría de intentos: se escribe con REQUIRES_NEW, en su PROPIA
-- transacción, así que sobrevive aunque confirmar pedido haga rollback.
-- Sin FK a propósito: debe poder registrar hasta un pedido inexistente.
CREATE TABLE intento_confirmacion (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pedido_id      BIGINT NOT NULL,
  resultado      TEXT NOT NULL,
  registrado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
