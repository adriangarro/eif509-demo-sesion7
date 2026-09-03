-- Datos suficientes para que el N+1 se note al contar consultas:
-- un tercer cliente y más pedidos repartidos entre los tres.
-- (V1 y V2 son las mismas migraciones de la Sesión 3.)

INSERT INTO cliente (nombre, email) VALUES
  ('Carmen Solís', 'carmen@ejemplo.cr');

INSERT INTO pedido (cliente_id, total) VALUES
  (2, 12000.00),
  (2,  4500.00),
  (3, 99000.00),
  (1,  2500.75),
  (3, 18300.00),
  (2,  7250.00),
  (1, 66000.00),
  (3,  3100.25);
