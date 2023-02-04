CREATE TABLE `marks` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT 'название марки',
  `cyrillic_name` varchar(255) NOT NULL,
  `alias` varchar(255) NOT NULL DEFAULT '' COMMENT 'псевдоним марки для урла',
  `description` mediumtext NOT NULL COMMENT 'описание',
  `is_popular` tinyint(4) NOT NULL COMMENT 'Марка пользуется популярностью',
  `is_completed` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT 'Марка обработана',
  `tecdoc_id` int(11) NOT NULL,
  `country_id` int(11) NOT NULL DEFAULT '0',
  `ya_code` varchar(255) DEFAULT NULL,
  `autoru_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `IX_alias` (`alias`),
  UNIQUE KEY `ya_code_idx` (`ya_code`),
  KEY `IX_name` (`name`),
  KEY `country_id` (`country_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Марки';

INSERT INTO `marks`(`id`, `name`, `cyrillic_name`, `alias`, `description`, `is_popular`, `is_completed`,
`tecdoc_id`, `country_id`, `ya_code`, `autoru_id`)
VALUES(1, 'test1', 'test1', 'test1', 'test1', 1, 1, 123, 123, 'TEST1', 123),
(2, 'test2', 'test2', 'test2', 'test2', 1, 1, 123, 123, 'TEST2', 123),
(3, 'test3', 'test3', 'test3', 'test3', 1, 1, 123, 123, 'TEST3', 123);