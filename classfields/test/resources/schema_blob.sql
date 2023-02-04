CREATE TABLE instances (
    user_id Utf8,
    object_id Utf8,
    payload string,
    signals string,
    context string,
    signal_switch_offs string,
    update_time timestamp,
    metadata string,
    expire_at timestamp,
    PRIMARY KEY (user_id, object_id)
) WITH (TTL = Interval("PT0S") ON expire_at);