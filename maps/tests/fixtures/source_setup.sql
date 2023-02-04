CREATE DATABASE IF NOT EXISTS stat;

CREATE TABLE IF NOT EXISTS stat.source_sample (
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
) ENGINE = MergeTree() ORDER BY (CampaignID, EventName, ReceiveTimestamp);

INSERT INTO stat.source_sample VALUES
(
  1554894569,
  7726,
  '37b36bd03bf84fcfe8f95ab43191653c',
  4,
  '4CE92B30-6A33-457D-A7D4-1B8CBAD54597',
  'iOS',
  '1112',
  11476,
  55.718732876522175,
  37.40151579701865,
  'geoadv.bb.action.makeRoute'
),
(
  1554894569,
  7728,
  '525ef1ea2bd9042d64ac32d3ea4adbcb',
  4,
  '4CE92B30-6A33-457D-A7D4-1B8CBAD54597',
  'iOS',
  '1112',
  11476,
  55.72769056007629,
  37.470415387480365,
  'geoadv.bb.action.makeRoute'
),
(
  1554894817,
  7726,
  'f7b7fed9dfa910e5048008e19b542516',
  4,
  '455960B8-9F53-4C25-B009-C951D352B1D9',
  'iOS',
  '1112',
  11476,
  55.92484900267415,
  38.00368778411113,
  'geoadv.bb.action.makeRoute'
),
(
  1554894860,
  7726,
  'f7b7fed9dfa910e5048008e19b542516',
  4,
  '16C36014-C555-441E-A5A8-D6B89F30EBDF',
  'iOS',
  '1112',
  11476,
  55.60455126413299,
  37.979286976982266,
  'geoadv.bb.action.makeRoute'
),
(
  1554895027,
  7726,
  'f7b7fed9dfa910e5048008e19b542516',
  4,
  'C3887054-8C3C-490C-A173-89503A3F1632',
  'iOS',
  '1112',
  11476,
  55.95008912257879,
  37.54331668483207,
  'geoadv.bb.action.makeRoute'
),
(
  1554895041,
  7726,
  'f7b7fed9dfa910e5048008e19b542516',
  4,
  'BFBD251A-0DFD-4283-8BC2-B06FBDE9F5DE',
  'iOS',
  '1112',
  11476,
  55.830902099609375,
  37.30220815170305,
  'geoadv.bb.action.makeRoute'
),
(
  1554894569,
  7728,
  '525ef1ea2bd9042d64ac32d3ea4adbcb',
  9999,
  '4CE92B30-6A33-457D-A7D4-1B8CBAD54597',
  'iOS',
  '1112',
  11476,
  55.72769056007629,
  37.470415387480365,
  'geoadv.bb.action.makeRoute'
);

-- missed CampaignID field
INSERT INTO stat.source_sample (
  ReceiveTimestamp,
  EventGroupId,
  APIKey,
  DeviceID,
  AppPlatform,
  AppVersionName,
  AppBuildNumber,
  Latitude,
  Longitude,
  EventName)
VALUES (
  1554894579,
  '37b36bd03bf84fcfe8f95ab43191653c',
  4,
  '4CE92B30-6A33-457D-A7D4-1B8CBAD43597',
  'iOS',
  '1112',
  11476,
  55.718732876522175,
  37.40151579701865,
  'geoadv.bb.action.makeRoute'
),