 CREATE TABLE task_views (
    key         Utf8,
    queue       Utf8,
    lock_user   Utf8,
    updated_at  Timestamp,
    expired_at   Timestamp,
    view        Utf8,
    PRIMARY KEY (key, queue)
  ) WITH (
      TTL = Interval("PT0S") ON expired_at);