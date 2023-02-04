CREATE TABLE instances (
    hash Uint64,
    user_id Utf8,
    object_id Utf8,
    column_name Utf8,
    key Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY(hash, user_id, object_id, column_name, key)
) WITH (
    TTL = Interval("PT0S") ON expire_at,
    UNIFORM_PARTITIONS = 2
);

CREATE TABLE user_object_relations (
    hash Uint64,
    user_id Utf8,
    object_id Utf8,
    PRIMARY KEY(hash, user_id, object_id)
) WITH (
    UNIFORM_PARTITIONS = 64,
    AUTO_PARTITIONING_BY_SIZE = ENABLED,
    AUTO_PARTITIONING_PARTITION_SIZE_MB = 128
);

CREATE TABLE feed_object_relations (
    hash Uint64,
    feed_id Utf8,
    object_id Utf8,
    PRIMARY KEY(hash, feed_id, object_id)
) WITH (
    UNIFORM_PARTITIONS = 64,
    AUTO_PARTITIONING_BY_SIZE = ENABLED,
    AUTO_PARTITIONING_PARTITION_SIZE_MB = 128
);