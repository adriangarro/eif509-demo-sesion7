-- Dos tablas con relación, tipos correctos y una regla
CREATE TABLE cliente (
  id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre TEXT NOT NULL,
  email  TEXT NOT NULL UNIQUE
);

CREATE TABLE pedido (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cliente_id BIGINT NOT NULL REFERENCES cliente(id),
  total      NUMERIC(12,2) NOT NULL CHECK (total >= 0),
  creado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índice en la FK que vamos a consultar
CREATE INDEX idx_pedido_cliente ON pedido(cliente_id);
