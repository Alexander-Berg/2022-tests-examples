create table if not exists client_comments
(
	client_id int(10) not null,
	date datetime not null,
	user_id int(10) not null,
	comment text not null,
	constraint client_id primary key (client_id)
) ENGINE = InnoDB
  AUTO_INCREMENT = 43864
  DEFAULT CHARSET = utf8;

insert into client_comments
    (client_id, date, user_id, comment)
values
    (1, '2020-11-03T17:00:00', 1, "Client 1 comment")
