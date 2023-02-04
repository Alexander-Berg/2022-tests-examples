CREATE DATABASE IF NOT EXISTS stat;

CREATE TABLE IF NOT EXISTS stat.accepted_sample (
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

CREATE MATERIALIZED VIEW IF NOT EXISTS stat.accepted_sample_event_group_ids (
  ReceiveTimestamp DateTime,
  EventGroupId String
)
ENGINE = MergeTree()
ORDER BY (ReceiveTimestamp, EventGroupId)
SETTINGS index_granularity = 8192
AS
SELECT ReceiveTimestamp, EventGroupId
FROM stat.accepted_sample;
