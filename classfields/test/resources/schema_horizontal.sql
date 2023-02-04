CREATE TABLE instances (
    user_id Utf8,
    object_id Utf8,
    column_name Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY(user_id, object_id, column_name, key)
) WITH (TTL = Interval("PT0S") ON expire_at);