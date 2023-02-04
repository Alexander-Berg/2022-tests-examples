-- FORWARD --
CREATE TABLE stat.mapkit_events_distributed
(
    receive_time DateTime('UTC') DEFAULT now()
)
    ENGINE = MergeTree()
        ORDER BY receive_time
        SETTINGS index_granularity = 8192;


-- BACKWARD --
DROP TABLE IF EXISTS stat.mapkit_events_distributed;
