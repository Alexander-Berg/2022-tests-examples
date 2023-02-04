CREATE DATABASE IF NOT EXISTS stat;

CREATE TABLE IF NOT EXISTS stat.normalized_sample (
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
    EventName String
) ENGINE=MergeTree() ORDER BY (ReceiveTimestamp, EventName, CampaignID);
