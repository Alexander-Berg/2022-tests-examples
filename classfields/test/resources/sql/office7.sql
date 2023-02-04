CREATE TABLE `clients` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` int(10) unsigned NOT NULL DEFAULT '1',
  `product_id` smallint(5) unsigned NOT NULL,
  `country_id` int(10) unsigned NOT NULL,
  `region_id` int(10) unsigned NOT NULL,
  `city_id` int(10) unsigned NOT NULL,
  `ya_country_id` int(10) unsigned NOT NULL,
  `ya_region_id` int(10) unsigned NOT NULL,
  `ya_city_id` int(10) unsigned NOT NULL,
  `url` varchar(255) NOT NULL COMMENT 'Client''s site url',
  `email` varchar(70) NOT NULL,
  `responsible_manager_email` varchar(255) NULL DEFAULT NULL,
  `phone` bigint(20) unsigned NOT NULL COMMENT 'Client''s phone number',
  `phone_mask` varchar(10) NOT NULL COMMENT 'Client''s phone mask',
  `description` text NOT NULL COMMENT 'Client description',
  `logo` varchar(255) NOT NULL COMMENT 'Logo URL',
  `logosize` varchar(32) NOT NULL COMMENT 'Logo size',
  `comment` mediumtext NOT NULL COMMENT 'Comment about client (for managers only)',
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
  `last_activation_date` datetime DEFAULT NULL,
  `is_agreement_accept` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'подтверждено ли пользовательское соглашение',
  `agreement_accept_date` datetime NOT NULL,
  `agreement_number` varchar(32) NOT NULL COMMENT 'Номер договора клиента',
  `post_index` varchar(6) NOT NULL,
  `adress` text NOT NULL,
  `contactname` varchar(255) NOT NULL,
  `is_suspected` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'Является ли подозрительным по мнению spy',
  `first_moderated` tinyint(1) NOT NULL COMMENT 'Прошел первичную модерацию',
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
  `month_sum` int(11) DEFAULT '0',
  `use_office_tariff` int(1) unsigned NOT NULL DEFAULT '0',
  `use_premium_tariff` int(1) unsigned NOT NULL DEFAULT '0',
  `is_gold_partner` int(1) unsigned NOT NULL DEFAULT '0',
  `discard_agency_discount` int(1) unsigned DEFAULT NULL,
  `wallet_available` int(1) unsigned NOT NULL DEFAULT '1',
  `is_self_registered` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `auto_prolong` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `use_office7_v2` tinyint(1) NOT NULL DEFAULT '1',
  `can_switch_v2` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `company_id` int(11) NOT NULL COMMENT 'группы компаний',
  `new_billing_available` tinyint(1) DEFAULT '0',
  `name` varchar(255) DEFAULT NULL,
  `sale_discount_available` tinyint(1) NOT NULL DEFAULT '1',
  `money_running_out` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `with_autoservices` tinyint(1) NOT NULL DEFAULT '0',
  `single_payment_model` varchar(128) DEFAULT NULL,
  `actual_stock` tinyint(1) NOT NULL DEFAULT '0',
  `feedprocessor_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `is_loyalty` tinyint(1) NOT NULL DEFAULT '0',
  `calls_auction_available` tinyint(1) NOT NULL DEFAULT '0',
  `redemption_available` tinyint(1) NOT NULL DEFAULT '0',
  `epoch` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `images_cache_breaker` int(11) DEFAULT NULL,
  `first_moderation_date` timestamp(3) NULL DEFAULT NULL,
  `priority_placement` tinyint(1) NOT NULL DEFAULT '0',
  `multiposting_enabled` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_1c_UNIQUE` (`id_1c`),
  KEY `product_id` (`product_id`),
  KEY `group_id` (`group_id`),
  KEY `id_clients` (`id_clients`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Clients table';

INSERT INTO `clients` (`id`,`group_id`,`product_id`,`country_id`,`region_id`,`city_id`,`ya_country_id`,`ya_region_id`,`ya_city_id`,`url`,`email`,`phone`,`phone_mask`,`description`,`logo`,`logosize`,`comment`,`is_agent`,`agent_id`,`loader_id`,`origin`,`status`,`contact_name`,`fax`,`fax_mask`,`contract_version`,`create_date`,`first_activation_date`,`last_activation_date`,`is_agreement_accept`,`agreement_accept_date`,`agreement_number`,`post_index`,`adress`,`contactname`,`is_suspected`,`first_moderated`,`is_onmoderate`,`is_onmoderate_rejected`,`is_onmoderate_info`,`is_onmoderate_trademark`,`is_onmoderate_dealership`,`is_onmoderate_salons`,`is_onmoderate_properties`,`is_new`,`is_new_info`,`is_new_properties`,`is_mounting`,`is_deleted`,`is_freezed`,`is_imported`,`date_update`,`id_1c`,`id_clients`,`id_agent_old`,`id_contacts_old`,`paid_with`,`paid_till`,`month_sum`,`use_office_tariff`,`use_premium_tariff`,`is_gold_partner`,`discard_agency_discount`,`wallet_available`,`is_self_registered`,`auto_prolong`,`use_office7_v2`,`can_switch_v2`,`company_id`,`new_billing_available`,`name`,`sale_discount_available`,`money_running_out`,`with_autoservices`,`single_payment_model`,`actual_stock`,`feedprocessor_enabled`,`is_loyalty`,`calls_auction_available`,`redemption_available`,`epoch`,`images_cache_breaker`,`first_moderation_date`, `priority_placement`,`multiposting_enabled`)
VALUES
(1,1,65,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',0,0,0,'msk0105','deleted','НБСмотор',0,'','new','2005-10-18 00:00:01','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,0,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 1, 0),
(2,1,65,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',0,0,0,'msk0105','deleted','НБСмотор',0,'','new','2005-10-18 00:00:02','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,0,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 1, 0),
(100,1,65,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',0,0,0,'msk0105','deleted','НБСмотор',0,'','new','2005-10-18 00:00:03','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,0,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(102,1,65,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',0,0,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:04','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,0,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(104,1,65,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',0,0,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:05','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,0,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(105,1,65,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',0,106,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:06','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,1,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(106,1,65,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',1,0,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:07','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,0,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(107,1,65,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',1,0,0,'msk0106','active','НБСмотор',0,'','new','2005-10-18 00:00:08','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,0,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 1),
(200,1,0,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',1,0,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:09','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,190,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(201,1,0,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',1,0,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:10','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,191,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(333,1,0,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',1,0,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:11','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,777,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(555,1,0,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',1,0,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:12','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,999,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(444,1,0,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',1,0,0,'msk0105','active','НБСмотор',0,'','new','2005-10-18 00:00:13','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,888,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0),
(999,1,0,1,42,1255,225,11316,65,'http://www.nbsmotor.ru','msk@nbsmotor.ru',74956020822,'1:3:7','Продажа подержанных мотоциклов из Японии.\r\nМотозапчасти и расходники. Экипировка IXS.\r\nОбслуживание мототехники. Принимаем мотоциклы на комиссию, возможен обмен. Постоянно в наличии широкий ассортимент мотоциклов. \r\nМы работаем ежедневно с 09:00 до 21:00','','','Компания НБСмотор - карты: msk0105, spb0461 ',1,0,0,'msk0105','new','НБСмотор',0,'','new','2005-10-18 00:00:14','2005-10-18 00:00:00','2017-04-19 01:00:02',0,'2019-01-01 00:00:00','','','ул. Пролетарская 271/5','',0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,'2015-03-25 01:31:01',NULL,4,0,0,'2018-11-08 05:00:01','2018-11-08 05:00:01',0,0,0,0,1,1,0,0,1,0,888,1,'НБСмотор',1,1,0,'cars:used,commercial',1,1,0,0,0,'2019-06-18 14:27:04.578',NULL,'2005-10-18 00:00:00.000', 0, 0);

CREATE TABLE `offer_docs` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `text` longtext NOT NULL,
  `name` varchar(250) DEFAULT NULL,
  `version` varchar(100) DEFAULT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8;

CREATE TABLE `offer_agreement` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `client_id` int(10) NOT NULL,
  `user_id` int(10) DEFAULT NULL,
  `offer_docs_id` int(10) NOT NULL,
  `date` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `client_id` (`client_id`,`offer_docs_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `client_dealers` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `client_id` int(10) unsigned NOT NULL,
  `mark_id` int(10) unsigned NOT NULL,
  `date_start` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `date_end` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `mark` (`client_id`,`mark_id`),
  KEY `client_id` (`client_id`),
  KEY `mark_id` (`mark_id`),
  KEY `date` (`date_start`,`date_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `client_dealers` (`client_id`, `mark_id`)
VALUES (20101, 1),
       (16453, 2),
       (20101, 3);

INSERT INTO `client_dealers` (`id`, `client_id`, `mark_id`)
VALUES (4, 10000, 10);
INSERT INTO `client_dealers` (`id`, `client_id`, `mark_id`, `date`)
VALUES (5, 105, 3, '2019-06-18 14:27:04.578');

CREATE TABLE `files` (
    `id` int(10) NOT NULL AUTO_INCREMENT,
    `entity` varchar(100) DEFAULT NULL,
    `entity_id` int(10) DEFAULT NULL,
    `code` varchar(100) DEFAULT NULL,
    `filename` varchar(250) DEFAULT NULL,
    `extra_data` text DEFAULT NULL,
    `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isilon2mds` int(10) DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `files` (`filename`,`code`,`entity`,`entity_id`)
VALUES ("img1.jpg", "photo", "salon", 1),
       ("img2.jpg", "photo", "client", 1),
       ("img3.jpg", "photo", "salon", 2),
       ("img4.jpg", "photo", "client", 2),
       ("img5.jpg", "dealer_certificate", "dealer", 4),
       ("img5.jpg", "dealer_certificate", "dealer", 5),
       ("img6.jpg", "photo", "salon", 4);

CREATE TABLE `client_users` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `client_id` int(10) unsigned NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  `client_group` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `client_unique` (`client_id`,`user_id`),
  UNIQUE KEY `user_unique` (`user_id`),
  KEY `user_id` (`user_id`),
  KEY `client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `client_users`(`client_id`, `user_id`, `client_group`)
VALUES (20101, 1, 'test'),
       (16453, 2, 'test'),
       (20101, 3, 'test'),
       (105, 4, 'test');


CREATE TABLE `companies` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(250) NOT NULL,
  `create_date` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `companies`(id, `title`, `create_date`)
VALUES (1, 'test1', '2019-01-01T21:00:00'),
       (2, 'tes2', '2019-01-01T21:00:00');

INSERT INTO `offer_docs` (`id`, `text`, `name`, `version`, `date`)
VALUES(19, '<div class="oferta-wrap"><h1 class="h1">Условия оказания услуг на сервисе Auto.ru</h1><p class="p">Настоящие условия являются официальным предложением ООО «Яндекс.Вертикали» (далее по тексту «Авто.ру») и содержат все существенные условия оказания технических услуг, предусмотренных настоящим документом в отношении Объявлений Клиентов на сервисе Auto.ru.</p><ol class="ol"><li class="li"><p class="p"><strong>ОПРЕДЕЛЕНИЯ И ТЕРМИНЫ</strong></p></li></ol></div>',
null, '03022017', '2017-02-03 12:48:01');

CREATE TABLE priority_placement_periods (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  client_id int(10) unsigned NOT NULL,
  start timestamp NOT NULL,
  finish timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY periods_clients (`client_id`),
  CONSTRAINT periods_clients FOREIGN KEY (`client_id`) REFERENCES clients (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO priority_placement_periods (`client_id`, `start`, `finish`)
VALUES (1, '2019-11-20', '2019-11-26'),
       (102, '2019-11-20', '2019-11-26'),
       (104, '2019-11-20', NULL);

create table external_offer_event_files (
    `id` int(12) unsigned not null auto_increment,
    `event_type` varchar(256) not null,
    `filename` varchar(256) not null,
    `date` timestamp not null default current_timestamp,
    primary key (`id`),
    unique key `event_filename` (`event_type`, `filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 DEFAULT COLLATE = utf8_unicode_ci;

CREATE TABLE `customer_discounts` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `customer_type` enum('CLIENT', 'COMPANY_GROUP') NOT NULL,
  `customer_id` int(11) unsigned NOT NULL,
  `product` enum('PLACEMENT', 'VAS') NOT NULL,
  `percent` int(11) unsigned NOT NULL,
  `edited_by_user_id` int(11) unsigned NOT NULL,
  `edited_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `loyalty_update_pushed_to_kafka` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE `customer_product` (`customer_id`,`customer_type`, `product`)
);
INSERT INTO `customer_discounts` (customer_type, customer_id, product, percent, edited_by_user_id, loyalty_update_pushed_to_kafka)
VALUES ('COMPANY_GROUP', 190, 'VAS', 10, 1, 0),
       ('COMPANY_GROUP', 191, 'VAS', 10, 1, 0),
       ('COMPANY_GROUP', 888, 'VAS', 10, 1, 1),
       ('CLIENT', 200, 'VAS', 10, 1, 0),
       ('CLIENT', 200, 'PLACEMENT', 10, 1, 0),
       ('CLIENT', 444, 'VAS', 10, 1, 1);
