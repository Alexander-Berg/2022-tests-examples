-- FORWARD --
CREATE TABLE normalized_events_distributed -- ON CLUSTER  '{cluster}'
(
    receive_timestamp DateTime ('UTC'),
    event_name Enum8(
        'TECHNICAL_PROCESSED_TO' = 9,
        'BILLBOARD_SHOW' = 1,
        'BILLBOARD_TAP' = 2,
        'ACTION_CALL' = 3,
        'ACTION_MAKE_ROUTE' = 4,
        'ACTION_SEARCH' = 5,
        'ACTION_OPEN_SITE' = 6,
        'ACTION_OPEN_APP' = 7,
        'ACTION_SAVE_OFFER' = 8
    ),
    campaign_id UInt32,
    event_group_id String,
    device_id String,
    application Enum8('NAVIGATOR' = 1, 'MOBILE_MAPS' = 2, 'METRO' = 3, 'BEEKEEPER' = 4),
    app_platform Enum8('ANDROID' = 1, 'IOS' = 2, 'LINUX' = 3),
    app_version_name String,  -- ? obsolete in favour of build number
    app_build_number UInt32, -- check all apps
    user_latitude Decimal(12, 9),
    user_longitude Decimal(12, 9),
    place_id Nullable(String),
    _normalization_metadata String
)
    ENGINE=MergeTree()  -- only for testing
--     ENGINE = ReplicatedMergeTree(  -- for production cluster
--              '/clickhouse/tables/{shard}/normalized_events_distributed',
--              '{replica}'
--     )
--     PARTITION BY toDate(receive_timestamp)

    ORDER BY (receive_timestamp)
    SETTINGS index_granularity = 8192;


-- BACKWARD --
DROP TABLE normalized_events_distributed;
