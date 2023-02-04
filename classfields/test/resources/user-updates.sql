CREATE TABLE user_updates (
    key Utf8,
    value String,
    expire_at timestamp,
    PRIMARY KEY (key)
) WITH (
    TTL = Interval("PT0S") ON expire_at
);