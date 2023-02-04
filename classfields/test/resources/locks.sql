CREATE TABLE locks (
    hash      Uint64,
    lock_id   utf8,
    is_locked bool,
    update_time Timestamp,
    PRIMARY KEY (hash, lock_id)
) WITH (
    UNIFORM_PARTITIONS = 2
);
