# sales-db -> all7
CREATE DATABASE IF NOT EXISTS all7;
USE all7;

CREATE TABLE IF NOT EXISTS sales(
  id BIGINT (20),
  user_id VARCHAR(20),
  new_client_id VARCHAR(20),
  create_date DATETIME);

TRUNCATE sales;

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (1, '1', NULL, DATE_SUB(NOW(), INTERVAL 1455 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (2, '2', NULL, DATE_SUB(NOW(), INTERVAL 1441 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (3, '3', NULL, DATE_SUB(NOW(), INTERVAL 1485 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (4, '4', 'client001', DATE_SUB(NOW(), INTERVAL 1441 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (5, '5', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (6, '8', NULL, DATE_SUB(NOW(), INTERVAL 1455 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (7, '9', NULL, DATE_SUB(NOW(), INTERVAL 1455 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (8, '17', NULL, DATE_SUB(NOW(), INTERVAL 1455 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (9, '11', NULL, DATE_SUB(NOW(), INTERVAL 1455 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (10, '14', NULL, DATE_SUB(NOW(), INTERVAL 1455 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (11, '15', NULL, DATE_SUB(NOW(), INTERVAL 1455 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (12, '30186344', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (13, '21', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (14, '22', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (15, '23', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (16, '24', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (17, '30', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (18, '40', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (19, '42', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));

INSERT INTO sales(id, user_id, new_client_id, create_date)
VALUES (20, '50', NULL, DATE_SUB(NOW(), INTERVAL 1488 MINUTE));
