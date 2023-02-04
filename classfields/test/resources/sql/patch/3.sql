CREATE TABLE `check_report` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT,
  `initial_key`    CHAR(32) NOT NULL,
  `check_key`      CHAR(32) NOT NULL,
  `validation_key` CHAR(32),
  PRIMARY KEY (`id`),
  UNIQUE KEY (`check_key`)
)
  ENGINE = InnoDB;