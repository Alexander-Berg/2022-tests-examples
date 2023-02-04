-- FORWARD --
CREATE TABLE mapkit_events_distributed -- ON CLUSTER  '{cluster}'
(
    event          String,
    reqid          String,
    log_time       DateTime('UTC'),
    req_time       DateTime('UTC'),
    log_id         String,
    user_lat       Float64,
    user_lon       Float64,
    device_id      String,
    user_agent     String,
    event_group_id String,
    place_id       String,
    receive_time   DateTime('UTC') DEFAULT now()
)
    ENGINE = MergeTree()  -- for testing
--     ENGINE = ReplicatedMergeTree(  -- for production
--             '/clickhouse/tables/{shard}/mapkit_events_distributed',
--             '{replica}'
--     )
--     PARTITION BY toDate(receive_time)

    ORDER BY receive_time
    SETTINGS index_granularity = 8192;


-- BACKWARD --
DROP TABLE mapkit_events_distributed;
