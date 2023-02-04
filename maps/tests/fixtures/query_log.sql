-- FORWARD --
CREATE TABLE sys.query_log
(
    type              Enum8(
        'QueryStart' = 1,
        'QueryFinish' = 2,
        'ExceptionBeforeStart' = 3,
        'ExceptionWhileProcessing' = 4
        ),
    event_time        DateTime,
    query_duration_ms UInt64,
    memory_usage      UInt64,
    query             String,
    exception         String
)
    ENGINE = MergeTree
        PARTITION BY toYYYYMMDD(event_time)
        ORDER BY (toYYYYMMDD(event_time), event_time)
        SETTINGS index_granularity = 1024;

-- BACKWARD --
DROP TABLE IF EXISTS sys.query_log;
