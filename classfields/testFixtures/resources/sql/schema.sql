create table offer_chat_revisit_queue(
    room_id varchar(36) not null primary key,
    revisit_after timestamp not null,
    attempt_number int not null,
    index revisit_after_index(revisit_after)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 DEFAULT COLLATE = utf8mb4_general_ci;

CREATE TABLE offer_chat_room_state (
    room_id VARCHAR(36) NOT NULL PRIMARY KEY,
    offer_id varchar(64) NOT NULL,
    state MEDIUMBLOB NOT NULL,
    state_json MEDIUMTEXT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    break_time timestamp(3) default null
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 DEFAULT COLLATE = utf8mb4_general_ci;

create table offer_chat_room_analytics (
    room_id varchar(36) not null primary key
  , created_time timestamp not null
  , category varchar(64) not null
  , switch_to_operator_time timestamp null
  , has_messages_from_user tinyint default 0
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 DEFAULT COLLATE = utf8mb4_general_ci;

create table offer_chat_start_date_responses (
    room_id varchar(36) not null primary key
  , config_version int not null
  , preset_index int null
  , response_text varchar(1024) not null
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 DEFAULT COLLATE = utf8mb4_general_ci;

create table offer_chat_idle_operator_ping_queue(
    room_id varchar(36) not null primary key
  , revisit_after timestamp not null
  , index revisit_after_index(revisit_after)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 DEFAULT COLLATE = utf8mb4_general_ci;

create table offer_chat_idle_user_ping_queue(
    room_id varchar(36) not null primary key
  , revisit_after timestamp not null
  , index revisit_after_index(revisit_after)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 DEFAULT COLLATE = utf8mb4_general_ci;
