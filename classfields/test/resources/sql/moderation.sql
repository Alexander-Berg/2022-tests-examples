create table if not exists moderation
(
	id int unsigned auto_increment
		primary key,
	entity_type enum('client_info', 'client_details', 'client_dealership', 'client_logo', 'salon', 'client_properties', 'client_certificate', 'client_tariff') not null,
	entity_id int unsigned not null,
	status enum('changed', 'rejected', 'stashed') default 'changed' not null,
	data text not null,
	date timestamp default CURRENT_TIMESTAMP not null
);

create table if not exists client_poi
(
	client_id int unsigned not null,
	poi_id int unsigned not null,
	primary key (client_id, poi_id),
	constraint poi_id
		unique (poi_id)
);

insert into moderation
    (entity_type, entity_id, status, data)
values
    ('client_properties', 1, 'changed', ''),
    ('salon', 2, 'changed', 'a:14:{s:11:"_resolution";s:0:"";s:10:"everyday24";s:1:"0";s:11:"hide_phones";b:1;s:9:"client_id";s:4:"2778";s:11:"description";s:0:"";s:6:"poi_id";s:4:"2744";s:6:"submit";s:18:"Сохранить";s:5:"title";s:18:"Евроспорт";s:3:"url";s:0:"";s:8:"services";a:1:{i:0;a:6:{s:2:"id";s:0:"";s:4:"name";s:0:"";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:10:"mark_limit";s:0:"";s:10:"delete_row";s:0:"";}}s:6:"phones";a:3:{i:0;a:11:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"926";s:5:"phone";s:7:"0067255";s:9:"extention";s:0:"";s:2:"id";s:6:"221752";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:16:"Менеджер";s:9:"call_from";s:2:"10";s:9:"call_till";s:2:"21";s:10:"delete_row";s:0:"";}i:1;a:11:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"916";s:5:"phone";s:7:"0019991";s:9:"extention";s:0:"";s:2:"id";s:6:"221753";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:16:"Менеджер";s:9:"call_from";s:2:"10";s:9:"call_till";s:2:"21";s:10:"delete_row";s:0:"";}i:2;a:11:{s:12:"country_code";s:1:"7";s:9:"city_code";s:3:"903";s:5:"phone";s:7:"1253607";s:9:"extention";s:0:"";s:2:"id";s:6:"221754";s:9:"marks_ids";s:0:"";s:11:"marks_names";s:0:"";s:5:"title";s:16:"Менеджер";s:9:"call_from";s:2:"10";s:9:"call_till";s:2:"21";s:10:"delete_row";s:0:"";}}s:5:"photo";a:3:{s:6:"origin";s:0:"";s:3:"new";s:0:"";s:6:"delete";s:0:"";}s:3:"poi";a:7:{s:10:"country_id";s:1:"1";s:9:"region_id";s:2:"87";s:7:"city_id";s:4:"1123";s:7:"address";s:16:"ул. Улица";s:3:"lat";s:9:"53.732112";s:3:"lng";s:9:"36.726348";s:2:"id";s:4:"2744";}s:6:"schema";a:3:{s:6:"origin";s:0:"";s:3:"new";s:0:"";s:6:"delete";s:0:"";}}');

insert into client_poi
    (client_id, poi_id)
values
    (1, 2),
    (2, 3),
    (105, 4),
    (999, 5);

CREATE TABLE `client_properties` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `client_id` int(10) unsigned NOT NULL,
  `field_id` int(10) unsigned NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `client_id_2` (`client_id`,`field_id`),
  KEY `client_id` (`client_id`),
  KEY `type_id` (`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `client_property_fields` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` int(10) unsigned NOT NULL,
  `status` enum('active','inactive') NOT NULL DEFAULT 'active',
  `required` enum('active','inactive') NOT NULL DEFAULT 'inactive',
  `type` enum('hidden','text','textarea','select','checkbox','radio') NOT NULL DEFAULT 'text',
  `alias` varchar(64) NOT NULL,
  `default` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `sorder` smallint(5) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `group_id_2` (`group_id`,`alias`),
  KEY `group_id` (`group_id`),
  KEY `sorder` (`sorder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Типы свойств клиента';

insert into client_property_fields (id, group_id, alias, name, description, sorder)
values (1, 1, 'block_reason', 'Причины блокировки', '', 0);

insert into client_properties (client_id, field_id, `value`) values (20101, 1, 'Жулики');
