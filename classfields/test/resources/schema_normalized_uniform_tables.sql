CREATE TABLE instances_main (
    hash Uint64,
    user_id Utf8,
    object_id Utf8,
    payload string,
    context string,
    update_time timestamp,
    expire_at timestamp,
    PRIMARY KEY (hash, user_id, object_id)
) WITH (
    TTL = Interval("PT0S") ON expire_at,
    UNIFORM_PARTITIONS = 2
);

CREATE TABLE instances_signals (
    hash Uint64,
    user_id Utf8,
    object_id Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY (hash, user_id, object_id, key)
) WITH (
    TTL = Interval("PT0S") ON expire_at,
    UNIFORM_PARTITIONS = 2
);

CREATE TABLE instances_signal_switch_offs (
    hash Uint64,
    user_id Utf8,
    object_id Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY (hash, user_id, object_id, key)
) WITH (
    TTL = Interval("PT0S") ON expire_at,
    UNIFORM_PARTITIONS = 2
);

CREATE TABLE instances_metadata (
    hash Uint64,
    user_id Utf8,
    object_id Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY (hash, user_id, object_id, key)
) WITH (
    TTL = Interval("PT0S") ON expire_at,
    UNIFORM_PARTITIONS = 2
);