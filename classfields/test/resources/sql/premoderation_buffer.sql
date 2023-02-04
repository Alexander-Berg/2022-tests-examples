create table if not exists premoderation_buffer
(
	id bigint unsigned auto_increment
		primary key,
	client_id bigint unsigned not null,
	update_request JSON not null,
	reasons JSON,
	status ENUM('Outdated', 'Approved', 'OnModeration', 'New', 'Rejected') not null,
	dealer_epoch TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
	created TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
	user_id bigint unsigned not null
);
