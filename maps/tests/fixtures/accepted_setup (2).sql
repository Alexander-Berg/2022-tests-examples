-- FORWARD --
CREATE TABLE stat.accepted_sample (
    ReceiveTimestamp DateTime,
    CampaignID UInt32,
    EventGroupId String,
    APIKey UInt32,
    DeviceID String,
    AppPlatform String,
    AppVersionName String,
    AppBuildNumber UInt32,
    Latitude Float64,
    Longitude Float64,
    EventName String,
    Cost Decimal32(6)
)
ENGINE=MergeTree()
ORDER BY (ReceiveTimestamp, EventName, CampaignID);

CREATE MATERIALIZED VIEW stat.aggregated_sample
ENGINE = AggregatingMergeTree()
    PARTITION BY toDate(date)
    ORDER BY (CampaignID, date)
    SETTINGS index_granularity = 8192
AS SELECT
    CampaignID,
    toDate(ReceiveTimestamp) as date,
    countStateIf(EventName, EventName = 'geoadv.bb.action.call') AS call,
    countStateIf(EventName, EventName = 'geoadv.bb.action.makeRoute') AS makeRoute,
    countStateIf(EventName, EventName = 'geoadv.bb.action.openSite') AS openSite,
    countStateIf(EventName, EventName = 'geoadv.bb.action.saveOffer') AS saveOffer,
    countStateIf(EventName, EventName = 'geoadv.bb.action.search') AS search,
    countStateIf(EventName, EventName = 'geoadv.bb.pin.show') AS show,
    countStateIf(EventName, EventName = 'geoadv.bb.pin.tap') AS tap,
    sumState(Cost) AS charged_sum,
    uniqState(DeviceID) AS show_unique
FROM stat.accepted_sample
GROUP BY CampaignID, toDate(ReceiveTimestamp);


CREATE TABLE stat.processed_events_distributed (
    receive_timestamp DateTime ('UTC'),
    event_name Enum8(
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
    device_id String,
    cost Decimal(11, 6)
)
ENGINE=MergeTree()
ORDER BY (receive_timestamp, event_name, campaign_id);

CREATE TABLE stat.normalized_events_distributed (
    receive_timestamp DateTime ('UTC'),
    event_name Enum8(
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
    device_id String
)
ENGINE=MergeTree()
ORDER BY (receive_timestamp, event_name, campaign_id);

CREATE TABLE stat.mapkit_events_distributed (
    receive_time DateTime ('UTC'),
    event String,
    log_id String
)
ENGINE=MergeTree()
ORDER BY (receive_time, event, log_id);

CREATE MATERIALIZED VIEW stat.aggregated_processed_events_by_campaigns_and_days_distributed
(
    campaign_id UInt32,
    date Date,
    call AggregateFunction(
        count,
        Enum8(
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
    ENGINE=AggregatingMergeTree()
        PARTITION BY toDate(date)
        ORDER BY (campaign_id, date)
        SETTINGS index_granularity = 8192
AS
SELECT campaign_id,
       toDate(receive_timestamp)                                  AS date,
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
FROM stat.processed_events_distributed
GROUP BY campaign_id, toDate(receive_timestamp);

-- BACKWARD --
DROP TABLE IF EXISTS stat.accepted_sample;
DROP TABLE IF EXISTS stat.mapkit_events_distributed;
DROP TABLE IF EXISTS stat.normalized_events_distributed;
DROP TABLE IF EXISTS stat.processed_events_distributed;
DROP TABLE IF EXISTS stat.aggregated_sample;
DROP TABLE IF EXISTS stat.aggregated_processed_events_by_campaigns_and_days_distributed;
