create table if not exists managers
(
	id int unsigned auto_increment
		primary key,
	user_id int unsigned not null,
	active tinyint(1) unsigned default 1 not null,
	role enum('manager', 'admin', 'sales_manager') default 'manager' not null
);

create table manager_contact
(
	id int unsigned not null
		primary key,
	fio varchar(100) null,
	email varchar(100) null,
	work_phone_number bigint null,
	work_phone_number_ext bigint null,
	mobile_phone_number bigint null,
	telegram varchar(100) null,
	viber bigint null,
	whatsapp bigint null,
	skype varchar(100) null
);

create table if not exists links_map
(
	id int unsigned auto_increment
		primary key,
	link_name varchar(100) not null,
	key1 int unsigned not null,
	key2 int unsigned not null,
	constraint link_name_keys
		unique (link_name, key1, key2)
);

insert into managers
(`id`, `user_id`, `active`)
values
(5, 50, 1),
(6, 60, 1);

insert into manager_contact
(`id`, `email`)
values
(5, 'old@yandex-team.ru'),
(6, 'new@yandex-team.ru');

insert into links_map
(link_name, key1, key2)
values
('office7.client.id-crm.manager.id', 105, 5);