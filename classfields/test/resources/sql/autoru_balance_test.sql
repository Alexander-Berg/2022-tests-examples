CREATE TABLE `clients`
(
    `id`                  int(11) NOT NULL AUTO_INCREMENT,
    `client_id`           int(11)          DEFAULT NULL COMMENT 'ID клиента в ЯБалансе',
    `client_type_id`      int(11)          DEFAULT NULL,
    `name`                varchar(255)     DEFAULT NULL,
    `email`               varchar(255)     DEFAULT NULL,
    `phone`               varchar(45)      DEFAULT NULL,
    `fax`                 varchar(45)      DEFAULT NULL,
    `url`                 varchar(45)      DEFAULT NULL,
    `city`                varchar(45)      DEFAULT NULL,
    `is_agency`           tinyint(1)       DEFAULT NULL,
    `agency_id`           int(11)          DEFAULT '0',
    `region_id`           int(11)          DEFAULT NULL,
    `service_id`          int(11)          DEFAULT NULL,
    `currency_id`         tinyint(4)       DEFAULT NULL,
    `contract_id`         int(11) unsigned DEFAULT NULL,
    `migrate_to_currency` datetime         DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`id`),
    KEY `region_id` (`region_id`),
    KEY `agency_id` (`is_agency`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 43864
  DEFAULT CHARSET = utf8;

INSERT INTO `clients`(`id`, `agency_id`, `client_id`, `is_agency`)
VALUES (1, 101, 1001, 1),
       (2, 102, 1002, 0),
       (3, 103, 1003, 1),
       (4, 104, 1004, 0),
       (5, 2000, 1005, 0),
       (6, 0, 2000, 1),
       (7, 0, 2001, 1),
       (9, 0, 2001, 1);

CREATE TABLE `links_map`
(
    `id`        int(11)      NOT NULL AUTO_INCREMENT,
    `link_name` varchar(255) NOT NULL,
    `key1`      varchar(255) NOT NULL,
    `key2`      varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `key1` (`key1`(16)),
    KEY `link_name` (`link_name`(16)),
    KEY `idx_links_map_name_key1` (`link_name`, `key1`),
    KEY `idx_links_map_name_key12` (`link_name`, `key1`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 75058
  DEFAULT CHARSET = utf8;

INSERT INTO `links_map`(`link_name`, `key1`, `key2`)
VALUES ('office7.clients.id-balance.clients.id', 10, 1),
       ('office7.clients.id-balance.clients.id', 20, 2),
       ('office7.clients.id-balance.clients.id', 100, 4),
       ('office7.clients.id-balance.clients.id', 105, 5),
       ('office7.clients.id-balance.clients.id', 200, 6),
       ('office7.clients.id-balance.clients.id', 201, 7),
       ('office7.clients.id-balance.clients.id', 999, 999);
