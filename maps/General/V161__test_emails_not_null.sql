UPDATE service.releases_notification_task SET test_emails = '{}'::text ARRAY WHERE test_emails IS NULL;

ALTER TABLE service.releases_notification_task ALTER COLUMN test_emails SET NOT NULL;
ALTER TABLE service.releases_notification_task ALTER COLUMN test_emails SET DEFAULT '{}'::text ARRAY;

