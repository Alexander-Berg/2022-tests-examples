CREATE TABLE instance_archive
(
    hashed_id String,
    external_id Utf8,
    update_time Timestamp,
    instance_data String,
    PRIMARY KEY(hashed_id, external_id, update_time)
) WITH (
    TTL = Interval("P30D") ON update_time
);
