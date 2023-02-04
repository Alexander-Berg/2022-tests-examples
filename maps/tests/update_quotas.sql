DO $$
BEGIN
    -- increment quotas_version
    UPDATE quotateka.resources SET default_limit = 10, anonym_limit = 2;
    -- dont increment quotas_version as we dont change ratelimiter settings
    UPDATE quotateka.client_quotas set quota = 10;
    -- zero-diff, should not increment quotas_version
    UPDATE quotateka.account_quotas SET quota = 10, allocated = 5;
END $$;
