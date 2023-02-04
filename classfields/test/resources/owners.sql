CREATE TABLE user_owners (
    hashed_key Uint64,
    key_field utf8,
    signals_proto string,
    expire_at timestamp,
    PRIMARY KEY (hashed_key, key_field)
) WITH (
    UNIFORM_PARTITIONS = 2,
    TTL = Interval("PT0S") ON expire_at
);
