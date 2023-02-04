create table if not exists clients_changed_buffer
(
	id bigint unsigned auto_increment
		primary key,
	client_id int not null,
	data_source varchar(100) not null
);

insert into `clients_changed_buffer`
(`client_id`, `data_source`)
values
(1, 'clients'),
(2, 'client_comments'),
(3, 'client_properties');