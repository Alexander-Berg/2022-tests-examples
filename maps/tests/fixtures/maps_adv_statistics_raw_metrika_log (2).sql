-- FORWARD --
CREATE TABLE stat.maps_adv_statistics_raw_metrika_log_distributed
(
    _timestamp       DateTime,
    _partition       String,
    _offset          UInt64,
    _idx             UInt32,
    ReceiveTimestamp DateTime,
    CampaignID       UInt32,
    EventGroupId     String,
    APIKey           UInt32,
    DeviceID         String,
    AppPlatform      String,
    AppVersionName   String,
    AppBuildNumber   UInt32,
    Latitude         Float64,
    Longitude        Float64,
    EventName        String
)
    ENGINE = MergeTree()
        ORDER BY (CampaignID, EventName, ReceiveTimestamp);

-- BACKWARD --
DROP TABLE IF EXISTS stat.maps_adv_statistics_raw_metrika_log_distributed;
