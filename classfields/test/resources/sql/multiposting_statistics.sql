create table if not exists external_statistic_counters (
    offer_id varchar(256) not null,
    source varchar(256) not null,
    client_id bigint unsigned not null,
    epoch timestamp default current_timestamp on update current_timestamp not null,
    views bigint not null,
    phone_views bigint not null,
    favorites bigint not null,

    primary key (offer_id, source)
);

create index external_statistic_counters_epoch_idx on external_statistic_counters (epoch);

insert into external_statistic_counters (offer_id, source, client_id, epoch, views, phone_views, favorites)
values
    ('id1', 'src', 1, current_timestamp, 0, 0, 0),
    ('id2', 'src', 1, current_timestamp - interval 21 day, 0, 0, 0),
    ('id3', 'src', 1, current_timestamp - interval 25 day, 0, 0, 0),
    ('id4', 'src', 1, current_timestamp - interval 31 day, 0, 0, 0),
    ('id5', 'src', 1, current_timestamp - interval 33 day, 0, 0, 0);


create table avito_wallet_operation
(
    operation_id   varchar(40) primary key,
    client_id      bigint unsigned not null,
    created_at     timestamp not null,
    operation_type text      not null,
    operation_name text      not null,
    service_id     int,
    service_name   text,
    amount_total   decimal   not null,
    amount_rub     decimal   not null,
    amount_bonus   decimal   not null,
    avito_offer_id int,
    is_placement   tinyint   not null,
    is_vas         tinyint   not null,
    is_other       tinyint   not null
);
