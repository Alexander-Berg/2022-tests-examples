CREATE TABLE offer_event_counter(
  day Date,
  id UInt64,
  component String,
  total UInt64
) ENGINE = SummingMergeTree()
  PARTITION BY toYYYYMM(day)
  ORDER BY (id, component, day)
  SETTINGS index_granularity = 8192;

insert into offer_event_counter(day, id, component, total)
values
('2018-05-01', 2, 'card_view', 5),
('2018-05-01', 2, 'phone_show', 5),
('2018-05-02', 2, 'card_view', 1),
('2018-05-02', 3, 'card_view', 8);


CREATE TABLE offer_event_counter_wide_time(
  timestamp DateTime,
  id UInt64,
  card_view UInt64,
  phone_show UInt64
) ENGINE = SummingMergeTree()
  PARTITION BY toYYYYMM(timestamp)
  ORDER BY (id, timestamp)
  SETTINGS index_granularity = 8192;

insert into offer_event_counter_wide_time(timestamp, id, card_view, phone_show)
values
('2018-05-01 08:15:00', 2, 5, 1),
('2018-05-01 12:00:00', 2, 0, 4),
('2018-05-02 00:00:00', 2, 1, 0),
('2018-05-02 00:00:00', 3, 8, 0);

CREATE TABLE offer_event_counter_wide_day(
  day Date,
  id UInt64,
  card_view UInt64,
  phone_show UInt64
) ENGINE = SummingMergeTree()
  PARTITION BY toYYYYMM(day)
  ORDER BY (id, day)
  SETTINGS index_granularity = 8192;

insert into offer_event_counter_wide_day(day, id, card_view, phone_show)
values
('2018-05-01', 2, 5, 1),
('2018-05-01', 2, 0, 4),
('2018-05-02', 2, 1, 0),
('2018-05-02', 3, 8, 0);

CREATE TABLE offer_event_counter_with_filter(
  day Date,
  id UInt64,
  component String,
  feed_id String,
  is_new UInt8,
  rgid UInt32,
  categories Array(UInt32)
) ENGINE = MergeTree()
  PARTITION BY toYYYYMM(day)
  ORDER BY (id, component, day)
  SETTINGS index_granularity = 8192;

insert into offer_event_counter_with_filter(day, id, component, feed_id, is_new, rgid, categories)
values
('2018-05-01', 2, 'card_view', '123', 1, 225, [1, 2]),
('2018-05-01', 2, 'phone_show', '123', 0, 1, [1, 2]),
('2018-05-02', 2, 'card_view', '456', 1, 1, [3, 4]),
('2018-05-02', 2, 'card_view', '456', 1, 22, [1, 2]),
('2018-05-02', 3, 'card_view', '789', 0, 225, [3, 4]);
