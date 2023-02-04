-- FORWARD --
CREATE TABLE maps_adv_statistics_raw_metrika_log_distributed (
  _timestamp DateTime,
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
) ENGINE = MergeTree() ORDER BY (CampaignID, EventName);

-- BACKWARD --
DROP TABLE maps_adv_statistics_raw_metrika_log_distributed;
