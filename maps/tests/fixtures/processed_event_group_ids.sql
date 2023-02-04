-- FORWARD --
CREATE MATERIALIZED VIEW processed_event_group_ids_distributed
-- ON CLUSTER  '{cluster}'  -- for production cluster
(
    receive_timestamp DateTime,
    event_group_id String
)
    ENGINE=MergeTree()  -- only for testing
--     ENGINE = ReplicatedMergeTree(  -- for production cluster
--              '/clickhouse/tables/{shard}/processed_event_group_ids_distributed',
--              '{replica}'
--     )
        PARTITION BY toDate(receive_timestamp)
        ORDER BY (receive_timestamp, event_group_id)
        SETTINGS index_granularity = 8192

AS
SELECT receive_timestamp, event_group_id
-- FROM stat.processed_events_distributed  -- for production cluster
FROM processed_events_distributed
;

-- BACKWARD --
DROP TABLE processed_event_group_ids_distributed;
