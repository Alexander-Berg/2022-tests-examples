CREATE TABLE instances (
    hash Uint64,
    user_id Utf8,
    object_id Utf8,
    column_name Utf8,
    value string,
    expire_at timestamp,
    PRIMARY KEY(hash, user_id, object_id, column_name)
) WITH (
    TTL = Interval("PT0S") ON expire_at,
    UNIFORM_PARTITIONS = 4
);

CREATE TABLE user_object_relations (
    hash Uint64,
    user_id Utf8,
    object_id Utf8,
    PRIMARY KEY(hash, user_id, object_id)
) WITH (
    UNIFORM_PARTITIONS = 4,
    AUTO_PARTITIONING_BY_SIZE = ENABLED,
    AUTO_PARTITIONING_PARTITION_SIZE_MB = 128
);

CREATE TABLE feed_object_relations (
    hash Uint64,
    feed_id Utf8,
    object_id Utf8,
    PRIMARY KEY(hash, feed_id, object_id)
) WITH (
    UNIFORM_PARTITIONS = 4,
    AUTO_PARTITIONING_BY_SIZE = ENABLED,
    AUTO_PARTITIONING_PARTITION_SIZE_MB = 128
);

CREATE TABLE entity_index (
                              hash Uint64,
                              entity_type Utf8,
                              entity_id Utf8,
                              external_id Utf8,
                              updated_at Timestamp,
                              PRIMARY KEY (hash, entity_type, entity_id, external_id)
) WITH (
      UNIFORM_PARTITIONS = 2
      );
