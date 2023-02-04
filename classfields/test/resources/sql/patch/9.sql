CREATE TABLE IF NOT EXISTS `queue_settings` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `queue` VARCHAR(64) NOT NULL,
  `settings` BLOB NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`queue`)
) ENGINE=InnoDB;

ALTER TABLE `tasks`
MODIFY `exclude_users` json NOT NULL;
