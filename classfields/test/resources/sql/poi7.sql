CREATE TABLE `types`
(
    `id`    int(11) unsigned NOT NULL AUTO_INCREMENT,
    `name`  varchar(10)      NOT NULL,
    `title` varchar(255)     NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8;

CREATE TABLE `poi`
(
    `id`               int(11) unsigned                 NOT NULL AUTO_INCREMENT,
    `hash`             varchar(6)                       NOT NULL,
    `type_id`          int(11) unsigned                 NOT NULL,
    `country_id`       int(11) unsigned                          DEFAULT NULL,
    `region_id`        int(11) unsigned                          DEFAULT NULL,
    `city_id`          int(11) unsigned                          DEFAULT NULL,
    `ya_country_id`    int(11) unsigned                          DEFAULT NULL,
    `ya_region_id`     int(11) unsigned                          DEFAULT NULL,
    `ya_city_id`       int(11) unsigned                          DEFAULT NULL,
    `address`          varchar(255)                              DEFAULT NULL,
    `lat`              float(11, 6)                              DEFAULT NULL,
    `lng`              float(11, 6)                              DEFAULT NULL,
    `tile_16`          int(11) unsigned                          DEFAULT NULL,
    `rand`             float                            NOT NULL,
    `create_date`      datetime                                  DEFAULT NULL,
    `change_date`      datetime                         NOT NULL,
    `active`           enum ('New','Good','Bad','Wait') NOT NULL DEFAULT 'New',
    `call_tracking_on` tinyint(1) unsigned              NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `FK_poi_type_id` (`type_id`),
    KEY `lat` (`lat`) USING BTREE,
    KEY `lng` (`lng`),
    KEY `tile16` (`tile_16`),
    KEY `rand` (`rand`),
    KEY `active` (`active`),
    CONSTRAINT `FK_poi_type_id` FOREIGN KEY (`type_id`) REFERENCES `types` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 41388
  DEFAULT CHARSET = utf8;

CREATE TABLE `poi_types`
(
    `type_id`    int(10) unsigned NOT NULL AUTO_INCREMENT,
    `type_name`  varchar(10)      NOT NULL,
    `type_title` varchar(255)     NOT NULL,
    PRIMARY KEY (`type_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8;

CREATE TABLE `poi_prop_names`
(
    `prop_name_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `type_id`      int(10) unsigned NOT NULL,
    `name`         varchar(45)      NOT NULL,
    PRIMARY KEY (`prop_name_id`) USING BTREE,
    KEY `FK_prop_names_type_id` (`type_id`),
    KEY `prop_names_name` (`name`),
    CONSTRAINT `FK_prop_names_type_id` FOREIGN KEY (`type_id`) REFERENCES `poi_types` (`type_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 42
  DEFAULT CHARSET = utf8;

CREATE TABLE `poi_prop_values`
(
    `prop_value_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `poi_id`        int(10) unsigned NOT NULL,
    `prop_name_id`  int(10) unsigned NOT NULL,
    `value`         text             NOT NULL,
    PRIMARY KEY (`prop_value_id`),
    KEY `FK_prop_values_poi_id` (`poi_id`),
    KEY `FK_poi_prop_values_prop_name_id` (`prop_name_id`),
    KEY `poi_prop_values_value` (`value`(5)),
    CONSTRAINT `FK_poi_prop_values_prop_name_id` FOREIGN KEY (`prop_name_id`) REFERENCES `poi_prop_names` (`prop_name_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT `FK_prop_values_poi_id` FOREIGN KEY (`poi_id`) REFERENCES `poi` (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 3505270
  DEFAULT CHARSET = utf8 COMMENT ='Values for POI Properties';

CREATE TABLE `poi_rating`
(
    `poi_id`              int(10) unsigned       NOT NULL,
    `register_score`      double(10, 2) unsigned NOT NULL,
    `properties_score`    double(10, 2) unsigned NOT NULL,
    `penalities_balls`    double(10, 2) unsigned NOT NULL,
    `penalities_events`   double(10, 2) unsigned NOT NULL,
    `phones_score`        double(10, 2) unsigned NOT NULL,
    `sales_count_ratio`   double(10, 2) unsigned NOT NULL,
    `carinfo_good_score`  double(10, 2) unsigned NOT NULL,
    `carinfo_crime_score` double(10, 2) unsigned NOT NULL,
    `rating`              decimal(10, 2)         NOT NULL,
    `relative_rating`     double(10, 2)          NOT NULL,
    `date`                datetime               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`poi_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


insert into types (id, name, title)
VALUES (1, 'salon', 'Салоны');

insert into poi_types(`type_id`, `type_name`, `type_title`)
values (1, 'salon', 'Салоны');

insert into poi (`id`, `hash`, `type_id`, `country_id`, `region_id`, `city_id`, `ya_country_id`, `ya_region_id`,
                 `ya_city_id`, `address`, `lat`, `lng`, `tile_16`, `rand`, `create_date`, `change_date`, `active`,
                 `call_tracking_on`)
values (1, '', 1, 1, 87, 1123, 225, 1, 213, 'ул.Нижние Поля 29Б (обязательно звоните) Метро "Марьино", "Братиславская"',
        55.657055, 37.732887, 1665423673, 0.529954, '2013-05-22 03:23:28', '2013-05-22 03:23:28', 'Good', 1),
        (4, '', 1, 1, 87, 1123, 225, 1, 213, 'address", "address"',
                        55.657055, 37.732887, 1665423673, 0.529954, '2013-05-22 03:23:28', '2013-05-22 03:23:28', 'Good', 1),
        (5, '', 1, 1, 87, 1123, 225, 1, 213, 'address", "address"',
                        55.657055, 37.732887, 1665423673, 0.529954, '2013-05-22 03:23:28', '2013-05-22 03:23:28', 'Good', 1),
        (3, '', 1, 1, 87, 1123, 225, 1, 213, 'address", "address"',
                55.657055, 37.732887, 1665423673, 0.529954, '2013-05-22 03:23:28', '2013-05-22 03:23:28', 'Good', 1);

insert into poi_prop_names(`prop_name_id`, `type_id`, `name`)
values (22, 1, 'allow_photo_reorder'),
       (32, 1, 'auto_activate_cars_offers'),
       (35, 1, 'auto_activate_commercial_offers'),
       (37, 1, 'auto_activate_moto_offers'),
       (21, 1, 'call_tracking'),
       (40, 1, 'call_tracking_by_offer'),
       (34, 1, 'chat_enabled'),
       (2, 1, 'description'),
       (38, 1, 'hide_license_plate'),
       (9, 1, 'hide_phones'),
       (11, 1, 'hide_vin_numbers'),
       (15, 1, 'lessor'),
       (6, 1, 'old_contacts_id'),
       (7, 1, 'origin'),
       (42, 1, 'overdraft_balance_person_id'),
       (41, 1, 'overdraft_enabled'),
       (19, 1, 'override_rating'),
       (43, 1, 'poi_multiposting'),
       (17, 1, 'sale_edit_address'),
       (10, 1, 'sale_edit_contact'),
       (8, 1, 'set_date'),
       (1, 1, 'title'),
       (3, 1, 'url'),
       (30, 1, 'verba'),
       (13, 1, 'vin_required'),
       (4, 1, 'workdays'),
       (5, 1, 'worktime');

insert into poi_prop_values(`prop_value_id`, `poi_id`, `prop_name_id`, `value`)
values (1, 1, 1, 'test_title'),
       (2, 1, 3, 'http://test-url.ru'),
       (3, 1, 40, 'false'),
       (4, 1, 40, 'true'),
       (5, 4, 3, 'http://best-test.ru' ),
       (6, 5, 3, 'http://i-hate-it.ru');

CREATE TABLE `poi_phones` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poi_id` int(11) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `phone` bigint(20) NOT NULL,
  `phone_mask` varchar(10) NOT NULL,
  `marks_ids` varchar(255) NOT NULL,
  `marks_names` varchar(255) NOT NULL,
  `call_from` tinyint(2) unsigned NOT NULL DEFAULT '10',
  `call_till` tinyint(2) unsigned NOT NULL DEFAULT '19',
  PRIMARY KEY (`id`),
  KEY `phone` (`phone`),
  KEY `poi_id` (`poi_id`)
) ENGINE=InnoDB AUTO_INCREMENT=461027 DEFAULT CHARSET=utf8 COMMENT='Phones';

insert into poi_phones(`id`, `poi_id`, `title`, `phone`, `phone_mask`, `marks_ids`,`marks_names`,`call_from`, `call_till`)
values (461028, 1, 'Дилер', 79267178974, '1:3:7', '','', 9, 23),
       (461029, 1, 'Дилер', 74993947451, '1:3:7', '','', 9, 23);


