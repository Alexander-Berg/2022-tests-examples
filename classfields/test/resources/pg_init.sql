create table push_queue
(
    id               varchar(36)  not null,
    data             bytea        not null,
    created          timestamp    default CURRENT_TIMESTAMP,
    modified         timestamp    default CURRENT_TIMESTAMP,
    device_id        varchar(255) not null,
    metrica_group_id integer,
    metrica_tag_name varchar(255) default NULL::character varying,
    status           smallint     default 1,
    delivery_name    varchar(255) default NULL::character varying,
    deliver_from     timestamp,
    deliver_to       timestamp,
    next_check       timestamp,
    client_type      varchar(255) default NULL::character varying,
    name             varchar(255) default NULL::character varying
);

create index push_queue_id_idx
    on push_queue (id);

create index push_queue_status_idx
    on push_queue (status);

create index push_queue_next_check_idx
    on push_queue (next_check);
