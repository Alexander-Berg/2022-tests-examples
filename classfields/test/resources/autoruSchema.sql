create database if not exists `all7`;
--
drop table if exists `all7`.`sales`;
--
CREATE TABLE `all7`.`sales` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hash` varchar(8) NOT NULL,
  `create_date` datetime NOT NULL,
  `set_date` datetime NOT NULL,
  `expire_date` datetime NOT NULL,
  `user_id` int(10) unsigned DEFAULT NULL,
  `poi_id` int(10) unsigned DEFAULT NULL,
  `country_id` int(10) unsigned NOT NULL,
  `region_id` int(10) unsigned NOT NULL,
  `city_id` int(10) unsigned NOT NULL,
  `ya_country_id` int(10) unsigned NOT NULL,
  `ya_region_id` int(10) unsigned NOT NULL,
  `ya_city_id` int(10) unsigned NOT NULL,
  `client_id` int(10) unsigned DEFAULT NULL,
  `contact_id` int(10) unsigned DEFAULT NULL,
  `file_id` int(10) unsigned DEFAULT NULL,
  `section_id` int(10) unsigned NOT NULL,
  `category_id` int(10) unsigned NOT NULL,
  `mark_id` int(10) unsigned DEFAULT NULL,
  `folder_id` int(10) unsigned DEFAULT NULL,
  `modification_id` int(10) unsigned DEFAULT NULL,
  `year` smallint(4) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `price_RUR` decimal(12,2) NOT NULL DEFAULT '0.00',
  `currency` enum('RUR','USD','EUR','CNY') NOT NULL DEFAULT 'RUR',
  `description` text,
  `status` tinyint(1) NOT NULL DEFAULT '0',
  `photo` tinyint(1) NOT NULL DEFAULT '0',
  `dealer` tinyint(1) NOT NULL DEFAULT '0',
  `lib2_table` enum('sale1','sale2','sale3','sale4','sale5') DEFAULT NULL,
  `lib2_category` varchar(255) DEFAULT NULL,
  `lib2_section` varchar(255) DEFAULT NULL,
  `lib2_id` int(10) DEFAULT NULL,
  `lib2_hash` varchar(6) DEFAULT NULL,
  `new_client_id` int(11) DEFAULT NULL,
  `salon_id` int(11) DEFAULT NULL,
  `salon_contact_id` int(11) NOT NULL DEFAULT '0',
  `fresh_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IX_status` (`status`),
  KEY `IX_region_id` (`region_id`),
  KEY `IX_city_id` (`city_id`),
  KEY `FK_sales_poi_id` (`poi_id`),
  KEY `IX_country_id` (`country_id`),
  KEY `set_date` (`set_date`),
  KEY `section_id` (`section_id`),
  KEY `client_id` (`client_id`),
  KEY `user_id` (`user_id`),
  KEY `new_client_id_status` (`new_client_id`,`status`),
  KEY `IX_ya_country_id` (`ya_country_id`),
  KEY `IX_ya_region_id` (`ya_region_id`),
  KEY `IX_ya_city_id` (`ya_city_id`),
  KEY `create_date` (`create_date`),
  KEY `expire_date` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_ids`;
--
CREATE TABLE `all7`.`sales_ids` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `IX_sales_id_created` (`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
ALTER TABLE `all7`.`sales_ids` AUTO_INCREMENT=1113778219;
--
drop table if exists `all7`.`sales_images`;
--
CREATE TABLE `all7`.`sales_images` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `sorder` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL,
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `exif_lat` float(10,6) DEFAULT NULL,
  `exif_lng` float(10,6) DEFAULT NULL,
  `exif_date` datetime NOT NULL,
  `source` enum('salecard','oldsale','avatar') NOT NULL DEFAULT 'salecard',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cv_hash` varbinary(32) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_main` (`sale_id`,`main`),
  KEY `IX_sale_id_name` (`sale_id`,`name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_property_values`;
--
CREATE TABLE `all7`.`sales_property_values` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `setting_id` int(10) unsigned NOT NULL,
  `property_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sales_property_values_settings_id` (`setting_id`),
  KEY `IX_sale_id_setting_id` (`sale_id`,`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_change_log`;
--
CREATE TABLE `all7`.`sales_change_log` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` int(10) unsigned NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_images_10372`;
--
CREATE TABLE `all7`.`sales_images_10372` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `sorder` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL,
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `exif_lat` float(10,6) DEFAULT NULL,
  `exif_lng` float(10,6) DEFAULT NULL,
  `exif_date` datetime NOT NULL,
  `source` enum('salecard','oldsale','avatar') NOT NULL DEFAULT 'salecard',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cv_hash` varbinary(32) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_main` (`sale_id`,`main`),
  KEY `IX_sale_id_name` (`sale_id`,`name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_images_10431`;
--
CREATE TABLE `all7`.`sales_images_10431` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `sorder` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL,
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `exif_lat` float(10,6) DEFAULT NULL,
  `exif_lng` float(10,6) DEFAULT NULL,
  `exif_date` datetime NOT NULL,
  `source` enum('salecard','oldsale','avatar') NOT NULL DEFAULT 'salecard',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cv_hash` varbinary(32) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_main` (`sale_id`,`main`),
  KEY `IX_sale_id_name` (`sale_id`,`name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_images_10425`;
--
CREATE TABLE `all7`.`sales_images_10425` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `sorder` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL,
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `exif_lat` float(10,6) DEFAULT NULL,
  `exif_lng` float(10,6) DEFAULT NULL,
  `exif_date` datetime NOT NULL,
  `source` enum('salecard','oldsale','avatar') NOT NULL DEFAULT 'salecard',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cv_hash` varbinary(32) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_main` (`sale_id`,`main`),
  KEY `IX_sale_id_name` (`sale_id`,`name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_images_10432`;
--
CREATE TABLE `all7`.`sales_images_10432` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `sorder` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL,
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `exif_lat` float(10,6) DEFAULT NULL,
  `exif_lng` float(10,6) DEFAULT NULL,
  `exif_date` datetime NOT NULL,
  `source` enum('salecard','oldsale','avatar') NOT NULL DEFAULT 'salecard',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cv_hash` varbinary(32) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_main` (`sale_id`,`main`),
  KEY `IX_sale_id_name` (`sale_id`,`name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_images_10433`;
--
CREATE TABLE `all7`.`sales_images_10433` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `sorder` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL,
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `exif_lat` float(10,6) DEFAULT NULL,
  `exif_lng` float(10,6) DEFAULT NULL,
  `exif_date` datetime NOT NULL,
  `source` enum('salecard','oldsale','avatar') NOT NULL DEFAULT 'salecard',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cv_hash` varbinary(32) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_main` (`sale_id`,`main`),
  KEY `IX_sale_id_name` (`sale_id`,`name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_images_10442`;
--
CREATE TABLE `all7`.`sales_images_10442` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `sorder` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL,
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `exif_lat` float(10,6) DEFAULT NULL,
  `exif_lng` float(10,6) DEFAULT NULL,
  `exif_date` datetime NOT NULL,
  `source` enum('salecard','oldsale','avatar') NOT NULL DEFAULT 'salecard',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cv_hash` varbinary(32) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_main` (`sale_id`,`main`),
  KEY `IX_sale_id_name` (`sale_id`,`name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_images_10443`;
--
CREATE TABLE `all7`.`sales_images_10443` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `sorder` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL,
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `exif_lat` float(10,6) DEFAULT NULL,
  `exif_lng` float(10,6) DEFAULT NULL,
  `exif_date` datetime NOT NULL,
  `source` enum('salecard','oldsale','avatar') NOT NULL DEFAULT 'salecard',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cv_hash` varbinary(32) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_main` (`sale_id`,`main`),
  KEY `IX_sale_id_name` (`sale_id`,`name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_property_values_10372`;
--
CREATE TABLE `all7`.`sales_property_values_10372` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `setting_id` int(10) unsigned NOT NULL,
  `property_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sales_property_values_settings_id` (`setting_id`),
  KEY `IX_sale_id_setting_id` (`sale_id`,`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_property_values_10431`;
--
CREATE TABLE `all7`.`sales_property_values_10431` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `setting_id` int(10) unsigned NOT NULL,
  `property_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sales_property_values_settings_id` (`setting_id`),
  KEY `IX_sale_id_setting_id` (`sale_id`,`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_property_values_10425`;
--
CREATE TABLE `all7`.`sales_property_values_10425` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `setting_id` int(10) unsigned NOT NULL,
  `property_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sales_property_values_settings_id` (`setting_id`),
  KEY `IX_sale_id_setting_id` (`sale_id`,`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_property_values_10432`;
--
CREATE TABLE `all7`.`sales_property_values_10432` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `setting_id` int(10) unsigned NOT NULL,
  `property_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sales_property_values_settings_id` (`setting_id`),
  KEY `IX_sale_id_setting_id` (`sale_id`,`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_property_values_10433`;
--
CREATE TABLE `all7`.`sales_property_values_10433` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `setting_id` int(10) unsigned NOT NULL,
  `property_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sales_property_values_settings_id` (`setting_id`),
  KEY `IX_sale_id_setting_id` (`sale_id`,`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_property_values_10442`;
--
CREATE TABLE `all7`.`sales_property_values_10442` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `setting_id` int(10) unsigned NOT NULL,
  `property_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sales_property_values_settings_id` (`setting_id`),
  KEY `IX_sale_id_setting_id` (`sale_id`,`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_property_values_10443`;
--
CREATE TABLE `all7`.`sales_property_values_10443` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `setting_id` int(10) unsigned NOT NULL,
  `property_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sales_property_values_settings_id` (`setting_id`),
  KEY `IX_sale_id_setting_id` (`sale_id`,`setting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`settings`;
--
CREATE TABLE `all7`.`settings` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `alias` varchar(45) NOT NULL,
  `method` varchar(45) DEFAULT NULL,
  `field_type` enum('int','timestamp','str2ordinal','bool','float','string','multi','bigint') NOT NULL,
  `search_type` enum('no','attr','field') NOT NULL,
  `search_field_type` enum('range','exact','custom') NOT NULL DEFAULT 'exact',
  `is_property` tinyint(1) NOT NULL,
  `sphinx_aggregate_field` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `alias` (`alias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_options`;
--
CREATE TABLE `all7`.`sales_options` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `alias` varchar(100) NOT NULL DEFAULT '',
  `value` varchar(50) NOT NULL DEFAULT '',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `IX_sale_id_alias` (`sale_id`,`alias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_options_modern`;
--
CREATE TABLE `all7`.`sales_options_modern` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` int(11) unsigned NOT NULL,
  `option_id` bigint(11) unsigned NOT NULL,
  `value` tinyint(3) NOT NULL,
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sale_to_options_unq` (`sale_id`,`option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_options_dictionary`;
--
CREATE TABLE `all7`.`sales_options_dictionary` (
  `id` bigint(11) unsigned NOT NULL,
  `code` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL DEFAULT '',
  `group` enum('comfort','exterior','interior','multimedia','protection','safety','visibility','other') NOT NULL DEFAULT 'other',
  `extra_id` int(10) unsigned NOT NULL,
  `extra_value` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unq_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`emails_sales`;
--
CREATE TABLE `all7`.`emails_sales` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(70) NOT NULL,
  `sale_id` bigint(20) unsigned NOT NULL,
  `user_id` int(11) unsigned NOT NULL,
  `hash` varchar(32) NOT NULL,
  `confirmed` tinyint(1) NOT NULL DEFAULT '0',
  `create_date` datetime NOT NULL,
  `update_date` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sale_id` (`sale_id`),
  KEY `user_id` (`user_id`),
  KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`tasks`;
--
CREATE TABLE `all7`.`tasks` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `task` varchar(255) NOT NULL DEFAULT '',
  `params` text NOT NULL,
  `status` enum('new','in_progress','done','fail') NOT NULL DEFAULT 'new',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `run_date` datetime NOT NULL,
  `partition` tinyint(3) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `status_idx` (`status`),
  KEY `run_date_idx` (`run_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`video_sales_yandex`;
--
CREATE TABLE `all7`.`video_sales_yandex` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `status` tinyint(2) unsigned NOT NULL DEFAULT '0',
  `type` varchar(255) DEFAULT NULL,
  `film_n` bigint(20) unsigned NOT NULL,
  `video_identificator` varchar(255) DEFAULT NULL,
  `video_url` text,
  `video_thumbs` text,
  `target_url` varchar(255) DEFAULT '',
  `progress_url` varchar(255) DEFAULT '',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `film_n_idx` (`film_n`),
  KEY `video_identificator` (`video_identificator`(16))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`video_sales`;
--
CREATE TABLE `all7`.`video_sales` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `provider_alias` varchar(100) NOT NULL,
  `value` varchar(255) NOT NULL,
  `parse_value` varchar(255) NOT NULL,
  `video_id` bigint(20) unsigned NOT NULL,
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `sale_id` (`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`poi`;
--
CREATE TABLE `all7`.`poi` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `country_id` int(10) unsigned DEFAULT NULL,
  `region_id` int(10) unsigned DEFAULT NULL,
  `city_id` int(10) unsigned DEFAULT NULL,
  `ya_country_id` int(10) unsigned DEFAULT NULL,
  `ya_region_id` int(10) unsigned DEFAULT NULL,
  `ya_city_id` int(10) unsigned DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `lat` float(10,6) DEFAULT NULL,
  `lng` float(10,6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IX_country_id` (`country_id`),
  KEY `IX_region_id` (`region_id`),
  KEY `IX_city_id` (`city_id`),
  KEY `IX_lat` (`lat`),
  KEY `IX_lng` (`lng`),
  KEY `IX_ya_country_id` (`ya_country_id`),
  KEY `IX_ya_region_id` (`ya_region_id`),
  KEY `IX_ya_city_id` (`ya_city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_phones`;
--
CREATE TABLE `all7`.`sales_phones` (
  `sale_id` bigint(20) unsigned NOT NULL,
  `phone_id` int(10) unsigned NOT NULL,
  `call_from` tinyint(2) unsigned NOT NULL,
  `call_till` tinyint(2) unsigned NOT NULL,
  `contact_name` varchar(255) NOT NULL,
  PRIMARY KEY (`sale_id`,`phone_id`),
  KEY `FK_sales_phones_sale_id` (`sale_id`),
  KEY `phone_idx` (`phone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`carinfo_sales`;
--
CREATE TABLE `all7`.`carinfo_sales` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `status_request` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `status_response` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `vin` varchar(255) DEFAULT NULL,
  `sts` varchar(255) DEFAULT NULL,
  `gosnomer` varchar(255) DEFAULT NULL,
  `crash` int(11) unsigned DEFAULT NULL,
  `displacement` int(11) DEFAULT NULL,
  `fullreporturl` varchar(255) DEFAULT NULL,
  `horsepower` int(11) unsigned DEFAULT NULL,
  `mark` varchar(255) DEFAULT NULL,
  `pledge` tinyint(1) unsigned DEFAULT NULL,
  `stealing` tinyint(1) unsigned DEFAULT NULL,
  `restrictions` tinyint(4) DEFAULT NULL,
  `year` int(11) unsigned DEFAULT NULL,
  `color` varchar(50) DEFAULT NULL,
  `create_date` datetime NOT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sale_id` (`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`badges_sales`;
--
CREATE TABLE `all7`.`badges_sales` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category_id` int(10) unsigned NOT NULL DEFAULT '15',
  `sale_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `badge` varchar(100) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `service_id` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `sale_id` (`sale_id`),
  KEY `status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_services`;
--
CREATE TABLE `all7`.`sales_services` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` int(10) unsigned NOT NULL,
  `service` varchar(45) NOT NULL,
  `expire_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_activated` int(1) unsigned NOT NULL DEFAULT '0',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `extra_data` text NOT NULL,
  `activate_date` timestamp NULL DEFAULT NULL,
  `price` blob,
  `price_deadline` timestamp NULL DEFAULT NULL,
  `offer_billing` blob,
  `offer_billing_deadline` timestamp NULL DEFAULT NULL,
  `hold_id` varchar(512) DEFAULT NULL,
  `epoch` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `is_activated` (`is_activated`),
  KEY `IX_sale_id_is_activated` (`sale_id`,`is_activated`),
  KEY `idx_price_deadline` (`price_deadline`),
  KEY `idx_offer_billing_deadline` (`offer_billing_deadline`),
  KEY `idx_epoch` (`epoch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_services_categories`;
--
CREATE TABLE `all7`.`sales_services_categories` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` int(11) NOT NULL,
  `section_id` int(10) unsigned NOT NULL,
  `sale_id` int(10) unsigned NOT NULL,
  `service` varchar(45) NOT NULL,
  `expire_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_activated` int(1) unsigned NOT NULL DEFAULT '0',
  `create_date` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `extra_data` text NOT NULL COMMENT 'json',
  `sale_hash` varchar(16) DEFAULT NULL,
  `client_id` int(11) DEFAULT NULL,
  `activate_date` timestamp NULL DEFAULT NULL,
  `price` blob,
  `price_deadline` timestamp NULL DEFAULT NULL,
  `offer_billing` blob,
  `offer_billing_deadline` timestamp NULL DEFAULT NULL,
  `hold_id` varchar(512) DEFAULT NULL,
  `epoch` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `is_activated` (`is_activated`),
  KEY `IX_sale_id_is_activated` (`sale_id`,`is_activated`),
  KEY `idx_sale_id_category_id` (`sale_id`,`category_id`),
  KEY `idx_price_deadline` (`price_deadline`),
  KEY `idx_offer_billing_deadline` (`offer_billing_deadline`),
  KEY `idx_epoch` (`epoch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_phones_redirect`;
--
CREATE TABLE `all7`.`sales_phones_redirect` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `status` tinyint(3) unsigned NOT NULL,
  `last_status` tinyint(3) DEFAULT NULL,
  `phones` text,
  `update_date` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sale_id` (`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_services_categories`;
--
CREATE TABLE `all7`.`sales_services_categories` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` int(11) NOT NULL,
  `section_id` int(10) unsigned NOT NULL,
  `sale_id` int(10) unsigned NOT NULL,
  `service` varchar(45) NOT NULL,
  `expire_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_activated` int(1) unsigned NOT NULL DEFAULT '0',
  `create_date` timestamp NOT NULL DEFAULT '1980-01-01 00:00:01',
  `extra_data` text NOT NULL,
  `sale_hash` varchar(16) DEFAULT NULL,
  `client_id` int(11) DEFAULT NULL,
  `activate_date` timestamp NULL DEFAULT NULL,
  `price` blob,
  `price_deadline` timestamp NULL DEFAULT NULL,
  `offer_billing` blob,
  `offer_billing_deadline` timestamp NULL DEFAULT NULL,
  `hold_id` varchar(512) DEFAULT NULL,
  `epoch` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `is_activated` (`is_activated`),
  KEY `IX_sale_id_is_activated` (`sale_id`,`is_activated`),
  KEY `idx_sale_id_category_id` (`sale_id`,`category_id`),
  KEY `idx_price_deadline` (`price_deadline`),
  KEY `idx_offer_billing_deadline` (`offer_billing_deadline`),
  KEY `idx_epoch` (`epoch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_discount`;
--
CREATE TABLE `all7`.`sales_discount` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` bigint(20) unsigned NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  `client_id` int(10) unsigned DEFAULT NULL,
  `price` decimal(12,2) NOT NULL,
  `status` enum('active','inactive') DEFAULT NULL,
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sale_id` (`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`ips`;
--
CREATE TABLE `all7`.`ips` (
  `sale_id` int(10) unsigned NOT NULL,
  `ip` int(10) unsigned NOT NULL,
  `autoru_sid` varchar(128) DEFAULT NULL,
  `autoruuid` varchar(128) DEFAULT NULL,
  `suid` varchar(128) DEFAULT NULL,
  `yandexuid` varchar(128) DEFAULT NULL,
  `fuid01` varchar(255) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `api_uuid` varchar(128) DEFAULT NULL,
  `ip_plain` varchar(39) DEFAULT NULL,
  PRIMARY KEY (`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all7`.`sales_from_api`;
--
CREATE TABLE `all7`.`sales_from_api` (
  `sale_id` bigint(20) NOT NULL,
  `platform` varchar(40) NOT NULL DEFAULT 'other',
  `placement` json DEFAULT NULL,
  `source` varchar(40) NOT NULL DEFAULT '',
  `user_id` int(10) NOT NULL DEFAULT '0',
  `client_id` int(10) NOT NULL DEFAULT '0',
  `user_role` varchar(40) NOT NULL DEFAULT '',
  `parsing_url` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`sale_id`),
  KEY `ix_sale_id` (`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
create database if not exists `office7`;
--
drop table if exists `all7`.`sales_vin`;
--
CREATE TABLE `all7`.`sales_vin` (
  `sale_id` bigint(20) unsigned NOT NULL,
  `vin` varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (`sale_id`),
  KEY `vin_idx` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `office7`.`clients`;
--
CREATE TABLE `office7`.`clients` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` int(10) unsigned NOT NULL DEFAULT '1',
  `product_id` smallint(5) unsigned NOT NULL,
  `country_id` int(10) unsigned NOT NULL,
  `region_id` int(10) unsigned NOT NULL,
  `city_id` int(10) unsigned NOT NULL,
  `ya_country_id` int(10) unsigned NOT NULL,
  `ya_region_id` int(10) unsigned NOT NULL,
  `ya_city_id` int(10) unsigned NOT NULL,
  `url` varchar(255) NOT NULL,
  `email` varchar(70) NOT NULL,
  `phone` bigint(20) unsigned NOT NULL,
  `phone_mask` varchar(10) NOT NULL,
  `description` text NOT NULL,
  `logo` varchar(255) NOT NULL,
  `logosize` varchar(32) NOT NULL,
  `comment` mediumtext NOT NULL,
  `is_agent` tinyint(1) unsigned NOT NULL,
  `agent_id` int(10) unsigned NOT NULL,
  `loader_id` int(10) unsigned NOT NULL,
  `origin` varchar(10) NOT NULL,
  `status` enum('new','active','inactive','deleted','freezed','waiting','stopped') DEFAULT 'inactive',
  `contact_name` varchar(255) NOT NULL,
  `fax` bigint(20) unsigned NOT NULL,
  `fax_mask` varchar(10) NOT NULL,
  `contract_version` enum('old','new') NOT NULL,
  `create_date` datetime NOT NULL,
  `first_activation_date` datetime DEFAULT NULL,
  `last_activation_date` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `is_agreement_accept` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `agreement_accept_date` datetime NOT NULL,
  `agreement_number` varchar(32) NOT NULL,
  `post_index` varchar(6) NOT NULL,
  `adress` text NOT NULL,
  `contactname` varchar(255) NOT NULL,
  `is_suspected` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `first_moderated` tinyint(1) NOT NULL,
  `is_onmoderate` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `is_onmoderate_rejected` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `is_onmoderate_info` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `is_onmoderate_trademark` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `is_onmoderate_dealership` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `is_onmoderate_salons` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `is_onmoderate_properties` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `is_new` tinyint(1) unsigned NOT NULL,
  `is_new_info` tinyint(1) unsigned NOT NULL,
  `is_new_properties` tinyint(1) unsigned NOT NULL,
  `is_mounting` tinyint(1) unsigned NOT NULL DEFAULT '1',
  `is_deleted` tinyint(1) unsigned NOT NULL,
  `is_freezed` tinyint(1) unsigned NOT NULL,
  `is_imported` tinyint(6) unsigned NOT NULL,
  `date_update` datetime NOT NULL,
  `id_1c` int(9) unsigned DEFAULT NULL,
  `id_clients` int(11) DEFAULT NULL,
  `id_agent_old` int(10) unsigned NOT NULL,
  `id_contacts_old` int(10) unsigned NOT NULL,
  `paid_with` datetime DEFAULT NULL,
  `paid_till` datetime DEFAULT NULL,
  `month_sum` int(11) DEFAULT NULL,
  `use_office_tariff` int(1) unsigned NOT NULL DEFAULT '0',
  `use_premium_tariff` int(1) unsigned NOT NULL DEFAULT '0',
  `is_gold_partner` int(1) unsigned NOT NULL DEFAULT '0',
  `discard_agency_discount` int(1) unsigned DEFAULT NULL,
  `wallet_available` int(1) unsigned NOT NULL DEFAULT '1',
  `is_self_registered` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `auto_prolong` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `use_office7_v2` tinyint(1) NOT NULL DEFAULT '0',
  `can_switch_v2` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `company_id` int(11) NOT NULL,
  `new_billing_available` tinyint(1) DEFAULT '0',
  `name` varchar(255) DEFAULT NULL,
  `sale_discount_available` tinyint(1) NOT NULL DEFAULT '1',
  `money_running_out` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `calls_auction_available` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_1c_UNIQUE` (`id_1c`),
  KEY `product_id` (`product_id`),
  KEY `group_id` (`group_id`),
  KEY `id_clients` (`id_clients`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `office7`.`client_dealers`;
--
CREATE TABLE `office7`.`client_dealers` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `client_id` int(10) unsigned NOT NULL,
  `mark_id` int(10) unsigned NOT NULL,
  `date_start` timestamp NOT NULL DEFAULT '1980-01-01 00:00:01',
  `date_end` timestamp NOT NULL DEFAULT '1980-01-01 00:00:01',
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `mark` (`client_id`,`mark_id`),
  KEY `client_id` (`client_id`),
  KEY `mark_id` (`mark_id`),
  KEY `date` (`date_start`,`date_end`),
  CONSTRAINT `client_dealers_ibfk_2` FOREIGN KEY (`client_id`) REFERENCES `office7`.`clients` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `office7`.`client_poi`;
--
CREATE TABLE `office7`.`client_poi` (
  `client_id` int(10) unsigned NOT NULL,
  `poi_id` int(10) unsigned NOT NULL,
  PRIMARY KEY (`client_id`,`poi_id`),
  UNIQUE KEY `poi_id` (`poi_id`),
  CONSTRAINT `client_poi_ibfk_2` FOREIGN KEY (`client_id`) REFERENCES `office7`.`clients` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
create database if not exists `poi7`;
--
drop table if exists `poi7`.`types`;
--
CREATE TABLE `poi7`.`types` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(10) NOT NULL,
  `title` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `poi7`.`poi_types`;
--
CREATE TABLE `poi7`.`poi_types` (
  `type_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `type_name` varchar(10) NOT NULL,
  `type_title` varchar(255) NOT NULL,
  PRIMARY KEY (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `poi7`.`poi`;
--
CREATE TABLE `poi7`.`poi` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `hash` varchar(6) NOT NULL,
  `type_id` int(11) unsigned NOT NULL,
  `country_id` int(11) unsigned DEFAULT NULL,
  `region_id` int(11) unsigned DEFAULT NULL,
  `city_id` int(11) unsigned DEFAULT NULL,
  `ya_country_id` int(11) unsigned DEFAULT NULL,
  `ya_region_id` int(11) unsigned DEFAULT NULL,
  `ya_city_id` int(11) unsigned DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `lat` float(11,6) DEFAULT NULL,
  `lng` float(11,6) DEFAULT NULL,
  `tile_16` int(11) unsigned DEFAULT NULL,
  `rand` float NOT NULL,
  `create_date` datetime DEFAULT NULL,
  `change_date` datetime NOT NULL,
  `active` enum('New','Good','Bad','Wait') NOT NULL DEFAULT 'New',
  `call_tracking_on` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_poi_type_id` (`type_id`),
  KEY `lat` (`lat`) USING BTREE,
  KEY `lng` (`lng`),
  KEY `tile16` (`tile_16`),
  KEY `rand` (`rand`),
  KEY `active` (`active`),
  CONSTRAINT `FK_poi_type_id` FOREIGN KEY (`type_id`) REFERENCES `poi7`.`types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `poi7`.`poi_prop_names`;
--
CREATE TABLE `poi7`.`poi_prop_names` (
  `prop_name_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `type_id` int(10) unsigned NOT NULL,
  `name` varchar(45) NOT NULL,
  PRIMARY KEY (`prop_name_id`) USING BTREE,
  KEY `FK_prop_names_type_id` (`type_id`),
  KEY `prop_names_name` (`name`),
  CONSTRAINT `FK_prop_names_type_id` FOREIGN KEY (`type_id`) REFERENCES `poi7`.`poi_types` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `poi7`.`poi_prop_values`;
--
CREATE TABLE `poi7`.`poi_prop_values` (
  `prop_value_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `poi_id` int(10) unsigned NOT NULL,
  `prop_name_id` int(10) unsigned NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`prop_value_id`),
  KEY `FK_prop_values_poi_id` (`poi_id`),
  KEY `FK_poi_prop_values_prop_name_id` (`prop_name_id`),
  KEY `poi_prop_values_value` (`value`(5)),
  CONSTRAINT `FK_poi_prop_values_prop_name_id` FOREIGN KEY (`prop_name_id`) REFERENCES `poi7`.`poi_prop_names` (`prop_name_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_prop_values_poi_id` FOREIGN KEY (`poi_id`) REFERENCES `poi7`.`poi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `poi7`.`poi_phones`;
--
CREATE TABLE `poi7`.`poi_phones` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `poi7`.`call_tracking`;
--
CREATE TABLE `poi7`.`call_tracking` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `poi_id` int(11) unsigned NOT NULL DEFAULT '0',
  `link_id` varchar(24) NOT NULL DEFAULT '',
  `poi_phone_id` int(11) unsigned NOT NULL DEFAULT '0',
  `poi_phone_number` bigint(20) unsigned NOT NULL DEFAULT '0',
  `virtual_phone_id` varchar(24) NOT NULL DEFAULT '',
  `virtual_phone_number` bigint(20) unsigned NOT NULL DEFAULT '0',
  `virtual_phone_mask` varchar(10) NOT NULL DEFAULT '1:3:7',
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `poi_phone_id` (`poi_phone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `poi7`.`poi_contacts`;
--
CREATE TABLE `poi7`.`poi_contacts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poi_id` int(11) NOT NULL,
  `title` varchar(45) NOT NULL,
  `position` int(2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `poi_id` (`poi_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `poi7`.`poi_contacts_phones`;
--
CREATE TABLE `poi7`.`poi_contacts_phones` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poi_contacts_id` int(11) NOT NULL,
  `title` varchar(45) NOT NULL,
  `phone` bigint(20) NOT NULL,
  `phone_mask` varchar(45) NOT NULL,
  `phone_id` int(11) NOT NULL,
  `call_from` time NOT NULL,
  `call_till` time NOT NULL,
  PRIMARY KEY (`id`),
  KEY `poi_contacts_id` (`poi_contacts_id`),
  KEY `phone_id` (`phone_id`),
  KEY `phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
create database if not exists `users`;
--
drop table if exists `users`.`phone_numbers`;
--
CREATE TABLE `users`.`phone_numbers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned DEFAULT NULL,
  `number` int(11) DEFAULT NULL,
  `phone` bigint(12) NOT NULL,
  `status` tinyint(1) NOT NULL,
  `is_main` tinyint(1) NOT NULL,
  `code` int(11) NOT NULL,
  `create_date` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `phone` (`phone`),
  UNIQUE KEY `number` (`number`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `users`.`user`;
--
CREATE TABLE `users`.`user` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `email` varchar(70) DEFAULT NULL,
  `new_email` varchar(70) NOT NULL,
  `password_hash` varchar(32) NOT NULL,
  `password_date` datetime NOT NULL,
  `lastwrite` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `reg_ip` varchar(255) DEFAULT NULL,
  `last_ip` varchar(255) DEFAULT NULL,
  `active` tinyint(1) NOT NULL,
  `active_code` varchar(255) NOT NULL,
  `remind_code` varchar(32) NOT NULL,
  `set_date` datetime NOT NULL,
  `delete` date NOT NULL,
  `log_ip` varchar(255) NOT NULL,
  `pd_accepted_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `active` (`active`),
  KEY `active_code` (`active_code`),
  KEY `set_date` (`set_date`),
  KEY `delete` (`delete`),
  KEY `remind_idx` (`remind_code`),
  KEY `last_ip` (`last_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `users`.`reason_archive_users`;
--
CREATE TABLE `users`.`reason_archive_users` (
  `sale_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `reason_id` int(11) NOT NULL,
  `many_calls` tinyint(2) NOT NULL DEFAULT '0',
  `date_update` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `price_of_sales` int(11) DEFAULT NULL,
  UNIQUE KEY `sale_id` (`sale_id`),
  UNIQUE KEY `sale_id_2` (`sale_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
create database if not exists `catalog7_yandex`;
--
drop table if exists `catalog7_yandex`.`categories`;
--
CREATE TABLE `catalog7_yandex`.`categories` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `parent_id` int(10) unsigned DEFAULT NULL,
  `lid` int(11) NOT NULL,
  `rid` int(11) NOT NULL,
  `ignored` tinyint(1) NOT NULL DEFAULT '0',
  `level` smallint(2) NOT NULL,
  `name` varchar(255) NOT NULL,
  `alias` varchar(255) NOT NULL,
  `description` mediumtext NOT NULL,
  `is_completed` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `IX_parent_id_alias` (`parent_id`,`alias`),
  KEY `IX_lid_rid` (`lid`,`rid`),
  KEY `IX_level` (`level`),
  KEY `FK_categories_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`engines`;
--
CREATE TABLE `catalog7_yandex`.`engines` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `mark_id` int(10) unsigned DEFAULT NULL,
  `ya_tech_param_id` int(10) unsigned DEFAULT NULL,
  `autoru_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ya_tech_param_idx` (`ya_tech_param_id`),
  KEY `IX_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`marks`;
--
CREATE TABLE `catalog7_yandex`.`marks` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `cyrillic_name` varchar(255) NOT NULL,
  `alias` varchar(255) NOT NULL DEFAULT '',
  `description` mediumtext NOT NULL,
  `is_popular` tinyint(4) NOT NULL,
  `is_completed` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `tecdoc_id` int(11) NOT NULL,
  `country_id` int(11) NOT NULL DEFAULT '0',
  `ya_code` varchar(255) DEFAULT NULL,
  `autoru_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `IX_alias` (`alias`),
  UNIQUE KEY `ya_code_idx` (`ya_code`),
  KEY `IX_name` (`name`),
  KEY `country_id` (`country_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`folders`;
--
CREATE TABLE `catalog7_yandex`.`folders` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `parent_id` int(10) unsigned DEFAULT NULL,
  `lid` int(11) NOT NULL,
  `rid` int(11) NOT NULL,
  `level` smallint(2) NOT NULL,
  `ignored` tinyint(1) NOT NULL DEFAULT '0',
  `mark_id` int(10) unsigned DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `cyrillic_name` varchar(255) NOT NULL,
  `alias` varchar(255) NOT NULL,
  `description` mediumtext NOT NULL,
  `is_popular` tinyint(4) NOT NULL DEFAULT '0',
  `hidden` tinyint(1) NOT NULL,
  `v3_type` enum('group','model') DEFAULT NULL,
  `v3_id` int(10) unsigned DEFAULT NULL,
  `ya_code` varchar(255) DEFAULT NULL,
  `autoru_id` int(11) DEFAULT NULL,
  `auto_group` enum('BUSINESS','CITY','FAMILY') DEFAULT NULL,
  `auto_segment` enum('ECONOMY','MEDIUM','PREMIUM') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `IX_parent_id_mark_alias` (`parent_id`,`mark_id`,`alias`),
  KEY `IX_lid_rid` (`lid`,`rid`),
  KEY `FK_folders_parent_id` (`parent_id`),
  KEY `FK_folders_mark_id` (`mark_id`),
  KEY `ix_name` (`name`),
  KEY `ix_lid` (`lid`),
  KEY `ix_rid` (`rid`),
  KEY `ix_level` (`level`),
  KEY `ya_code_idx` (`ya_code`),
  KEY `autoru_idx` (`autoru_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`labels`;
--
CREATE TABLE `catalog7_yandex`.`labels` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `mark_id` int(10) unsigned NOT NULL,
  `name` varchar(100) NOT NULL,
  `type` enum('default','front') NOT NULL DEFAULT 'default',
  `sort` int(10) NOT NULL,
  `autoru_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `mark_id_name_type_idx` (`mark_id`,`name`,`type`),
  KEY `mark_id` (`mark_id`),
  KEY `sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`modifications`;
--
CREATE TABLE `catalog7_yandex`.`modifications` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `mark_id` int(10) unsigned NOT NULL,
  `folder_id` int(10) unsigned NOT NULL,
  `category_id` int(10) unsigned NOT NULL,
  `label_id` int(10) unsigned DEFAULT NULL,
  `front_label_id` int(10) unsigned DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `description` mediumtext NOT NULL,
  `start_year` year(4) DEFAULT NULL,
  `end_year` year(4) DEFAULT NULL,
  `v3_id` int(10) unsigned DEFAULT NULL,
  `engine_id` int(10) unsigned DEFAULT NULL,
  `copy_id` int(10) unsigned DEFAULT NULL,
  `default` tinyint(4) NOT NULL DEFAULT '0',
  `tecdoc_id` int(10) unsigned DEFAULT NULL,
  `price` float(10,2) unsigned DEFAULT NULL,
  `currency` enum('RUR','EUR','USD','CNY') NOT NULL DEFAULT 'RUR',
  `auto_name` varchar(255) NOT NULL DEFAULT '',
  `auto_suffix` varchar(255) NOT NULL DEFAULT '',
  `auto_class` enum('A','B','C','D','E','F','J','M','S') DEFAULT NULL,
  `ya_tech_param_id` int(10) unsigned DEFAULT NULL,
  `ya_configuration_id` int(10) unsigned DEFAULT NULL,
  `autoru_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ya_tech_param_configuration_idx` (`ya_tech_param_id`,`ya_configuration_id`),
  KEY `FK_modifications_category_id` (`category_id`),
  KEY `FK_modifications_folder_id` (`folder_id`),
  KEY `FK_modifications_mark_id` (`mark_id`),
  KEY `FK_modifications_engine_id` (`engine_id`),
  KEY `IX_category_id_mark_id` (`mark_id`,`category_id`),
  KEY `ix_start_year` (`start_year`),
  KEY `ix_end_year` (`end_year`),
  KEY `label_id` (`label_id`),
  KEY `v3_idx` (`v3_id`),
  KEY `front_label_id_idx` (`front_label_id`),
  KEY `autoru_idx` (`autoru_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`tech_units`;
--
CREATE TABLE `catalog7_yandex`.`tech_units` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `unit` varchar(30) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`tech_groups`;
--
CREATE TABLE `catalog7_yandex`.`tech_groups` (
  `id` tinyint(2) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `engine` tinyint(1) unsigned NOT NULL,
  `sorder` tinyint(2) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IX_sorder` (`sorder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`tech_properties`;
--
CREATE TABLE `catalog7_yandex`.`tech_properties` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` int(10) unsigned NOT NULL,
  `group_id` tinyint(2) unsigned NOT NULL,
  `type` enum('checkbox','text','select','int') NOT NULL,
  `rule` enum('numeric','nonzero') DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `alias` varchar(255) NOT NULL,
  `print` smallint(1) unsigned NOT NULL,
  `unit_id` int(10) unsigned DEFAULT NULL,
  `sorder` smallint(3) unsigned NOT NULL,
  `copy_id` int(10) unsigned DEFAULT NULL,
  `searchable` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `is_group_property` tinyint(4) NOT NULL DEFAULT '0',
  `ya_attr` varchar(255) DEFAULT NULL,
  `ya_attr_index` int(11) DEFAULT NULL,
  `ya_attr_verba` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ya_attrs_idx` (`ya_attr`,`ya_attr_index`,`ya_attr_verba`),
  KEY `IX_sorder` (`sorder`),
  KEY `IX_searchable` (`searchable`),
  KEY `FK_tech_properties_category_id` (`category_id`),
  KEY `FK_tech_properties_group_id` (`group_id`),
  KEY `FK_tech_properties_unit_id` (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`tech_selects`;
--
CREATE TABLE `catalog7_yandex`.`tech_selects` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `property_id` int(10) unsigned NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `alias` varchar(255) DEFAULT NULL,
  `sorder` smallint(3) DEFAULT '0',
  `ya_code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `property_id_alias` (`property_id`,`alias`),
  UNIQUE KEY `property_ya_code_idx` (`property_id`,`ya_code`),
  KEY `IX_sorder` (`sorder`),
  KEY `FK_tech_selects_property_id` (`property_id`),
  KEY `alias` (`alias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`tech_selects_synonyms`;
--
CREATE TABLE `catalog7_yandex`.`tech_selects_synonyms` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `tech_select_id` int(10) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `alias` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tech_select_id` (`tech_select_id`),
  KEY `fk_tech_select_id` (`tech_select_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`tech_modifications`;
--
CREATE TABLE `catalog7_yandex`.`tech_modifications` (
  `modification_id` int(10) unsigned NOT NULL,
  `property_id` int(10) unsigned NOT NULL,
  `synonym_id` int(10) unsigned DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `select` tinyint(1) unsigned NOT NULL,
  PRIMARY KEY (`modification_id`,`property_id`),
  KEY `FK_tech_modifications_modification_id` (`modification_id`),
  KEY `FK_tech_modifications_property_id` (`property_id`),
  KEY `synonym_idx` (`synonym_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `catalog7_yandex`.`tech_engines`;
--
CREATE TABLE `catalog7_yandex`.`tech_engines` (
  `engine_id` int(10) unsigned NOT NULL,
  `property_id` int(10) unsigned NOT NULL,
  `value` varchar(255) NOT NULL,
  `select` tinyint(1) NOT NULL,
  PRIMARY KEY (`engine_id`,`property_id`),
  KEY `FK_tech_engines_engine_id` (`engine_id`),
  KEY `FK_tech_engines_property_id` (`property_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
create database if not exists `all`;
--
drop table if exists `all`.`phone_set`;
--
CREATE TABLE `all`.`phone_set` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL DEFAULT '',
  `converter` varchar(255) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all`.`phones`;
--
CREATE TABLE `all`.`phones` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `set_id` int(10) unsigned NOT NULL DEFAULT '0',
  `user_id` int(11) NOT NULL DEFAULT '0',
  `phone` bigint(20) NOT NULL DEFAULT '0',
  `t_phone` varchar(255) NOT NULL DEFAULT '',
  `phone_mask` varchar(255) NOT NULL DEFAULT '',
  `converter` varchar(255) NOT NULL DEFAULT '0',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `set_id` (`set_id`),
  KEY `user_id` (`user_id`),
  KEY `phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all`.`sale3`;
--
CREATE TABLE `all`.`sale3` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `hash` varchar(8) NOT NULL,
  `category_id` mediumint(9) NOT NULL DEFAULT '0',
  `section_id` smallint(5) unsigned NOT NULL DEFAULT '0',
  `price_file_id` int(11) NOT NULL DEFAULT '0',
  `client_id` int(11) NOT NULL DEFAULT '0',
  `client_id_old` int(11) NOT NULL,
  `contact_id` int(11) NOT NULL DEFAULT '0',
  `phone_set_id` int(10) unsigned DEFAULT '0',
  `user_id` int(11) NOT NULL DEFAULT '0',
  `sms_id` int(11) NOT NULL DEFAULT '0',
  `sms_color` tinyint(1) NOT NULL DEFAULT '0',
  `client_color` tinyint(1) NOT NULL,
  `create_date` datetime NOT NULL,
  `set_date` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `expire_date` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `mark_id` int(11) NOT NULL DEFAULT '0',
  `mark_type` tinyint(3) unsigned NOT NULL DEFAULT '2',
  `group_id` mediumint(9) NOT NULL DEFAULT '0',
  `model_id` int(11) NOT NULL DEFAULT '0',
  `model_name` varchar(255) NOT NULL DEFAULT '',
  `modification_id` int(11) NOT NULL DEFAULT '0',
  `modification_name` varchar(255) NOT NULL DEFAULT '',
  `body_key` int(5) NOT NULL DEFAULT '0',
  `cabin_key` smallint(4) DEFAULT NULL,
  `loading` int(5) DEFAULT NULL,
  `length` int(5) NOT NULL,
  `width` int(5) NOT NULL,
  `height` int(5) NOT NULL,
  `container_type` int(5) NOT NULL,
  `body_volume` int(5) NOT NULL,
  `axis` tinyint(1) DEFAULT NULL,
  `trail_loading` int(5) DEFAULT NULL,
  `seats` mediumint(2) DEFAULT NULL,
  `beds` tinyint(2) DEFAULT NULL,
  `saddle_height` smallint(5) unsigned NOT NULL DEFAULT '0',
  `suspension_chassis` smallint(5) unsigned NOT NULL,
  `suspension_cabin` smallint(5) unsigned NOT NULL,
  `suspension_type` smallint(5) unsigned NOT NULL,
  `brake_type` smallint(5) unsigned NOT NULL,
  `wheel_drive` smallint(5) unsigned NOT NULL,
  `eco_class` tinyint(3) unsigned DEFAULT NULL,
  `fuel_tanks` tinyint(3) unsigned NOT NULL DEFAULT '1',
  `fuel_tanks_volume` smallint(5) unsigned NOT NULL DEFAULT '0',
  `fuel_tanks_material` smallint(5) unsigned NOT NULL,
  `trailer_type` smallint(5) unsigned NOT NULL,
  `bus_type` smallint(5) unsigned NOT NULL,
  `color_id` int(11) NOT NULL DEFAULT '0',
  `transmission_key` smallint(5) NOT NULL DEFAULT '0',
  `engine_key` smallint(5) NOT NULL DEFAULT '0',
  `engine_volume` int(11) NOT NULL DEFAULT '0',
  `engine_power` mediumint(9) NOT NULL DEFAULT '0',
  `drive_key` smallint(5) NOT NULL DEFAULT '0',
  `wheel_key` smallint(5) NOT NULL DEFAULT '0',
  `year` int(11) NOT NULL DEFAULT '0',
  `run` int(6) NOT NULL DEFAULT '0',
  `run_key` enum('km','mile') NOT NULL DEFAULT 'km',
  `price` int(6) NOT NULL DEFAULT '0',
  `price_usd` int(6) unsigned NOT NULL DEFAULT '0',
  `available_key` tinyint(2) NOT NULL DEFAULT '1',
  `currency_key` enum('USD','RUR','EUR','CNY') NOT NULL DEFAULT 'USD',
  `haggle_key` tinyint(2) NOT NULL DEFAULT '0',
  `used_key` tinyint(2) NOT NULL DEFAULT '0',
  `custom_key` tinyint(2) NOT NULL DEFAULT '0',
  `change_key` tinyint(3) unsigned DEFAULT NULL,
  `vin` varchar(30) NOT NULL DEFAULT '',
  `extras` set('1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30','31','32','33','34','35','36','37','38','39','40','41','42','43','44','45','46','47','48','49','50','51','52','53','54','55','56','57','58','59','60','61','62','63','64') NOT NULL DEFAULT '',
  `photo` tinyint(1) NOT NULL DEFAULT '0',
  `address` varchar(255) DEFAULT NULL,
  `description` mediumtext NOT NULL,
  `country_id` int(11) NOT NULL DEFAULT '0',
  `region_id` int(11) NOT NULL DEFAULT '0',
  `city_id` int(11) NOT NULL DEFAULT '0',
  `ip` varchar(255) NOT NULL DEFAULT '',
  `converter` varchar(255) NOT NULL DEFAULT '',
  `id_old` int(10) unsigned NOT NULL DEFAULT '0',
  `deleted_time` datetime NOT NULL,
  `deleted_by_user` int(11) NOT NULL,
  `deleted_reason_alias` varchar(64) NOT NULL,
  `deleted_reason_comment` mediumtext NOT NULL,
  `journal` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `journal_counter` smallint(5) unsigned NOT NULL,
  `status` tinyint(1) unsigned NOT NULL,
  `coordinates_lat` float(10,6) NOT NULL,
  `coordinates_lng` float(10,6) NOT NULL,
  `fresh_date` datetime DEFAULT NULL,
  `ya_country_id` int(10) unsigned NOT NULL DEFAULT '0',
  `ya_region_id` int(10) unsigned NOT NULL DEFAULT '0',
  `ya_city_id` int(10) unsigned NOT NULL DEFAULT '0',
  `type_id` int(5) unsigned DEFAULT '0',
  `load_height` int(4) unsigned DEFAULT '0',
  `crane_radius` mediumint(4) unsigned DEFAULT '0',
  `operating_hours` int(5) DEFAULT '0',
  `traction_class` mediumint(5) DEFAULT '0',
  `bucket_volume` float(10,6) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_category_section_id_idx` (`id`,`category_id`,`section_id`),
  KEY `mark_id` (`mark_id`),
  KEY `client_id` (`client_id`),
  KEY `user_id` (`user_id`),
  KEY `set_date` (`set_date`),
  KEY `section_id` (`section_id`),
  KEY `phone_set_id` (`phone_set_id`),
  KEY `category_id` (`category_id`,`section_id`),
  KEY `price_usd` (`price_usd`),
  KEY `group_id` (`group_id`),
  KEY `client_color` (`client_color`),
  KEY `ix_city_id` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
----
drop table if exists `all`.`sale3_ids`;
--
CREATE TABLE `all`.`sale3_ids` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `IX_sales_id_created` (`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
ALTER TABLE `all`.`sale3_ids` AUTO_INCREMENT=18839743;
--
drop table if exists `all`.`sale_images`;
--
CREATE TABLE `all`.`sale_images` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category_id` smallint(5) unsigned NOT NULL DEFAULT '0',
  `section_id` smallint(5) unsigned NOT NULL DEFAULT '0',
  `sale_id` int(10) unsigned NOT NULL DEFAULT '0',
  `main` tinyint(1) NOT NULL DEFAULT '0',
  `imagename` varchar(64) NOT NULL DEFAULT '',
  `imagesize` varchar(16) NOT NULL DEFAULT '',
  `backup_date` datetime DEFAULT NULL,
  `is_suspect` tinyint(1) NOT NULL,
  `isilon` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `exif_latitude` float(10,6) NOT NULL,
  `exif_longitude` float(10,6) NOT NULL,
  `exif_date` datetime NOT NULL,
  `order` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `main` (`main`),
  KEY `sale_id` (`sale_id`),
  KEY `section_id` (`section_id`),
  KEY `category_id` (`category_id`),
  KEY `backup_date` (`backup_date`),
  KEY `is_suspect` (`is_suspect`),
  KEY `imagename` (`imagename`),
  KEY `category_section_sale` (`category_id`,`section_id`,`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all`.`sale5`;
--
CREATE TABLE `all`.`sale5` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `hash` varchar(8) NOT NULL,
  `category_id` mediumint(9) NOT NULL DEFAULT '0',
  `section_id` smallint(5) unsigned NOT NULL DEFAULT '0',
  `price_file_id` int(11) NOT NULL DEFAULT '0',
  `client_id` int(11) NOT NULL DEFAULT '0',
  `client_id_old` int(11) NOT NULL,
  `contact_id` int(11) NOT NULL DEFAULT '0',
  `phone_set_id` int(10) unsigned DEFAULT '0',
  `user_id` int(11) NOT NULL DEFAULT '0',
  `sms_id` int(11) NOT NULL DEFAULT '0',
  `sms_color` tinyint(1) NOT NULL DEFAULT '0',
  `client_color` tinyint(1) NOT NULL,
  `create_date` datetime NOT NULL,
  `set_date` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `expire_date` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `mark_id` int(11) NOT NULL DEFAULT '0',
  `mark_type` tinyint(3) unsigned NOT NULL DEFAULT '2',
  `group_id` mediumint(9) NOT NULL DEFAULT '0',
  `model_id` int(11) NOT NULL DEFAULT '0',
  `model_name` varchar(255) NOT NULL DEFAULT '',
  `modification_id` int(11) NOT NULL DEFAULT '0',
  `modification_name` varchar(255) NOT NULL DEFAULT '',
  `type_id` int(11) NOT NULL DEFAULT '0',
  `color_id` int(11) NOT NULL DEFAULT '0',
  `transmission_key` smallint(5) NOT NULL DEFAULT '0',
  `engine_key` smallint(5) NOT NULL DEFAULT '0',
  `engine_volume` int(11) NOT NULL DEFAULT '0',
  `engine_power` mediumint(9) NOT NULL DEFAULT '0',
  `cylinders` mediumint(5) NOT NULL DEFAULT '0',
  `cylinders_type` mediumint(5) NOT NULL DEFAULT '0',
  `strokes` mediumint(5) NOT NULL DEFAULT '0',
  `displacement` mediumint(6) NOT NULL DEFAULT '0',
  `operating_hours` int(10) unsigned DEFAULT NULL,
  `loading` int(10) DEFAULT NULL,
  `seats` int(5) DEFAULT NULL,
  `drive_key` mediumint(5) NOT NULL DEFAULT '0',
  `wheel_key` mediumint(5) NOT NULL DEFAULT '0',
  `year` int(11) NOT NULL DEFAULT '0',
  `run` int(6) NOT NULL DEFAULT '0',
  `run_key` enum('km','mile') NOT NULL DEFAULT 'km',
  `price` int(6) NOT NULL DEFAULT '0',
  `price_usd` int(6) unsigned NOT NULL DEFAULT '0',
  `available_key` tinyint(2) NOT NULL DEFAULT '1',
  `currency_key` enum('USD','RUR','EUR','CNY') NOT NULL DEFAULT 'USD',
  `haggle_key` tinyint(2) NOT NULL DEFAULT '0',
  `used_key` tinyint(2) NOT NULL DEFAULT '0',
  `custom_key` tinyint(2) NOT NULL DEFAULT '0',
  `change_key` tinyint(3) unsigned DEFAULT NULL,
  `weight` char(10) NOT NULL,
  `used_for_key` smallint(4) unsigned NOT NULL,
  `type_key` smallint(4) unsigned NOT NULL,
  `wheel_size` tinyint(2) NOT NULL,
  `speed_num` tinyint(2) NOT NULL,
  `body_size` char(10) NOT NULL,
  `back_brake_key` smallint(4) unsigned NOT NULL,
  `front_brake_key` smallint(4) unsigned NOT NULL,
  `vin` varchar(30) NOT NULL DEFAULT '',
  `extras` set('1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30','31','32','33','34','35','36','37','38','39','40','41','42','43','44','45','46','47','48','49','50','51','52','53','54','55','56','57','58','59','60','61','62','63','64') NOT NULL DEFAULT '',
  `photo` tinyint(1) NOT NULL DEFAULT '0',
  `address` varchar(255) DEFAULT NULL,
  `description` mediumtext NOT NULL,
  `country_id` int(11) NOT NULL DEFAULT '0',
  `region_id` int(11) NOT NULL DEFAULT '0',
  `city_id` int(11) NOT NULL DEFAULT '0',
  `ip` varchar(255) NOT NULL DEFAULT '',
  `converter` varchar(255) NOT NULL DEFAULT '',
  `id_old` int(10) unsigned NOT NULL DEFAULT '0',
  `deleted_time` datetime NOT NULL,
  `deleted_by_user` int(11) NOT NULL,
  `deleted_reason_alias` varchar(64) NOT NULL,
  `deleted_reason_comment` mediumtext NOT NULL,
  `journal` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `journal_counter` smallint(5) unsigned NOT NULL,
  `status` tinyint(1) unsigned NOT NULL,
  `coordinates_lat` float(10,6) NOT NULL,
  `coordinates_lng` float(10,6) NOT NULL,
  `fresh_date` datetime DEFAULT NULL,
  `ya_country_id` int(10) unsigned NOT NULL DEFAULT '0',
  `ya_region_id` int(10) unsigned NOT NULL DEFAULT '0',
  `ya_city_id` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_category_section_id_idx` (`id`,`category_id`,`section_id`),
  KEY `mark_id` (`mark_id`),
  KEY `client_id` (`client_id`),
  KEY `user_id` (`user_id`),
  KEY `section_id` (`section_id`),
  KEY `category_id` (`category_id`,`section_id`),
  KEY `price_usd` (`price_usd`),
  KEY `group_id` (`group_id`),
  KEY `set_date` (`set_date`),
  KEY `ix_city_id` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all`.`sale5_ids`;
--
CREATE TABLE `all`.`sale5_ids` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `IX_sales_id_created` (`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
ALTER TABLE `all`.`sale5_ids` AUTO_INCREMENT=3945165;
--
drop table if exists `all`.`sale4`;
--
CREATE TABLE `all`.`sale4` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `hash` varchar(8) NOT NULL,
  `category_id` mediumint(9) NOT NULL DEFAULT '0',
  `section_id` smallint(5) unsigned NOT NULL DEFAULT '0',
  `price_file_id` int(11) NOT NULL DEFAULT '0',
  `client_id` int(11) NOT NULL DEFAULT '0',
  `client_id_old` int(11) NOT NULL,
  `contact_id` int(11) NOT NULL DEFAULT '0',
  `phone_set_id` int(10) unsigned DEFAULT '0',
  `user_id` int(11) NOT NULL DEFAULT '0',
  `sms_id` int(11) NOT NULL DEFAULT '0',
  `sms_color` tinyint(1) NOT NULL DEFAULT '0',
  `client_color` tinyint(1) NOT NULL,
  `create_date` datetime NOT NULL,
  `set_date` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `expire_date` datetime NOT NULL DEFAULT '1980-01-01 00:00:01',
  `mark_id` int(11) NOT NULL DEFAULT '0',
  `mark_type` tinyint(3) unsigned NOT NULL DEFAULT '2',
  `group_id` mediumint(9) NOT NULL DEFAULT '0',
  `model_id` int(11) NOT NULL DEFAULT '0',
  `model_name` varchar(255) NOT NULL DEFAULT '',
  `modification_id` int(11) NOT NULL DEFAULT '0',
  `modification_name` varchar(255) NOT NULL DEFAULT '',
  `type_id` int(5) NOT NULL DEFAULT '0',
  `body_key` int(5) NOT NULL DEFAULT '0',
  `loading` int(6) NOT NULL DEFAULT '0',
  `load_height` mediumint(4) NOT NULL DEFAULT '0',
  `crane_radius` mediumint(4) NOT NULL DEFAULT '0',
  `operating_hours` int(5) NOT NULL DEFAULT '0',
  `traction_class` mediumint(5) NOT NULL DEFAULT '0',
  `bucket_volume` mediumint(5) NOT NULL DEFAULT '0',
  `color_id` int(11) NOT NULL DEFAULT '0',
  `transmission_key` smallint(5) NOT NULL DEFAULT '0',
  `engine_key` smallint(5) NOT NULL DEFAULT '0',
  `engine_volume` int(11) NOT NULL DEFAULT '0',
  `engine_power` mediumint(9) NOT NULL DEFAULT '0',
  `drive_key` smallint(5) NOT NULL DEFAULT '0',
  `wheel_key` smallint(5) NOT NULL DEFAULT '0',
  `year` int(11) NOT NULL DEFAULT '0',
  `run` int(6) NOT NULL DEFAULT '0',
  `run_key` enum('km','mile') NOT NULL DEFAULT 'km',
  `price` int(6) NOT NULL DEFAULT '0',
  `price_usd` int(6) unsigned NOT NULL DEFAULT '0',
  `available_key` tinyint(2) NOT NULL DEFAULT '1',
  `currency_key` enum('USD','RUR','EUR','CNY') NOT NULL DEFAULT 'USD',
  `haggle_key` tinyint(2) NOT NULL DEFAULT '0',
  `used_key` tinyint(2) NOT NULL DEFAULT '0',
  `custom_key` tinyint(2) NOT NULL DEFAULT '0',
  `change_key` tinyint(3) unsigned DEFAULT NULL,
  `trail_loading` tinyint(5) NOT NULL,
  `vin` varchar(30) NOT NULL DEFAULT '',
  `extras` set('1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30','31','32','33','34','35','36','37','38','39','40','41','42','43','44','45','46','47','48','49','50','51','52','53','54','55','56','57','58','59','60','61','62','63','64') NOT NULL DEFAULT '',
  `photo` tinyint(1) NOT NULL DEFAULT '0',
  `address` varchar(255) DEFAULT NULL,
  `description` mediumtext NOT NULL,
  `country_id` int(11) NOT NULL DEFAULT '0',
  `region_id` int(11) NOT NULL DEFAULT '0',
  `city_id` int(11) NOT NULL DEFAULT '0',
  `ip` varchar(255) NOT NULL DEFAULT '',
  `converter` varchar(255) NOT NULL DEFAULT '',
  `id_old` int(10) unsigned NOT NULL DEFAULT '0',
  `deleted_time` datetime NOT NULL,
  `deleted_by_user` int(11) NOT NULL,
  `deleted_reason_alias` varchar(64) NOT NULL,
  `deleted_reason_comment` mediumtext NOT NULL,
  `journal` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `journal_counter` smallint(5) unsigned NOT NULL,
  `status` tinyint(1) unsigned NOT NULL,
  `coordinates_lat` float(10,6) NOT NULL,
  `coordinates_lng` float(10,6) NOT NULL,
  `fresh_date` datetime DEFAULT NULL,
  `ya_country_id` int(10) unsigned NOT NULL DEFAULT '0',
  `ya_region_id` int(10) unsigned NOT NULL DEFAULT '0',
  `ya_city_id` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_category_section_id_idx` (`id`,`category_id`,`section_id`),
  KEY `mark_id` (`mark_id`),
  KEY `client_id` (`client_id`),
  KEY `user_id` (`user_id`),
  KEY `run_key` (`run_key`),
  KEY `section_id` (`section_id`),
  KEY `category_id` (`category_id`,`section_id`),
  KEY `group_id` (`group_id`),
  KEY `deleted_time` (`deleted_time`),
  KEY `set_date` (`set_date`),
  KEY `ix_city_id` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all`.`sales_truck_change_log`;
--
CREATE TABLE `all`.`sales_truck_change_log` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` int(10) unsigned NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all`.`sales_moto_change_log`;
--
CREATE TABLE `all`.`sales_moto_change_log` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
drop table if exists `all`.`sales_special_change_log`;
--
CREATE TABLE `all`.`sales_special_change_log` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sale_id` int(10) unsigned NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
--
