ALTER TABLE service.releases_notification_task ADD COLUMN test_emails text ARRAY;
ALTER TABLE service.releases_notification_task ADD CONSTRAINT test_emails_in_test_mode
    CHECK (mode = 'test' OR array_length(test_emails, 1) = 0);

COMMENT ON COLUMN service.releases_notification_task.test_emails IS
    'user specified emails in test mode';