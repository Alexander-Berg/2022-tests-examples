CREATE TABLE instances_main (
    user_id Utf8,
    object_id Utf8,
    payload string,
    context string,
    update_time timestamp,
    expire_at timestamp,
    PRIMARY KEY (user_id, object_id)
) WITH (TTL = Interval("PT0S") ON expire_at);

CREATE TABLE instances_signals (
    user_id Utf8,
    object_id Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY (user_id, object_id, key)
) WITH (TTL = Interval("PT0S") ON expire_at);

CREATE TABLE instances_signal_switch_offs (
    user_id Utf8,
    object_id Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY (user_id, object_id, key)
) WITH (TTL = Interval("PT0S") ON expire_at);

CREATE TABLE instances_metadata (
    user_id Utf8,
    object_id Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY (user_id, object_id, key)
) WITH (TTL = Interval("PT0S") ON expire_at);