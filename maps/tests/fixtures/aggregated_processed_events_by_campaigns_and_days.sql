-- FORWARD --
CREATE MATERIALIZED VIEW aggregated_processed_events_by_campaigns_and_days_distributed
-- ON CLUSTER  '{cluster}'  -- for production cluster
(
    campaign_id UInt32,
    date Date,
    call AggregateFunction(
        count,
        Enum8(
            'TECHNICAL_PROCESSED_TO' = 9,
            'BILLBOARD_SHOW' = 1,
            'BILLBOARD_TAP' = 2,
            'ACTION_CALL' = 3,
            'ACTION_MAKE_ROUTE' = 4,
            'ACTION_SEARCH' = 5,
            'ACTION_OPEN_SITE' = 6,
            'ACTION_OPEN_APP' = 7,
            'ACTION_SAVE_OFFER' = 8
        )
    ),
    make_route AggregateFunction(
        count,
        Enum8(
            'TECHNICAL_PROCESSED_TO' = 9,
            'BILLBOARD_SHOW' = 1,
            'BILLBOARD_TAP' = 2,
            'ACTION_CALL' = 3,
            'ACTION_MAKE_ROUTE' = 4,
            'ACTION_SEARCH' = 5,
            'ACTION_OPEN_SITE' = 6,
            'ACTION_OPEN_APP' = 7,
            'ACTION_SAVE_OFFER' = 8
        )
    ),
    open_site AggregateFunction(
        count,
        Enum8(
            'TECHNICAL_PROCESSED_TO' = 9,
            'BILLBOARD_SHOW' = 1,
            'BILLBOARD_TAP' = 2,
            'ACTION_CALL' = 3,
            'ACTION_MAKE_ROUTE' = 4,
            'ACTION_SEARCH' = 5,
            'ACTION_OPEN_SITE' = 6,
            'ACTION_OPEN_APP' = 7,
            'ACTION_SAVE_OFFER' = 8
        )
    ),
    open_app AggregateFunction(
        count,
        Enum8(
            'TECHNICAL_PROCESSED_TO' = 9,
            'BILLBOARD_SHOW' = 1,
            'BILLBOARD_TAP' = 2,
            'ACTION_CALL' = 3,
            'ACTION_MAKE_ROUTE' = 4,
            'ACTION_SEARCH' = 5,
            'ACTION_OPEN_SITE' = 6,
            'ACTION_OPEN_APP' = 7,
            'ACTION_SAVE_OFFER' = 8
        )
    ),
    save_offer AggregateFunction(
        count,
        Enum8(
            'TECHNICAL_PROCESSED_TO' = 9,
            'BILLBOARD_SHOW' = 1,
            'BILLBOARD_TAP' = 2,
            'ACTION_CALL' = 3,
            'ACTION_MAKE_ROUTE' = 4,
            'ACTION_SEARCH' = 5,
            'ACTION_OPEN_SITE' = 6,
            'ACTION_OPEN_APP' = 7,
            'ACTION_SAVE_OFFER' = 8
        )
    ),
    search AggregateFunction(
        count,
        Enum8(
            'TECHNICAL_PROCESSED_TO' = 9,
            'BILLBOARD_SHOW' = 1,
            'BILLBOARD_TAP' = 2,
            'ACTION_CALL' = 3,
            'ACTION_MAKE_ROUTE' = 4,
            'ACTION_SEARCH' = 5,
            'ACTION_OPEN_SITE' = 6,
            'ACTION_OPEN_APP' = 7,
            'ACTION_SAVE_OFFER' = 8
        )
    ),
    show AggregateFunction(
        count,
        Enum8(
            'TECHNICAL_PROCESSED_TO' = 9,
            'BILLBOARD_SHOW' = 1,
            'BILLBOARD_TAP' = 2,
            'ACTION_CALL' = 3,
            'ACTION_MAKE_ROUTE' = 4,
            'ACTION_SEARCH' = 5,
            'ACTION_OPEN_SITE' = 6,
            'ACTION_OPEN_APP' = 7,
            'ACTION_SAVE_OFFER' = 8
        )
    ),
    tap AggregateFunction(
        count,
        Enum8(
            'TECHNICAL_PROCESSED_TO' = 9,
            'BILLBOARD_SHOW' = 1,
            'BILLBOARD_TAP' = 2,
            'ACTION_CALL' = 3,
            'ACTION_MAKE_ROUTE' = 4,
            'ACTION_SEARCH' = 5,
            'ACTION_OPEN_SITE' = 6,
            'ACTION_OPEN_APP' = 7,
            'ACTION_SAVE_OFFER' = 8
        )
    ),
    charged_sum AggregateFunction(sum, Decimal(11, 6)),
    show_unique AggregateFunction(uniq, String)
)
    ENGINE=AggregatingMergeTree()  -- only for testing
--     ENGINE = ReplicatedAggregatingMergeTree(
--         '/clickhouse/tables/{shard}/aggregated_processed_events_by_campaigns_and_days_distributed',
--         '{replica}'
--     )
        PARTITION BY toDate(date)
        ORDER BY (campaign_id, date)
        SETTINGS index_granularity = 8192
AS
SELECT campaign_id,
       multiIf(
           timezone = 'Asia/Irkutsk', toDate(receive_timestamp, 'Asia/Irkutsk'),
           timezone = 'Asia/Kamchatka', toDate(receive_timestamp, 'Asia/Kamchatka'),
           timezone = 'Asia/Magadan', toDate(receive_timestamp, 'Asia/Magadan'),
           timezone = 'Asia/Novosibirsk', toDate(receive_timestamp, 'Asia/Novosibirsk'),
           timezone = 'Asia/Omsk', toDate(receive_timestamp, 'Asia/Omsk'),
           timezone = 'Asia/Vladivostok', toDate(receive_timestamp, 'Asia/Vladivostok'),
           timezone = 'Asia/Yakutsk', toDate(receive_timestamp, 'Asia/Yakutsk'),
           timezone = 'Asia/Yekaterinburg', toDate(receive_timestamp, 'Asia/Yekaterinburg'),
           timezone = 'Europe/Kaliningrad', toDate(receive_timestamp, 'Europe/Kaliningrad'),
           timezone = 'Europe/Moscow', toDate(receive_timestamp, 'Europe/Moscow'),
           timezone = 'Europe/Samara', toDate(receive_timestamp, 'Europe/Samara'),
           toDate(receive_timestamp, 'UTC')
       )                                                          AS date,
       countStateIf(event_name, event_name = 'ACTION_CALL')       AS call,
       countStateIf(event_name, event_name = 'ACTION_MAKE_ROUTE') AS make_route,
       countStateIf(event_name, event_name = 'ACTION_OPEN_SITE')  AS open_site,
       countStateIf(event_name, event_name = 'ACTION_OPEN_APP')   AS open_app,
       countStateIf(event_name, event_name = 'ACTION_SAVE_OFFER') AS save_offer,
       countStateIf(event_name, event_name = 'ACTION_SEARCH')     AS search,
       countStateIf(event_name, event_name = 'BILLBOARD_SHOW')    AS show,
       countStateIf(event_name, event_name = 'BILLBOARD_TAP')     AS tap,
       sumState(cost)                                             AS charged_sum,
       uniqState(device_id)                                       AS show_unique
-- FROM stat.processed_events  -- for production cluster
FROM processed_events_distributed
GROUP BY campaign_id,
         multiIf(
           timezone = 'Asia/Irkutsk', toDate(receive_timestamp, 'Asia/Irkutsk'),
           timezone = 'Asia/Kamchatka', toDate(receive_timestamp, 'Asia/Kamchatka'),
           timezone = 'Asia/Magadan', toDate(receive_timestamp, 'Asia/Magadan'),
           timezone = 'Asia/Novosibirsk', toDate(receive_timestamp, 'Asia/Novosibirsk'),
           timezone = 'Asia/Omsk', toDate(receive_timestamp, 'Asia/Omsk'),
           timezone = 'Asia/Vladivostok', toDate(receive_timestamp, 'Asia/Vladivostok'),
           timezone = 'Asia/Yakutsk', toDate(receive_timestamp, 'Asia/Yakutsk'),
           timezone = 'Asia/Yekaterinburg', toDate(receive_timestamp, 'Asia/Yekaterinburg'),
           timezone = 'Europe/Kaliningrad', toDate(receive_timestamp, 'Europe/Kaliningrad'),
           timezone = 'Europe/Moscow', toDate(receive_timestamp, 'Europe/Moscow'),
           timezone = 'Europe/Samara', toDate(receive_timestamp, 'Europe/Samara'),
           toDate(receive_timestamp, 'UTC')
       )
;


-- BACKWARD --
DROP TABLE aggregated_processed_events_by_campaigns_and_days_distributed;
