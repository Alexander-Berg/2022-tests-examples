-- table tasks
CREATE TABLE tasks (
  id BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  client_id INT UNSIGNED NOT NULL,
  feedloader_task_id BIGINT UNSIGNED NULL COMMENT 'Идентификатор фида в Feedloader-е',
  feedloader_task_result_id BIGINT UNSIGNED NULL COMMENT 'Идентификатор конкретной загрузки в Feedloader-е',
  xml_host_id INT UNSIGNED COMMENT 'Deprecated: идентификатор фида в bdupload',
  status ENUM('new', 'processing', 'success', 'failure') NOT NULL,
  url VARCHAR(255) NOT NULL COMMENT 'Результат загрузки Feedloader-ом',
  client_url VARCHAR(255) NOT NULL COMMENT 'Оригинальный адрес клиентского фида',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMP NULL,
  service_info_json VARCHAR(512) NOT NULL DEFAULT '{}' COMMENT 'Сервис-специфичные данные в json',
  error_message VARCHAR(255) COMMENT 'Причина неудачной обработки всего фида (для статуса failed)',
  category ENUM('CARS','TRUCK','LCV','TRAILER','SWAP_BODY','BUS','ARTIC','AGRICULTURAL',
    'CONSTRUCTION','AUTOLOADER','CRANE','DREDGE','BULLDOZERS','CRANE_HYDRAULICS','MUNICIPAL') DEFAULT NULL,
  section ENUM('USED','NEW') DEFAULT NULL,
  PRIMARY KEY (id),
  INDEX idx_created_at (created_at),
  INDEX idx_status_client_id (status, client_id),
  INDEX idx_category_section_client_id (category, section, client_id),
  UNIQUE INDEX idx_feedloader_task_id (feedloader_task_id, feedloader_task_result_id)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8
  DEFAULT COLLATE = utf8_general_ci;

-- Tables for debug and statistics:
CREATE TABLE offers (
  id BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  position INT UNSIGNED NOT NULL,
  feed_id VARBINARY(32) NOT NULL,
  task_id BIGINT(2) UNSIGNED NOT NULL,
  client_id INT UNSIGNED NOT NULL,

  offer_id VARCHAR(32),
  feedprocessor_id VARCHAR(32),
  status ENUM('unknown_status', 'insert', 'update', 'delete', 'skip', 'error') NOT NULL,
  error_message VARCHAR(255), -- текст ошибки, в случае ошибки над всем батчем офферов
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  vin varchar(255),
  unique_id varchar(255),

  PRIMARY KEY (id),
  INDEX offers_offer_id_idx (offer_id),
  INDEX offers_client_id_idx (client_id),
  INDEX offers_feed_id_idx (feed_id),
  INDEX offers_task_id_idx (task_id)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8
  DEFAULT COLLATE = utf8_general_ci;

CREATE TABLE offer_errors (
  id BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  offer_id BIGINT(20) UNSIGNED NOT NULL, -- Ссылка на AUTOINCREMENT INT, не на "offer_id" из VOS!
  `type` ENUM('error', 'notice') NOT NULL,
  message VARCHAR(255) NOT NULL,
  context VARCHAR(64),
  column_name VARCHAR(64),
  original_value VARCHAR(255),

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX offer_errors_offer_id_idx (offer_id)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8
  DEFAULT COLLATE = utf8_general_ci;

CREATE TABLE key_value (
  `key` VARCHAR(255) NOT NULL,
  value VARCHAR(255) NOT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8
  DEFAULT COLLATE = utf8_general_ci;
