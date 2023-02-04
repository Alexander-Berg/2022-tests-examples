-- FORWARD --
CREATE TABLE stat.processed_events_distributed
(
    receive_timestamp DateTime('UTC')
)
    ENGINE = MergeTree()
        ORDER BY (receive_timestamp)
        SETTINGS index_granularity = 8192
;

-- BACKWARD --
DROP TABLE IF EXISTS stat.processed_events_distributed;
