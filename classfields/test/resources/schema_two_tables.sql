CREATE TABLE instances_main (
    user_id Utf8,
    object_id Utf8,
    payload string,
    context string,
    update_time timestamp,
    expire_at timestamp,
    PRIMARY KEY (user_id, object_id)
) WITH (TTL = Interval("PT0S") ON expire_at);

CREATE TABLE instances_kv (
    user_id Utf8,
    object_id Utf8,
    kv_name Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY (user_id, object_id, kv_name, key)
) WITH (TTL = Interval("PT0S") ON expire_at);